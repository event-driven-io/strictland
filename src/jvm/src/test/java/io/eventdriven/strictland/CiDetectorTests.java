package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class CiDetectorTests {

    @Test
    void headless_isAlwaysNonInteractive() {
        assertTrue(CiDetector.isNonInteractive(Map.of("DISPLAY", ":0"), true, "windows"));
    }

    @Test
    void aCiMarker_isNonInteractiveEvenWithADisplay() {
        var env = Map.of("GITHUB_ACTIONS", "true", "DISPLAY", ":0");

        assertTrue(CiDetector.isNonInteractive(env, false, "linux"));
    }

    @Test
    void linuxWithoutADisplay_isNonInteractive() {
        assertTrue(CiDetector.isNonInteractive(Map.of(), false, "linux"));
    }

    @Test
    void linuxWithAnX11Display_isInteractive() {
        assertFalse(CiDetector.isNonInteractive(Map.of("DISPLAY", ":0"), false, "linux"));
    }

    @Test
    void linuxWithAWaylandDisplay_isInteractive() {
        assertFalse(CiDetector.isNonInteractive(Map.of("WAYLAND_DISPLAY", "wayland-0"), false, "linux"));
    }

    @Test
    void desktopWithADisplay_isInteractive() {
        assertFalse(CiDetector.isNonInteractive(Map.of("DISPLAY", ":0"), false, "windows"));
    }

    @Test
    void aBlankMarker_doesNotCount() {
        assertFalse(CiDetector.isNonInteractive(Map.of("CI", "  ", "DISPLAY", ":0"), false, "windows"));
    }

    @Test
    void theRealEnvironmentResolvesWithoutAssertingLocalMachineState() {
        assertDoesNotThrow(() -> CiDetector.isNonInteractive());
    }
}
