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

    private static final class RecordingLauncher implements DiffLauncher {
        private int launches;

        @Nullable private DiffTool tool;

        @Nullable private Path received;

        @Nullable private Path approved;

        @Override
        public void launch(DiffTool tool, Path received, Path approved) {
            this.launches++;
            this.tool = tool;
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

    private static FileSnapshotStorage storage(
            Path root, SnapshotReview review, DiffLauncher launcher, boolean nonInteractive) {
        return new FileSnapshotStorage(
                SnapshotLayout.registry().rootPath(root.toString()), review, launcher, () -> nonInteractive);
    }

    private static void seedApproved(FileSnapshotStorage storage) {
        storage.store(LOCATION, new SnapshotData(APPROVED));
    }

    @Test
    void approveMode_promotesTheReceivedPayloadWithoutThrowingOrLaunching(@TempDir Path root) throws Exception {
        var launcher = new RecordingLauncher();
        var storage = storage(root, SnapshotReview.approve(), launcher, false);
        seedApproved(storage);

        storage.store(LOCATION, new SnapshotData(DRIFTED));

        assertArrayEquals(DRIFTED, Files.readAllBytes(approvedFile(root)));
        assertFalse(Files.exists(receivedFile(root)), "received should be cleaned up after promotion");
        assertEquals(0, launcher.launches);
    }

    @Test
    void autoMode_local_launchesTheExplicitToolOnceWithReceivedThenApproved(@TempDir Path root) {
        var acme =
                new DiffTool("acme", java.util.List.of("acme"), java.util.List.of("acme", "{received}", "{approved}"));
        var launcher = new RecordingLauncher();
        var storage = storage(root, SnapshotReview.tool(acme), launcher, false);
        seedApproved(storage);

        assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        assertEquals(1, launcher.launches);
        assertEquals("acme", requireNonNull(launcher.tool).name());
        assertEquals(receivedFile(root), launcher.received);
        assertEquals(approvedFile(root), launcher.approved);
    }

    @Test
    void autoMode_local_detectsAToolWhenNoneIsExplicit(@TempDir Path root) {
        var launcher = new RecordingLauncher();
        var storage = storage(root, SnapshotReview.auto(), launcher, false);
        seedApproved(storage);

        assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        // git is installed in the dev container, so detection finds a tool to launch.
        assertEquals(1, launcher.launches);
    }

    @Test
    void autoMode_nonInteractive_writesReceivedAndThrowsWithoutLaunching(@TempDir Path root) throws Exception {
        var launcher = new RecordingLauncher();
        var storage = storage(root, SnapshotReview.auto(), launcher, true);
        seedApproved(storage);

        assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        assertEquals(0, launcher.launches);
        assertArrayEquals(DRIFTED, Files.readAllBytes(receivedFile(root)));
    }

    @Test
    void offMode_neverLaunchesButStillFails(@TempDir Path root) {
        var launcher = new RecordingLauncher();
        var storage = storage(root, SnapshotReview.off(), launcher, false);
        seedApproved(storage);

        assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        assertEquals(0, launcher.launches);
    }

    @Test
    void driftMessage_carriesTheInlineDiffBothPathsAndTheApproveHint(@TempDir Path root) {
        var storage = storage(root, SnapshotReview.off(), new RecordingLauncher(), false);
        seedApproved(storage);

        var error = assertThrows(AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(DRIFTED)));

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("- {\"id\":1}"), message);
        assertTrue(message.contains("+ {\"id\":2}"), message);
        assertTrue(message.contains("received: " + receivedFile(root)), message);
        assertTrue(message.contains("approved: " + approvedFile(root)), message);
        assertTrue(message.contains("-Dstrictland.review.mode=approve"), message);
    }

    @Test
    void driftMessage_fallsBackToAHexSummaryForBinaryPayloads(@TempDir Path root) {
        var storage = storage(root, SnapshotReview.off(), new RecordingLauncher(), false);
        storage.store(LOCATION, new SnapshotData(new byte[] {0, 1, 2}));

        var error = assertThrows(
                AssertionError.class, () -> storage.store(LOCATION, new SnapshotData(new byte[] {0, 1, 3})));

        assertTrue(requireNonNull(error.getMessage()).contains("Binary content differs"), error.getMessage());
    }
}
