package io.eventdriven.strictland;

import java.nio.file.Path;

interface DiffLauncher {

    void launch(ResolvedDiffTool tool, Path received, Path approved);
}
