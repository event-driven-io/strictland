package io.eventdriven.strictland;

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
    void anAgentCliMarker_isNonInteractive() {
        assertTrue(CiDetector.isNonInteractive(Map.of("CLAUDECODE", "1"), false, "mac os x"));
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
    void theRealEnvironmentResolvesWithoutError() {
        // In the headless dev container this is true; the point is it runs the real branch.
        assertTrue(CiDetector.isNonInteractive());
    }
}
