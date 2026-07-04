package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * Opens a {@link DiffTool} on a drifted snapshot. {@link FileSnapshotStorage} calls it on drift so the
 * two files show up side by side; the seam lets a test assert what would launch without spawning a
 * process. The default implementation is {@link ProcessDiffLauncher}.
 */
interface DiffLauncher {

    /**
     * Launches {@code tool} on the two files, fire-and-forget: it must not block the test on a GUI, and
     * a tool that fails to start must not change the test's outcome.
     */
    void launch(DiffTool tool, Path received, Path approved);
}
