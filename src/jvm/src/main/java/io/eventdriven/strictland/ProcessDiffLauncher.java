package io.eventdriven.strictland;

import java.io.IOException;
import java.nio.file.Path;

final class ProcessDiffLauncher implements DiffLauncher {

    @Override
    public void launch(ResolvedDiffTool tool, Path received, Path approved) {
        var argv = tool.command(received.toString(), approved.toString());
        try {
            new ProcessBuilder(argv)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException | RuntimeException e) {
            System.getLogger(ProcessDiffLauncher.class.getName())
                    .log(System.Logger.Level.DEBUG, "Could not launch diff tool " + tool.name(), e);
        }
    }
}
