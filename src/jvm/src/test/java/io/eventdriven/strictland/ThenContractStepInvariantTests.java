package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class ThenContractStepInvariantTests {

    private record OrderPlaced(String orderId) {}

    /** Records what the check asked the diff review to open, without launching anything. */
    private static final class RecordingDiffReview implements DiffReview {
        private int opens;

        @Nullable private Path received;

        @Nullable private Path approved;

        @Override
        public void open(Path received, Path approved) {
            this.opens++;
            this.received = received;
            this.approved = approved;
        }
    }

    private static ContractContext context() {
        return context(DiffReview.forReview(SnapshotReview.off()), SnapshotLayout.registry());
    }

    private static ContractContext context(DiffReview diffReview, SnapshotLayout layout) {
        return new ContractContext(
                new JacksonMessageSerializer(new JsonMapper()),
                new FileSnapshotStorage(layout),
                MessageTypeMapper.fullyQualifiedName(),
                new SnapshotReviewer(SnapshotReview.auto()),
                diffReview);
    }

    @Test
    void whenTheDestinationIsAMessageInHand_thenContractIsUnchanged_rejectsIt() {
        var step = new ThenContractStep<>(
                new OrderPlaced("1"),
                MessageSnapshot.of(new OrderPlaced("1")),
                MessageSnapshot.DEFAULT_VERSION,
                context());

        assertThrows(IllegalArgumentException.class, step::thenContractIsUnchanged);
    }

    @Test
    void whenTheContractDrifts_thenContractIsUnchanged_opensTheDrivenPairInTheDiffReviewThenFails(@TempDir Path root) {
        var diffReview = new RecordingDiffReview();
        var context = context(diffReview, SnapshotLayout.registry().rootPath(root.toString()));

        // First run baselines the approved snapshot, so nothing is opened yet.
        new ThenContractStep<>(new OrderPlaced("1"), null, MessageSnapshot.DEFAULT_VERSION, context)
                .thenContractIsUnchanged();
        assertEquals(0, diffReview.opens);

        // Second run drifts, so the check must open the drifted pair before it fails.
        var drifted = new ThenContractStep<>(new OrderPlaced("2"), null, MessageSnapshot.DEFAULT_VERSION, context);

        assertThrows(AssertionError.class, drifted::thenContractIsUnchanged);

        assertEquals(1, diffReview.opens);
        assertNotNull(diffReview.received);
        assertNotNull(diffReview.approved);
        assertTrue(requireNonNull(diffReview.received).toString().endsWith(".received.json"), "received file");
        assertTrue(requireNonNull(diffReview.approved).toString().endsWith(".approved.json"), "approved file");
    }
}
