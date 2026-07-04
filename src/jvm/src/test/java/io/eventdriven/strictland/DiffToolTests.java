package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void command_substitutesReceivedAndApprovedIntoTheTemplate() {
        var meld = new DiffTool("meld", List.of("meld"), List.of("meld", "{received}", "{approved}"));

        var argv = meld.command("/tmp/x.received.json", "/tmp/x.approved.json");

        assertEquals(List.of("meld", "/tmp/x.received.json", "/tmp/x.approved.json"), argv);
    }

    @Test
    void command_leavesLiteralArgumentsUntouched() {
        var winmerge = new DiffTool(
                "winmerge", List.of("WinMergeU.exe"), List.of("WinMergeU", "/u", "/wl", "{received}", "{approved}"));

        var argv = winmerge.command("recv", "appr");

        assertEquals(List.of("WinMergeU", "/u", "/wl", "recv", "appr"), argv);
    }

    @Test
    void byName_returnsTheRegisteredTool() {
        var tool = DiffTools.byName("meld").orElseThrow();

        assertEquals("meld", tool.name());
        assertEquals(List.of("meld", "recv", "appr"), tool.command("recv", "appr"));
    }

    @Test
    void byName_isEmptyForAnUnknownName() {
        assertTrue(DiffTools.byName("no-such-tool").isEmpty());
    }

    @Test
    void gitFallback_putsApprovedOnTheLeftForDifftool() {
        var git = DiffTools.byName("git").orElseThrow();

        assertEquals(List.of("git", "difftool", "--no-index", "appr", "recv"), git.command("recv", "appr"));
    }

    @Test
    void custom_buildsAToolFromAFullTemplate() {
        var tool = DiffTools.custom("my-diff --wait {received} {approved}");

        assertEquals("custom", tool.name());
        assertEquals(List.of("my-diff", "--wait", "recv", "appr"), tool.command("recv", "appr"));
    }

    @Test
    void fromSetting_resolvesARegisteredNameOrACustomTemplate() {
        assertEquals("meld", DiffTools.fromSetting("meld").orElseThrow().name());
        assertEquals(
                "custom",
                DiffTools.fromSetting("my-diff {received} {approved}")
                        .orElseThrow()
                        .name());
    }

    @Test
    void fromSetting_isEmptyForBlankOrUnknown() {
        assertTrue(DiffTools.fromSetting("   ").isEmpty());
        assertTrue(DiffTools.fromSetting("no-such-tool").isEmpty());
    }

    @Test
    void detect_returnsTheFirstInstalledToolInRegistryOrder() {
        // Only "meld" is reported installed, so it wins over the tools listed before it.
        var detected = DiffTools.detect(exe -> exe.equals("meld")).orElseThrow();

        assertEquals("meld", detected.name());
    }

    @Test
    void detect_isEmptyWhenNothingIsInstalled() {
        assertTrue(DiffTools.detect(exe -> false).isEmpty());
    }

    @Test
    void onPath_findsARunnableInAPathEntryWhileSkippingEmptyOnes(@TempDir Path dir) throws IOException {
        var tool = Files.createFile(dir.resolve("faux-diff")).toFile();
        assertTrue(tool.setExecutable(true));
        // Leading empty entry exercises the skip; the temp dir holds the runnable.
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

    @Test
    void onPath_overTheRealPathResolvesAToolThatExists() {
        assertTrue(DiffTools.onPath("sh"), "sh should be on PATH in the dev container");
    }

    @Test
    void detect_overTheRealPathResolvesWithoutError() {
        // git is present in the dev container, so real detection finds a tool.
        assertTrue(DiffTools.detect().isPresent());
    }
}
