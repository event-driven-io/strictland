package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Opens a diff tool for a drifted snapshot pair on behalf of {@link FileSnapshotStorage}. Whether a
 * tool opens at all is decided while selecting one: {@link #selectTool} yields no tool when the review
 * is off or the machine is non-interactive, so a run with nothing to open simply launches nothing.
 */
interface DiffReview {

    void open(Path received, Path approved);

    /**
     * The production review: pick a tool for the current environment and launch it as a real process.
     */
    static DiffReview forReview(SnapshotReview review) {
        return new Launching(() -> selectTool(review, CiDetector.isNonInteractive()), new ProcessDiffLauncher());
    }

    /**
     * The tool to open for a drift, or empty when the review is off or the machine is non-interactive
     * (CI or headless). Folding the interactivity check into selection keeps launching a plain "open
     * whatever was selected", with nothing to open on a non-interactive run.
     */
    static Optional<ResolvedDiffTool> selectTool(SnapshotReview review, boolean nonInteractive) {
        if (review.mode() == ReviewMode.OFF || nonInteractive) {
            return Optional.empty();
        }
        return DiffTools.resolve(review.toolPreference());
    }

    /** A review that launches the selected tool, if any, through the given launcher. */
    record Launching(Supplier<Optional<ResolvedDiffTool>> tool, DiffLauncher launcher) implements DiffReview {

        @Override
        public void open(Path received, Path approved) {
            tool.get().ifPresent(resolved -> launcher.launch(resolved, received, approved));
        }
    }
}
