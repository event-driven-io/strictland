package io.eventdriven.strictland;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private CiDetector() {}

    static boolean isNonInteractive() {
        return isNonInteractive(
                System.getenv(),
                GraphicsEnvironment.isHeadless(),
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT));
    }

    static boolean isNonInteractive(Map<String, String> env, boolean headless, String osName) {
        if (headless) {
            return true;
        }
        for (var name : CI_VARS) {
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
