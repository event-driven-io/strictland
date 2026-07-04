package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class DiffToolTests {

    @Test
    void resolvedCommandUsesTheExecutableThatWasActuallyFound() {
        var vscode = DiffTools.byName("vscode").orElseThrow();

        var resolved = vscode.resolve(exe -> exe.equals("code.cmd")).orElseThrow();

        assertEquals(List.of("code.cmd", "--diff", "--wait", "recv", "appr"), resolved.command("recv", "appr"));
    }

    @Test
    void executablePathCandidateResolvesEvenWhenItIsNotOnThePath(@TempDir Path dir) throws IOException {
        var executable = Files.createFile(dir.resolve("acme-diff")).toFile();
        assertTrue(executable.setExecutable(true));
        var tool = new DiffTool("acme", List.of(executable.getAbsolutePath()), List.of("{received}", "{approved}"));

        var resolved = tool.resolve(candidate -> false).orElseThrow();

        assertEquals(executable.getAbsolutePath(), resolved.executable());
    }

    @Test
    void nonExecutablePathCandidateDoesNotResolve(@TempDir Path dir) {
        var tool = new DiffTool("acme", List.of(dir.resolve("missing-diff").toString()), List.of("{received}"));

        assertTrue(tool.resolve(candidate -> false).isEmpty());
    }

    @Test
    void gitFallback_putsApprovedOnTheLeftForDifftool() {
        var git = DiffTools.byName("git").orElseThrow();

        var resolved = git.resolve(exe -> exe.equals("git")).orElseThrow();

        assertEquals(List.of("git", "difftool", "--no-index", "appr", "recv"), resolved.command("recv", "appr"));
    }

    @Test
    void explicitSingleToolWinsAndDoesNotFallbackWhenUnavailable() {
        var resolved = DiffTools.resolve(ToolPreference.single("idea"), exe -> exe.equals("code"));

        assertTrue(resolved.isEmpty());
    }

    @Test
    void explicitSingleToolCoversTheAvailablePath() {
        var resolved = DiffTools.resolve(ToolPreference.single("meld"), exe -> exe.equals("meld"))
                .orElseThrow();

        assertEquals("meld", resolved.name());
    }

    @Test
    void explicitOrderPutsListedAvailableToolsBeforeUnlistedTools() {
        var preference = ToolPreference.order(List.of("meld", "idea"));

        var resolved = DiffTools.resolve(preference, exe -> exe.equals("idea") || exe.equals("code"))
                .orElseThrow();

        assertEquals("idea", resolved.name());
    }

    @Test
    void builtInRegistryOrderAppliesWhenNoPreferenceExists() {
        var resolved = DiffTools.resolve(null, exe -> exe.equals("meld") || exe.equals("git"))
                .orElseThrow();

        assertEquals("meld", resolved.name());
    }

    @Test
    void gitFallbackIsLastAndOnlyUsedWhenResolved() {
        assertTrue(DiffTools.resolve(null, exe -> false).isEmpty());
        assertEquals(
                "git",
                DiffTools.resolve(null, exe -> exe.equals("git")).orElseThrow().name());
    }

    @Test
    void customConfigCommandResolvesOnlyWhenTheExecutableExists() {
        var resolved = DiffTools.resolve(
                        ToolPreference.custom("my-diff --wait {received} {approved}"), exe -> exe.equals("my-diff"))
                .orElseThrow();

        assertEquals(List.of("my-diff", "--wait", "recv", "appr"), resolved.command("recv", "appr"));
        assertTrue(DiffTools.resolve(ToolPreference.custom("missing {received} {approved}"), exe -> false)
                .isEmpty());
    }

    @Test
    void customConfigCommandCanResolveByExecutablePath(@TempDir Path dir) throws IOException {
        var executable = Files.createFile(dir.resolve("custom-diff")).toFile();
        assertTrue(executable.setExecutable(true));

        var resolved = DiffTools.resolve(
                        ToolPreference.custom(executable.getAbsolutePath() + " {received} {approved}"), exe -> false)
                .orElseThrow();

        assertEquals(executable.getAbsolutePath(), resolved.executable());
    }

    @Test
    void parseOrderAcceptsCommaPipeWhitespaceAndBlankSegments() {
        assertEquals(List.of("idea", "vscode", "meld"), DiffTools.parseOrder("idea,vscode|meld"));
        assertEquals(List.of("idea", "vscode"), DiffTools.parseOrder("idea  vscode"));
    }

    @Test
    void unknownAndBlankToolNamesAreRejected() {
        assertTrue(DiffTools.byName("no-such-tool").isEmpty());
        assertTrue(requireNonNull(assertThrows(IllegalArgumentException.class, () -> DiffTools.requireName("   "))
                        .getMessage())
                .contains("Unknown diff tool"));
        assertTrue(requireNonNull(assertThrows(IllegalArgumentException.class, () -> DiffTools.fromToolSetting("   "))
                        .getMessage())
                .contains("blank"));
        assertTrue(requireNonNull(assertThrows(IllegalArgumentException.class, () -> DiffTools.parseOrder("   "))
                        .getMessage())
                .contains("at least one"));
    }

    @Test
    void onPath_findsARunnableInAPathEntryWhileSkippingEmptyOnes(@TempDir Path dir) throws IOException {
        var tool = Files.createFile(dir.resolve("faux-diff")).toFile();
        assertTrue(tool.setExecutable(true));
        var path = File.pathSeparator + dir;

        assertTrue(DiffTools.onPath("faux-diff", path));
    }

    @Test
    void onPath_isFalseWhenNoEntryHoldsARunnable(@TempDir Path dir) {
        assertFalse(DiffTools.onPath("faux-diff", dir.resolve("nope").toString()));
    }

    @Test
    void onPath_isFalseWhenPathIsUnset() {
        assertFalse(DiffTools.onPath("anything", null));
    }
}
