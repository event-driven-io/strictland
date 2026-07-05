package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
final class DiffReviewTests {

    private static final Path RECEIVED = Path.of("OrderPlaced.1.default.snap.received.json");
    private static final Path APPROVED = Path.of("OrderPlaced.1.default.snap.approved.json");

    private static final class RecordingLauncher implements DiffLauncher {
        private int launches;

        @Nullable private List<String> command;

        @Override
        public void launch(List<String> command) {
            this.launches++;
            this.command = command;
        }
    }

    private static DiffReview.Launching launching(
            SnapshotReview review, boolean nonInteractive, DiffLauncher launcher) {
        // git is a registered tool always present in the build environment, so this drives the real
        // command resolution rather than an injected tool.
        return new DiffReview.Launching(review, exe -> exe.equals("git"), nonInteractive, launcher);
    }

    @Test
    void open_launchesTheResolvedToolWithApprovedThenReceived() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.tool(DiffTool.GIT), false, launcher).open(RECEIVED, APPROVED);

        assertEquals(1, launcher.launches);
        assertEquals(
                List.of("git", "difftool", "--no-index", APPROVED.toString(), RECEIVED.toString()), launcher.command);
    }

    @Test
    void open_launchesNothingWhenTheReviewIsOff() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.off(), false, launcher).open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
        assertNull(launcher.command);
    }

    @Test
    void open_launchesNothingWhenTheMachineIsNonInteractive() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.tool(DiffTool.GIT), true, launcher).open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
    }

    @Test
    void open_launchesNothingWhenNoToolIsInstalled() {
        var launcher = new RecordingLauncher();
        var review = new DiffReview.Launching(SnapshotReview.tool(DiffTool.MELD), exe -> false, false, launcher);

        review.open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
    }

    @Test
    void forReview_wiresTheEnvironmentWithoutThrowing() {
        // On the headless build the review is a safe no-op; this still drives the production wiring
        // (real interactivity check and process launcher).
        var review = DiffReview.forReview(SnapshotReview.tool(DiffTool.GIT));

        assertDoesNotThrow(() -> review.open(RECEIVED, APPROVED));
    }
}
