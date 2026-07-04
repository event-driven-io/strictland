package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

final class DiffTools {

    private static final String RECV = "{received}";
    private static final String APPR = "{approved}";

    private static final Pattern ORDER_SEPARATOR = Pattern.compile("[,|\\s]+");

    private static final System.Logger LOGGER = System.getLogger(DiffTools.class.getName());

    private static final List<DiffTool> REGISTRY = List.of(
            new DiffTool("vscode", List.of("code", "code.cmd"), List.of("--diff", "--wait", RECV, APPR)),
            new DiffTool("cursor", List.of("cursor", "cursor.cmd"), List.of("--diff", "--wait", RECV, APPR)),
            new DiffTool("idea", List.of("idea", "idea.sh", "idea.cmd"), List.of("diff", RECV, APPR)),
            new DiffTool("meld", List.of("meld"), List.of(RECV, APPR)),
            new DiffTool("bcompare", List.of("bcompare", "BCompare.exe"), List.of(RECV, APPR)),
            new DiffTool("kdiff3", List.of("kdiff3"), List.of(RECV, APPR)),
            new DiffTool("p4merge", List.of("p4merge"), List.of(RECV, APPR)),
            new DiffTool("winmerge", List.of("WinMergeU.exe"), List.of("/u", "/wl", RECV, APPR)),
            new DiffTool("git", List.of("git"), List.of("difftool", "--no-index", APPR, RECV)));

    private DiffTools() {}

    static Optional<DiffTool> byName(String name) {
        for (var tool : REGISTRY) {
            if (tool.name().equals(name)) {
                return Optional.of(tool);
            }
        }
        return Optional.empty();
    }

    static String requireName(String name) {
        var trimmed = name.trim();
        if (trimmed.isEmpty() || byName(trimmed).isEmpty()) {
            throw new IllegalArgumentException("Unknown diff tool: " + name + ". Known tools: " + knownNames());
        }
        return trimmed;
    }

    static ToolPreference fromToolSetting(String value) {
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Diff tool setting must not be blank.");
        }
        if (trimmed.contains(" ")) {
            return ToolPreference.custom(trimmed);
        }
        return ToolPreference.single(requireName(trimmed));
    }

    static List<String> parseOrder(String value) {
        var names = new ArrayList<String>();
        ORDER_SEPARATOR
                .splitAsStream(value.trim())
                .filter(token -> !token.isBlank())
                .map(DiffTools::requireName)
                .forEach(names::add);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Tool order must name at least one registered diff tool.");
        }
        return List.copyOf(names);
    }

    static Optional<ResolvedDiffTool> resolve(@Nullable ToolPreference preference) {
        return resolve(preference, DiffTools::onPath);
    }

    static Optional<ResolvedDiffTool> resolve(@Nullable ToolPreference preference, Predicate<String> isInstalled) {
        if (preference == null) {
            return firstAvailable(REGISTRY, isInstalled);
        }
        return switch (preference.kind()) {
            case SINGLE -> resolveSingle(preference.names().get(0), isInstalled);
            case ORDER -> firstAvailable(ordered(preference.names()), isInstalled);
            case CUSTOM -> resolveCustom(requireNonNull(preference.template()), isInstalled);
        };
    }

    static boolean onPath(String executable) {
        return onPath(executable, System.getenv("PATH"));
    }

    static boolean onPath(String executable, @Nullable String path) {
        if (path == null) {
            return false;
        }
        for (var dir : path.split(File.pathSeparator, -1)) {
            if (!dir.isEmpty() && new File(dir, executable).canExecute()) {
                return true;
            }
        }
        return false;
    }

    private static Optional<ResolvedDiffTool> resolveSingle(String name, Predicate<String> isInstalled) {
        var resolved = byName(name).flatMap(tool -> tool.resolve(isInstalled));
        if (resolved.isEmpty()) {
            LOGGER.log(System.Logger.Level.DEBUG, "Configured diff tool is not available: " + name);
        }
        return resolved;
    }

    private static Optional<ResolvedDiffTool> firstAvailable(List<DiffTool> tools, Predicate<String> isInstalled) {
        for (var tool : tools) {
            var resolved = tool.resolve(isInstalled);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }

    private static Optional<ResolvedDiffTool> resolveCustom(String template, Predicate<String> isInstalled) {
        var tokens = List.of(template.trim().split("\\s+", -1));
        var executable = tokens.get(0);
        if (!isInstalled.test(executable) && !new File(executable).canExecute()) {
            LOGGER.log(System.Logger.Level.DEBUG, "Configured diff command is not available: " + executable);
            return Optional.empty();
        }
        return Optional.of(new ResolvedDiffTool("custom", executable, tokens.subList(1, tokens.size())));
    }

    private static List<DiffTool> ordered(List<String> preferredNames) {
        var tools = new ArrayList<DiffTool>();
        for (var name : preferredNames) {
            byName(name).ifPresent(tools::add);
        }
        for (var tool : REGISTRY) {
            if (!preferredNames.contains(tool.name())) {
                tools.add(tool);
            }
        }
        return List.copyOf(tools);
    }

    private static List<String> knownNames() {
        return REGISTRY.stream().map(DiffTool::name).toList();
    }
}
