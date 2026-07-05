package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Opens the developer's diff tool on a drifted snapshot's received and approved files, so a failing
 * check can be eyeballed and, if the change is wanted, accepted from the tool. Opening is a no-op when
 * the review is off or the machine is non-interactive (CI or headless), or when no tool is installed.
 */
interface DiffReview {

    void open(Path received, Path approved);

    static DiffReview forReview(SnapshotReview review) {
        return new Launching(review, DiffTools::onPath, CiDetector.isNonInteractive(), new ProcessDiffLauncher());
    }

    /** Resolves the tool for the review against the environment and launches it, unless there is nothing to open. */
    record Launching(SnapshotReview review, Predicate<String> installed, boolean nonInteractive, DiffLauncher launcher)
            implements DiffReview {

        @Override
        public void open(Path received, Path approved) {
            if (review.mode() == ReviewMode.OFF || nonInteractive) {
                return;
            }
            DiffTools.command(review.toolPreference(), received, approved, installed)
                    .ifPresent(launcher::launch);
        }
    }
}
