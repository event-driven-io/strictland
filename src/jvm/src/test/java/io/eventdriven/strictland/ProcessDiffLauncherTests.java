package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ProcessDiffLauncherTests {

    private final ProcessDiffLauncher launcher = new ProcessDiffLauncher();

    @Test
    void launch_startsARealProcessWithoutBlocking() {
        assertDoesNotThrow(() -> launcher.launch(List.of("true", "x.received.json", "x.approved.json")));
    }

    @Test
    void launch_swallowsAFailureToStartSoTheDriftFailureStands() {
        assertDoesNotThrow(() -> launcher.launch(List.of("strictland-no-such-binary", "x.received.json")));
    }
}
