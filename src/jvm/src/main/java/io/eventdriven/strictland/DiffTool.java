package io.eventdriven.strictland;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A diff tool Strictland knows how to launch on a drifted snapshot's received and approved files. Pick
 * one with {@link SnapshotReview#tool(DiffTool)} when you have a favourite; otherwise {@link
 * SnapshotReview#auto()} tries them in the order declared here and launches the first one installed,
 * with {@link #GIT}'s no-index difftool as the always-available fallback. For a tool not listed here,
 * use {@link SnapshotReview#customTool(String)}.
 *
 * <pre>
 * SpecificationOptions options = Json.Jackson.defaults().snapshotReview(SnapshotReview.tool(DiffTool.MELD));
 * </pre>
 */
public enum DiffTool {

    /** Visual Studio Code, launched as {@code code --diff}. */
    VSCODE(List.of("code", "code.cmd"), List.of("--diff", "--wait", "{received}", "{approved}")),

    /** IntelliJ IDEA, launched as {@code idea diff}. */
    IDEA(List.of("idea", "idea.sh", "idea.cmd"), List.of("diff", "{received}", "{approved}")),

    /** Meld. */
    MELD(List.of("meld"), List.of("{received}", "{approved}")),

    /** Beyond Compare. */
    BCOMPARE(List.of("bcompare", "BCompare.exe"), List.of("{received}", "{approved}")),

    /** KDiff3. */
    KDIFF3(List.of("kdiff3"), List.of("{received}", "{approved}")),

    /** Perforce P4Merge. */
    P4MERGE(List.of("p4merge"), List.of("{received}", "{approved}")),

    /** WinMerge. */
    WINMERGE(List.of("WinMergeU.exe"), List.of("/u", "/wl", "{received}", "{approved}")),

    /** Git's {@code difftool --no-index}, the fallback that works wherever Git is installed. */
    GIT(List.of("git"), List.of("difftool", "--no-index", "{approved}", "{received}"));

    private static final String RECEIVED = "{received}";
    private static final String APPROVED = "{approved}";

    // List.copyOf yields an unmodifiable list; the enum stays effectively immutable.
    @SuppressWarnings("ImmutableEnumChecker")
    private final List<String> candidates;

    @SuppressWarnings("ImmutableEnumChecker")
    private final List<String> arguments;

    DiffTool(List<String> candidates, List<String> arguments) {
        this.candidates = List.copyOf(candidates);
        this.arguments = List.copyOf(arguments);
    }

    /**
     * The command to launch this tool on the given pair, or empty when none of its candidate
     * executables is installed. Trying each candidate covers a tool's platform-specific names and
     * install locations (say {@code code} on the path and an absolute install path as a fallback).
     */
    Optional<List<String>> command(Path received, Path approved, Predicate<String> installed) {
        for (var candidate : candidates) {
            if (installed.test(candidate)) {
                return Optional.of(render(candidate, received, approved));
            }
        }
        return Optional.empty();
    }

    static Optional<DiffTool> byName(String name) {
        try {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException notARegisteredTool) {
            return Optional.empty();
        }
    }

    static List<String> knownNames() {
        var names = new ArrayList<String>(values().length);
        for (var tool : values()) {
            names.add(tool.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(names);
    }

    private List<String> render(String executable, Path received, Path approved) {
        var argv = new ArrayList<String>(arguments.size() + 1);
        argv.add(executable);
        for (var token : arguments) {
            argv.add(token.replace(RECEIVED, received.toString()).replace(APPROVED, approved.toString()));
        }
        return List.copyOf(argv);
    }
}
