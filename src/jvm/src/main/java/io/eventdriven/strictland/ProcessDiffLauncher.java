package io.eventdriven.strictland;

import java.io.IOException;
import java.util.List;

final class ProcessDiffLauncher implements DiffLauncher {

    @Override
    public void launch(List<String> command) {
        try {
            new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException | RuntimeException e) {
            System.getLogger(ProcessDiffLauncher.class.getName())
                    .log(System.Logger.Level.DEBUG, "Could not launch diff tool " + command, e);
        }
    }
}
