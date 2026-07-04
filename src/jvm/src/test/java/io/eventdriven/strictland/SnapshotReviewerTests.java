package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotReviewerTests {

    private static final MessageTypeName MESSAGE_TYPE = MessageTypeName.of("io.eventdriven.strictland.OrderPlaced");
    private static final SnapshotLocation LOCATION =
            SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");
    private static final Path RECEIVED_FILE = Path.of("OrderPlaced.1.default.snap.received.json");
    private static final Path APPROVED_FILE = Path.of("OrderPlaced.1.default.snap.approved.json");

    private static SnapshotResult.Drifted drift(byte[] approved, byte[] received) {
        return new SnapshotResult.Drifted(
                LOCATION, new SnapshotData(approved), new SnapshotData(received), APPROVED_FILE, RECEIVED_FILE);
    }

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

    /** A storage that only records {@code approve} calls; the reviewer never reaches its other methods. */
    private static final class RecordingStorage implements SnapshotStorage {
        private int approvals;

        @Nullable private SnapshotLocation location;

        @Nullable private SnapshotData data;

        @Override
        public SnapshotResult store(SnapshotLocation location, SnapshotData data) {
            throw new AssertionError("store is not used by the reviewer");
        }

        @Override
        public void approve(SnapshotLocation location, SnapshotData data) {
            this.approvals++;
            this.location = location;
            this.data = data;
        }

        @Override
        public SnapshotData read(SnapshotLocation location) {
            throw new AssertionError("read is not used by the reviewer");
        }

        @Override
        public List<SnapshotData> readAll(SnapshotFilter filter) {
            throw new AssertionError("readAll is not used by the reviewer");
        }
    }

    @Test
    void aFirstRunApprovalPassesWithoutOpeningOrThrowing() {
        var diffReview = new RecordingDiffReview();
        var reviewer = new SnapshotReviewer(SnapshotReview.auto(), diffReview);

        reviewer.review(new RecordingStorage(), new SnapshotResult.Approved(LOCATION));

        assertEquals(0, diffReview.opens);
    }

    @Test
    void anUnchangedMatchPassesWithoutOpeningOrThrowing() {
        var diffReview = new RecordingDiffReview();
        var reviewer = new SnapshotReviewer(SnapshotReview.auto(), diffReview);

        reviewer.review(new RecordingStorage(), new SnapshotResult.Unchanged(LOCATION));

        assertEquals(0, diffReview.opens);
    }

    @Test
    void aDriftOpensTheReviewOnceWithReceivedThenApprovedAndFails() {
        var diffReview = new RecordingDiffReview();
        var reviewer = new SnapshotReviewer(SnapshotReview.auto(), diffReview);

        assertThrows(
                AssertionError.class,
                () -> reviewer.review(
                        new RecordingStorage(), drift("{\"id\":1}".getBytes(UTF_8), "{\"id\":2}".getBytes(UTF_8))));

        assertEquals(1, diffReview.opens);
        assertEquals(RECEIVED_FILE, diffReview.received);
        assertEquals(APPROVED_FILE, diffReview.approved);
    }

    @Test
    void aDriftFailureCarriesTheInlineDiffBothPathsAndTheApproveHint() {
        var reviewer = new SnapshotReviewer(SnapshotReview.off(), (received, approved) -> {});

        var error = assertThrows(
                AssertionError.class,
                () -> reviewer.review(
                        new RecordingStorage(), drift("{\"id\":1}".getBytes(UTF_8), "{\"id\":2}".getBytes(UTF_8))));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Text content differs (- approved, + received):"), message);
        assertTrue(message.contains("- 1 | {\"id\":1}"), message);
        assertTrue(message.contains("+ 1 | {\"id\":2}"), message);
        assertTrue(message.contains("received: " + RECEIVED_FILE), message);
        assertTrue(message.contains("approved: " + APPROVED_FILE), message);
        assertTrue(message.contains("-Dstrictland.review.mode=approve"), message);
    }

    @Test
    void aDriftFailureFallsBackToAHexSummaryForBinaryPayloads() {
        var reviewer = new SnapshotReviewer(SnapshotReview.off(), (received, approved) -> {});

        var error = assertThrows(
                AssertionError.class,
                () -> reviewer.review(new RecordingStorage(), drift(new byte[] {0, 1, 2}, new byte[] {0, 1, 3})));

        assertTrue(requireNonNull(error.getMessage()).contains("Binary content differs"), error.getMessage());
    }

    @Test
    void approveModeReBaselinesViaStorageWithoutOpeningOrThrowing() {
        var diffReview = new RecordingDiffReview();
        var storage = new RecordingStorage();
        var reviewer = new SnapshotReviewer(SnapshotReview.approve(), diffReview);
        var received = "{\"id\":2}".getBytes(UTF_8);

        reviewer.review(storage, drift("{\"id\":1}".getBytes(UTF_8), received));

        assertEquals(0, diffReview.opens);
        assertEquals(1, storage.approvals);
        assertEquals(LOCATION, storage.location);
        assertArrayEquals(received, requireNonNull(storage.data).bytes());
    }
}
