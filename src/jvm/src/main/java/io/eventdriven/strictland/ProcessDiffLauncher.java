package io.eventdriven.strictland;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The default {@link DiffLauncher}: runs the tool's command with a real {@link ProcessBuilder}. It
 * starts the process without waiting, so the test doesn't block on the GUI, and swallows a failure to
 * start, so a missing or broken tool leaves the drift's {@code AssertionError} as the outcome rather
 * than a new error.
 */
final class ProcessDiffLauncher implements DiffLauncher {

    @Override
    public void launch(DiffTool tool, Path received, Path approved) {
        var argv = tool.command(received.toString(), approved.toString());
        try {
            new ProcessBuilder(argv)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException | RuntimeException e) {
            // Best effort: a diff tool that won't launch must not replace the drift failure.
            System.getLogger(ProcessDiffLauncher.class.getName())
                    .log(System.Logger.Level.DEBUG, "Could not launch diff tool " + tool.name(), e);
        }
    }
}
