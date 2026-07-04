package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class FileSnapshotStorageReviewTests {

    private static final MessageTypeName MESSAGE_TYPE = MessageTypeName.of("io.eventdriven.strictland.OrderPlaced");
    private static final SnapshotLocation LOCATION =
            SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");
    private static final byte[] APPROVED = "{\"id\":1}".getBytes(UTF_8);
    private static final byte[] DRIFTED = "{\"id\":2}".getBytes(UTF_8);

    private static final class RecordingReview implements DiffReview {
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

    private static Path approvedFile(Path root) {
        return root.resolve("contract-registry")
                .resolve("io/eventdriven/strictland/OrderPlaced")
                .resolve("OrderPlaced.1.default.snap.approved.json");
    }

    private static Path receivedFile(Path root) {
        return root.resolve("contract-registry")
                .resolve("io/eventdriven/strictland/OrderPlaced")
                .resolve("OrderPlaced.1.default.snap.received.json");
    }

    private static FileSnapshotStorage storage(Path root, SnapshotReview review, DiffReview diffReview) {
        return new FileSnapshotStorage(SnapshotLayout.registry().rootPath(root.toString()), review, diffReview);
    }

    private static void seedApproved(FileSnapshotStorage storage) {
        storage.store(LOCATION, new SnapshotData(APPROVED));
    }

    @Test
    void approveMode_promotesTheReceivedPayloadWithoutThrowingOrOpeningAReview(@TempDir Path root) throws Exception {
        var review = new RecordingReview();
        var storage = storage(root, SnapshotReview.approve(), review);
        seedApproved(storage);

        storage.store(LOCATION, new SnapshotData(DRIFTED));

        assertArrayEquals(DRIFTED, Files.readAllBytes(approvedFile(root)));
        assertFalse(Files.exists(receivedFile(root)), "received should be cleaned up after promotion");
        assertEquals(0, review.opens);
    }

    @Test
    void drift_writesReceivedThenOpensTheReviewOnceWithReceivedAndApprovedAndFails(@TempDir Path root)
            throws Exception {
        var review = new RecordingReview();
        var storage = storage(root, SnapshotReview.auto(), review);
        seedApproved(storage);

        assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        assertArrayEquals(DRIFTED, Files.readAllBytes(receivedFile(root)));
        assertEquals(1, review.opens);
        assertEquals(receivedFile(root), review.received);
        assertEquals(approvedFile(root), review.approved);
    }

    @Test
    void driftMessage_carriesTheInlineDiffBothPathsAndTheApproveHint(@TempDir Path root) {
        var storage = storage(root, SnapshotReview.off(), (received, approved) -> {});
        seedApproved(storage);

        var error = assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Text content differs (- approved, + received):"), message);
        assertTrue(message.contains("- 1 | {\"id\":1}"), message);
        assertTrue(message.contains("+ 1 | {\"id\":2}"), message);
        assertTrue(message.contains("received: " + receivedFile(root)), message);
        assertTrue(message.contains("approved: " + approvedFile(root)), message);
        assertTrue(message.contains("-Dstrictland.review.mode=approve"), message);
    }

    @Test
    void driftMessage_fallsBackToAHexSummaryForBinaryPayloads(@TempDir Path root) {
        var storage = storage(root, SnapshotReview.off(), (received, approved) -> {});
        storage.store(LOCATION, new SnapshotData(new byte[] {0, 1, 2}));

        var error = assertThrows(
                AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(new byte[] {0, 1, 3})));

        assertTrue(requireNonNull(error.getMessage()).contains("Binary content differs"), error.getMessage());
    }
}
