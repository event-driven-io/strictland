package io.eventdriven.strictland;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

final class DiffTools {

    private DiffTools() {}

    /**
     * The command to launch for a drift, or empty when nothing usable is installed: a specific tool
     * when one is chosen, a custom command line when one is configured, otherwise the automatic
     * default - the developer's git-configured diff tool when git has one, or, failing that, the first
     * installed GUI tool from {@link DiffTool}'s built-in order.
     */
    static Optional<List<String>> command(
            @Nullable ToolPreference preference,
            Path received,
            Path approved,
            Predicate<String> installed,
            BooleanSupplier gitDiffToolConfigured) {
        return switch (preference) {
            case null -> automatic(received, approved, installed, gitDiffToolConfigured);
            case ToolPreference.Named named -> named.tool().command(received, approved, installed);
            case ToolPreference.Custom custom -> customCommand(custom.command(), received, approved, installed);
        };
    }

    private static Optional<List<String>> automatic(
            Path received, Path approved, Predicate<String> installed, BooleanSupplier gitDiffToolConfigured) {
        if (gitDiffToolConfigured.getAsBoolean()) {
            var git = gitDifftool(received, approved, installed);
            if (git.isPresent()) {
                return git;
            }
        }
        return firstInstalledGui(received, approved, installed);
    }

    /**
     * Delegates to {@code git difftool --no-index}, which opens whatever the developer set in {@code git
     * config diff.tool}. Empty only on the vanishingly rare machine without git on the path.
     */
    private static Optional<List<String>> gitDifftool(Path received, Path approved, Predicate<String> installed) {
        if (!installed.test("git")) {
            return Optional.empty();
        }
        return Optional.of(List.of("git", "difftool", "--no-index", approved.toString(), received.toString()));
    }

    /**
     * Reports whether {@code git config diff.tool} resolves to a value, meaning the developer has told
     * git which diff tool to open. Any launch or read failure is treated as "not configured".
     */
    static boolean gitDiffToolConfigured() {
        return gitDiffToolConfigured(new ProcessBuilder("git", "config", "diff.tool"));
    }

    static boolean gitDiffToolConfigured(ProcessBuilder probe) {
        probe.redirectError(ProcessBuilder.Redirect.DISCARD);
        try {
            var process = probe.start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return !output.isEmpty();
        } catch (IOException failedToProbe) {
            return false;
        }
    }

    static boolean onPath(String executable) {
        return onPath(executable, System.getenv("PATH"));
    }

    static boolean onPath(String executable, @Nullable String path) {
        if (executable.contains(File.separator)) {
            return new File(executable).canExecute();
        }
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

    static ToolPreference fromToolSetting(String value) {
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Diff tool setting must not be blank.");
        }
        if (trimmed.contains(" ")) {
            return new ToolPreference.Custom(trimmed);
        }
        return new ToolPreference.Named(requireName(trimmed));
    }

    static DiffTool requireName(String name) {
        return DiffTool.byName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown diff tool: " + name + ". Known tools: " + DiffTool.knownNames()));
    }

    private static Optional<List<String>> firstInstalledGui(Path received, Path approved, Predicate<String> installed) {
        for (var tool : DiffTool.values()) {
            var command = tool.command(received, approved, installed);
            if (command.isPresent()) {
                return command;
            }
        }
        return Optional.empty();
    }

    private static Optional<List<String>> customCommand(
            String template, Path received, Path approved, Predicate<String> installed) {
        var tokens = List.of(template.trim().split("\\s+", -1));
        var executable = tokens.get(0);
        if (!installed.test(executable)) {
            return Optional.empty();
        }
        var argv = new ArrayList<String>(tokens.size());
        argv.add(executable);
        for (var token : tokens.subList(1, tokens.size())) {
            argv.add(token.replace("{received}", received.toString()).replace("{approved}", approved.toString()));
        }
        return Optional.of(List.copyOf(argv));
    }
}
