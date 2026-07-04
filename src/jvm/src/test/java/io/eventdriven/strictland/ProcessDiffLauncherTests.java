package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ProcessDiffLauncherTests {

    private final ProcessDiffLauncher launcher = new ProcessDiffLauncher();
    private final Path received = Path.of("x.received.json");
    private final Path approved = Path.of("x.approved.json");

    @Test
    void launch_startsARealProcessWithoutBlocking() {
        // "true" exists in the dev container, ignores its args, and exits immediately.
        var tool = DiffTools.custom("true {received} {approved}");

        assertDoesNotThrow(() -> launcher.launch(tool, received, approved));
    }

    @Test
    void launch_swallowsAFailureToStartSoTheDriftFailureStands() {
        var missing = DiffTools.custom("strictland-no-such-binary {received} {approved}");

        assertDoesNotThrow(() -> launcher.launch(missing, received, approved));
    }
}
