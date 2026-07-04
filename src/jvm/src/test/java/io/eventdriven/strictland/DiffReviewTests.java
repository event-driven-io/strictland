package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
final class DiffReviewTests {

    private static final Path RECEIVED = Path.of("OrderPlaced.1.default.snap.received.json");
    private static final Path APPROVED = Path.of("OrderPlaced.1.default.snap.approved.json");
    private static final ResolvedDiffTool TOOL =
            new ResolvedDiffTool("acme", "acme", List.of("{received}", "{approved}"));

    private static final class RecordingLauncher implements DiffLauncher {
        private int launches;

        @Nullable private ResolvedDiffTool tool;

        @Nullable private Path received;

        @Nullable private Path approved;

        @Override
        public void launch(ResolvedDiffTool tool, Path received, Path approved) {
            this.launches++;
            this.tool = tool;
            this.received = received;
            this.approved = approved;
        }
    }

    @Test
    void selectTool_isEmptyWhenTheReviewIsOff() {
        assertTrue(DiffReview.selectTool(SnapshotReview.off(), false).isEmpty());
    }

    @Test
    void selectTool_isEmptyWhenTheMachineIsNonInteractive() {
        assertTrue(DiffReview.selectTool(SnapshotReview.tool("git"), true).isEmpty());
    }

    @Test
    void selectTool_resolvesTheConfiguredToolFromTheEnvironmentWhenInteractive() {
        // git is a registered tool and is always present in the build environment, so this drives the
        // real DiffTools.resolve/onPath lookup rather than an injected tool.
        var resolved = DiffReview.selectTool(SnapshotReview.tool("git"), false);

        assertEquals("git", resolved.orElseThrow().name());
    }

    @Test
    void open_launchesTheSelectedToolWithReceivedThenApproved() {
        var launcher = new RecordingLauncher();
        var review = new DiffReview.Launching(() -> Optional.of(TOOL), launcher);

        review.open(RECEIVED, APPROVED);

        assertEquals(1, launcher.launches);
        assertEquals(TOOL, launcher.tool);
        assertEquals(RECEIVED, launcher.received);
        assertEquals(APPROVED, launcher.approved);
    }

    @Test
    void open_launchesNothingWhenNoToolIsSelected() {
        var launcher = new RecordingLauncher();
        var review = new DiffReview.Launching(Optional::empty, launcher);

        review.open(RECEIVED, APPROVED);

        assertEquals(0, launcher.launches);
    }

    @Test
    void forReview_wiresTheEnvironmentWithoutThrowing() {
        // On the headless build the selection resolves to nothing, so opening is a safe no-op; this
        // still drives the production wiring (real interactivity check and process launcher).
        var review = DiffReview.forReview(SnapshotReview.tool("git"));

        assertDoesNotThrow(() -> review.open(RECEIVED, APPROVED));
    }
}
