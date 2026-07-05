package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
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
            SnapshotReview review,
            Predicate<String> installed,
            boolean gitConfigured,
            boolean nonInteractive,
            DiffLauncher launcher) {
        return new DiffReview.Launching(review, installed, () -> gitConfigured, nonInteractive, launcher);
    }

    @Test
    void open_launchesTheGitConfiguredDifftoolWithApprovedThenReceived() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.auto(), exe -> exe.equals("git"), true, false, launcher)
                .open(RECEIVED, APPROVED);

        assertEquals(1, launcher.launches);
        assertEquals(
                List.of("git", "difftool", "--no-index", APPROVED.toString(), RECEIVED.toString()), launcher.command);
    }

    @Test
    void open_launchesTheFirstInstalledGuiWhenGitHasNoConfiguredTool() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.auto(), exe -> exe.equals("meld"), false, false, launcher)
                .open(RECEIVED, APPROVED);

        assertEquals(1, launcher.launches);
        assertEquals(List.of("meld", RECEIVED.toString(), APPROVED.toString()), launcher.command);
    }

    @Test
    void open_launchesNothingWhenTheReviewIsOff() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.off(), exe -> true, true, false, launcher).open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
        assertNull(launcher.command);
    }

    @Test
    void open_launchesNothingWhenTheMachineIsNonInteractive() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.auto(), exe -> true, true, true, launcher).open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
    }

    @Test
    void open_launchesNothingWhenNoToolIsInstalled() {
        var launcher = new RecordingLauncher();

        launching(SnapshotReview.tool(DiffTool.MELD), exe -> false, false, false, launcher)
                .open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
    }

    @Test
    void forReview_wiresTheReviewIntoTheProductionLauncher() {
        // Only construct the production wiring - deliberately do NOT call open() here. open() resolves
        // and launches the real tool, which on an interactive machine spawns a diff GUI. That launch is
        // covered above with a recording launcher; the resolution seams are covered in DiffToolTests.
        var review = SnapshotReview.auto();

        var wired = DiffReview.forReview(review);

        assertEquals(review, assertInstanceOf(DiffReview.Launching.class, wired).review());
    }
}
