package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Opens the developer's diff tool on a drifted snapshot's received and approved files, so a failing
 * check can be eyeballed and, if the change is wanted, accepted from the tool. Whether anything opens
 * is settled up front when the tool is selected: {@link #selectTool} yields no tool when review is off
 * or the machine is non-interactive (CI or headless), so on those runs opening is simply a no-op.
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
