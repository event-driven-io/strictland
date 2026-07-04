package io.eventdriven.strictland;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * The curated registry of diff tools Strictland knows how to launch, plus the detection that picks one
 * that is actually installed. Package-private: callers reach it through {@link SnapshotReview} and the
 * {@code strictland.review.tool} setting rather than directly.
 *
 * <p>The registry is a single ordered list, most preferred first. Each tool lists every executable
 * name it might install as on any platform, so detection just probes those names on the {@code PATH}
 * without branching on the operating system.</p>
 */
final class DiffTools {

    private DiffTools() {}

    static List<DiffTool> registry() {
        return List.of(
                new DiffTool("vscode", List.of("code", "code.cmd"), List.of("code", "--diff", "--wait", RECV, APPR)),
                new DiffTool(
                        "cursor", List.of("cursor", "cursor.cmd"), List.of("cursor", "--diff", "--wait", RECV, APPR)),
                new DiffTool("idea", List.of("idea", "idea.sh", "idea.cmd"), List.of("idea", "diff", RECV, APPR)),
                new DiffTool("meld", List.of("meld"), List.of("meld", RECV, APPR)),
                new DiffTool("bcompare", List.of("bcompare", "BCompare.exe"), List.of("bcompare", RECV, APPR)),
                new DiffTool("kdiff3", List.of("kdiff3"), List.of("kdiff3", RECV, APPR)),
                new DiffTool("p4merge", List.of("p4merge"), List.of("p4merge", RECV, APPR)),
                new DiffTool("winmerge", List.of("WinMergeU.exe"), List.of("WinMergeU", "/u", "/wl", RECV, APPR)),
                new DiffTool("git", List.of("git"), List.of("git", "difftool", "--no-index", APPR, RECV)));
    }

    private static final String RECV = "{received}";
    private static final String APPR = "{approved}";

    /**
     * Returns the registered tool with the given logical name, or empty when the name isn't one
     * Strictland knows. Backs {@link SnapshotReview#tool(String)} and a {@code strictland.review.tool}
     * value that names a registered tool.
     */
    static Optional<DiffTool> byName(String name) {
        return registry().stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    /**
     * Builds an ad-hoc tool from a full {@code path {received} {approved}} template, for a diff tool
     * that isn't in the registry. The first token is the executable; the rest are its arguments.
     */
    static DiffTool custom(String template) {
        var tokens = List.of(template.trim().split("\\s+", -1));
        return new DiffTool("custom", List.of(tokens.get(0)), tokens);
    }

    /**
     * Resolves a {@code strictland.review.tool} value: a registered tool by name, or, when the value
     * holds spaces, a {@link #custom(String)} template. Never empty for a non-blank value.
     */
    static Optional<DiffTool> fromSetting(String value) {
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        if (trimmed.contains(" ")) {
            return Optional.of(custom(trimmed));
        }
        return byName(trimmed);
    }

    /**
     * Returns the first registry tool whose executable {@code isInstalled} reports as present, or empty
     * when none is. The predicate is the seam that keeps detection testable without probing the real
     * machine.
     */
    static Optional<DiffTool> detect(Predicate<String> isInstalled) {
        for (var tool : registry()) {
            if (tool.candidates().stream().anyMatch(isInstalled)) {
                return Optional.of(tool);
            }
        }
        return Optional.empty();
    }

    /** Returns the first installed registry tool as found on the real {@code PATH}, or empty. */
    static Optional<DiffTool> detect() {
        return detect(DiffTools::onPath);
    }

    /** True when {@code executable} is found as a runnable file on one of the real {@code PATH} entries. */
    static boolean onPath(String executable) {
        return onPath(executable, System.getenv("PATH"));
    }

    /** The pure scan behind {@link #onPath(String)}, taking the {@code PATH} string so it stays testable. */
    static boolean onPath(String executable, @Nullable String path) {
        if (path == null) {
            return false;
        }
        for (var dir : path.split(File.pathSeparator, -1)) {
            if (dir.isEmpty()) {
                continue;
            }
            if (new File(dir, executable).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
