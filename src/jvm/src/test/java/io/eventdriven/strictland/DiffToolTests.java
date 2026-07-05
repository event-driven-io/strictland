package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private static final Path RECEIVED = Path.of("recv");
    private static final Path APPROVED = Path.of("appr");

    @Test
    void commandUsesTheCandidateThatWasActuallyFound() {
        var command = DiffTool.VSCODE.command(RECEIVED, APPROVED, exe -> exe.equals("code.cmd"));

        assertEquals(List.of("code.cmd", "--diff", "--wait", "recv", "appr"), command.orElseThrow());
    }

    @Test
    void commandIsEmptyWhenNoCandidateIsInstalled() {
        assertTrue(DiffTool.MELD.command(RECEIVED, APPROVED, exe -> false).isEmpty());
    }

    @Test
    void explicitToolWinsAndDoesNotFallBackWhenUnavailable() {
        var command = DiffTools.command(
                new ToolPreference.Named(DiffTool.IDEA), RECEIVED, APPROVED, exe -> exe.equals("code"), () -> false);

        assertTrue(command.isEmpty());
    }

    @Test
    void explicitToolCoversTheAvailablePath() {
        var command = DiffTools.command(
                new ToolPreference.Named(DiffTool.MELD), RECEIVED, APPROVED, exe -> exe.equals("meld"), () -> false);

        assertEquals(List.of("meld", "recv", "appr"), command.orElseThrow());
    }

    @Test
    void auto_opensTheGitConfiguredDifftoolWithApprovedOnTheLeft() {
        var command = DiffTools.command(null, RECEIVED, APPROVED, exe -> exe.equals("git"), () -> true);

        assertEquals(List.of("git", "difftool", "--no-index", "appr", "recv"), command.orElseThrow());
    }

    @Test
    void auto_fallsBackToTheFirstInstalledGuiWhenGitHasNoConfiguredTool() {
        // git is "installed" here too, but with no configured diff.tool it must not hijack the GUI probe.
        var command = DiffTools.command(
                null, RECEIVED, APPROVED, exe -> exe.equals("meld") || exe.equals("git"), () -> false);

        assertEquals(List.of("meld", "recv", "appr"), command.orElseThrow());
    }

    @Test
    void auto_fallsBackToTheGuiProbeWhenGitIsConfiguredButNotInstalled() {
        var command = DiffTools.command(null, RECEIVED, APPROVED, exe -> exe.equals("meld"), () -> true);

        assertEquals(List.of("meld", "recv", "appr"), command.orElseThrow());
    }

    @Test
    void auto_isEmptyWhenNothingIsAvailable() {
        assertTrue(DiffTools.command(null, RECEIVED, APPROVED, exe -> false, () -> false)
                .isEmpty());
    }

    @Test
    void gitDiffToolConfigured_isTrueWhenGitReportsAConfiguredTool() {
        assertTrue(DiffTools.gitDiffToolConfigured(new ProcessBuilder("echo", "meld")));
    }

    @Test
    void gitDiffToolConfigured_isFalseWhenGitReportsNothing() {
        assertFalse(DiffTools.gitDiffToolConfigured(new ProcessBuilder("true")));
    }

    @Test
    void gitDiffToolConfigured_isFalseWhenTheProbeCannotStart() {
        assertFalse(DiffTools.gitDiffToolConfigured(new ProcessBuilder("strictland-no-such-binary")));
    }

    @Test
    void gitDiffToolConfigured_probesTheRealGitConfigWithoutThrowing() {
        assertDoesNotThrow(() -> DiffTools.gitDiffToolConfigured());
    }

    @Test
    void customConfigCommandResolvesOnlyWhenTheExecutableExists() {
        var command = DiffTools.command(
                new ToolPreference.Custom("my-diff --wait {received} {approved}"),
                RECEIVED,
                APPROVED,
                exe -> exe.equals("my-diff"),
                () -> false);

        assertEquals(List.of("my-diff", "--wait", "recv", "appr"), command.orElseThrow());
        assertTrue(DiffTools.command(
                        new ToolPreference.Custom("missing {received} {approved}"),
                        RECEIVED,
                        APPROVED,
                        exe -> false,
                        () -> false)
                .isEmpty());
    }

    @Test
    void customConfigCommandCanResolveByExecutablePath(@TempDir Path dir) throws IOException {
        var executable = Files.createFile(dir.resolve("custom-diff")).toFile();
        assertTrue(executable.setExecutable(true));

        var command = DiffTools.command(
                new ToolPreference.Custom(executable.getAbsolutePath() + " {received} {approved}"),
                RECEIVED,
                APPROVED,
                DiffTools::onPath,
                () -> false);

        assertEquals(executable.getAbsolutePath(), command.orElseThrow().get(0));
    }

    @Test
    void unknownAndBlankToolNamesAreRejected() {
        assertTrue(DiffTool.byName("no-such-tool").isEmpty());
        assertTrue(requireNonNull(assertThrows(IllegalArgumentException.class, () -> DiffTools.requireName("   "))
                        .getMessage())
                .contains("Unknown diff tool"));
        assertTrue(requireNonNull(assertThrows(IllegalArgumentException.class, () -> DiffTools.fromToolSetting("   "))
                        .getMessage())
                .contains("blank"));
    }

    @Test
    void onPath_findsARunnableInAPathEntryWhileSkippingEmptyOnes(@TempDir Path dir) throws IOException {
        var tool = Files.createFile(dir.resolve("faux-diff")).toFile();
        assertTrue(tool.setExecutable(true));
        var path = File.pathSeparator + dir;

        assertTrue(DiffTools.onPath("faux-diff", path));
    }

    @Test
    void onPath_resolvesAnAbsolutePathCandidateWithoutSearchingThePath(@TempDir Path dir) throws IOException {
        var tool = Files.createFile(dir.resolve("faux-diff")).toFile();
        assertTrue(tool.setExecutable(true));

        assertTrue(DiffTools.onPath(tool.getAbsolutePath(), null));
        assertFalse(DiffTools.onPath(dir.resolve("missing").toString(), null));
    }

    @Test
    void onPath_isFalseWhenNoEntryHoldsARunnable(@TempDir Path dir) {
        assertFalse(DiffTools.onPath("faux-diff", dir.resolve("nope").toString()));
    }

    @Test
    void onPath_isFalseWhenPathIsUnset() {
        assertFalse(DiffTools.onPath("anything", null));
    }

    @Test
    void onPath_singleArg_readsTheProcessPathAndDoesNotFindABogusBinary() {
        assertFalse(DiffTools.onPath("strictland-no-such-binary"));
    }
}
