package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ProcessDiffLauncherTests {

    private final ProcessDiffLauncher launcher = new ProcessDiffLauncher();
    private final Path received = Path.of("x.received.json");
    private final Path approved = Path.of("x.approved.json");

    @Test
    void launch_startsARealProcessWithoutBlocking() {
        var tool = new ResolvedDiffTool("custom", "true", List.of("{received}", "{approved}"));

        assertDoesNotThrow(() -> launcher.launch(tool, received, approved));
    }

    @Test
    void launch_swallowsAFailureToStartSoTheDriftFailureStands() {
        var missing = new ResolvedDiffTool("custom", "strictland-no-such-binary", List.of("{received}", "{approved}"));

        assertDoesNotThrow(() -> launcher.launch(missing, received, approved));
    }
}
