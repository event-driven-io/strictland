package io.eventdriven.strictland;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Decides whether the current run is one where opening a diff tool makes no sense - a CI build, a
 * headless machine, or an agent CLI with no one watching. {@link FileSnapshotStorage} consults it so a
 * drift on CI leaves only the inline diff, never a launched GUI that no one sees. Package-private: the
 * decision is an internal gate on tool launching.
 */
final class CiDetector {

    private static final List<String> CI_VARS = List.of(
            "CI",
            "CONTINUOUS_INTEGRATION",
            "GITHUB_ACTIONS",
            "JENKINS_URL",
            "TEAMCITY_VERSION",
            "TF_BUILD",
            "GITLAB_CI",
            "BUILDKITE",
            "CIRCLECI",
            "TRAVIS",
            "APPVEYOR",
            "DOTNET_RUNNING_IN_CONTAINER");

    // Best-effort markers for agent CLIs that run without a human at a display. Cheap to extend; on
    // Linux the no-display check below already catches most of these regardless.
    private static final List<String> AGENT_VARS =
            List.of("CLAUDECODE", "CURSOR_AGENT", "AIDER_CHAT", "GEMINI_CLI", "COPILOT_CLI", "GITHUB_COPILOT_CLI");

    private CiDetector() {}

    /** True when this run shouldn't open a diff tool, read from the real environment and display. */
    static boolean isNonInteractive() {
        return isNonInteractive(
                System.getenv(),
                GraphicsEnvironment.isHeadless(),
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT));
    }

    /**
     * The pure decision behind {@link #isNonInteractive()}: non-interactive when the JVM is headless,
     * a CI or agent-CLI marker is set, or a Linux run has no display server to open onto.
     */
    static boolean isNonInteractive(Map<String, String> env, boolean headless, String osName) {
        if (headless) {
            return true;
        }
        for (var name : CI_VARS) {
            if (isSet(env, name)) {
                return true;
            }
        }
        for (var name : AGENT_VARS) {
            if (isSet(env, name)) {
                return true;
            }
        }
        return osName.contains("linux") && !isSet(env, "DISPLAY") && !isSet(env, "WAYLAND_DISPLAY");
    }

    private static boolean isSet(Map<String, String> env, String name) {
        var value = env.get(name);
        return value != null && !value.isBlank();
    }
}
