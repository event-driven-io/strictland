package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class FileSnapshotStorageLayoutTests {

    private static final String PACKAGE_PATH = "io/eventdriven/strictland";
    private static final String CALLER = "FileSnapshotStorageLayoutTests";
    private static final byte[] PAYLOAD = "{\"id\":1}".getBytes(UTF_8);

    @Test
    void globalRoot_perTestClass_storesAndReadsAtResolvedPath(@TempDir Path root) {
        var layout = SnapshotLayout.globalRoot(root.toString());
        var storage = new FileSnapshotStorage(layout, ".json");

        storage.store("OrderPlaced", PAYLOAD);

        var expected = root.resolve(PACKAGE_PATH).resolve(CALLER).resolve("OrderPlaced.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
        assertArrayEquals(PAYLOAD, storage.read("OrderPlaced").orElseThrow());
    }

    @Test
    void globalRoot_perContract_groupsUnderMessageType(@TempDir Path root) {
        var layout = SnapshotLayout.globalRoot(root.toString()).grouping(Grouping.PER_CONTRACT);
        var storage = new FileSnapshotStorage(layout, ".json");

        storage.store("OrderPlaced", PAYLOAD);

        var expected = root.resolve(PACKAGE_PATH).resolve("OrderPlaced").resolve("OrderPlaced.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
        assertArrayEquals(PAYLOAD, storage.read("OrderPlaced").orElseThrow());
    }

    @Test
    void nextToTest_perTestClass_storesBesideTestAndReadsBack() throws Exception {
        var wrapper = "snapshots-throwaway";
        var layout = SnapshotLayout.nextToTest().wrapperFolder(wrapper);
        var storage = new FileSnapshotStorage(layout, ".json");
        var expected = Path.of("src/test/java", PACKAGE_PATH, wrapper, CALLER, "OrderPlaced.snap.approved.json");

        try {
            storage.store("OrderPlaced", PAYLOAD);

            assertTrue(Files.exists(expected), "expected snapshot at " + expected);
            assertArrayEquals(PAYLOAD, storage.read("OrderPlaced").orElseThrow());
        } finally {
            deleteWrapper(wrapper);
        }
    }

    @Test
    void nextToTest_perContract_groupsUnderMessageType() throws Exception {
        var wrapper = "snapshots-throwaway-contract";
        var layout = SnapshotLayout.nextToTest().wrapperFolder(wrapper).grouping(Grouping.PER_CONTRACT);
        var storage = new FileSnapshotStorage(layout, ".json");
        var expected = Path.of("src/test/java", PACKAGE_PATH, wrapper, "OrderPlaced", "OrderPlaced.snap.approved.json");

        try {
            storage.store("OrderPlaced", PAYLOAD);

            assertTrue(Files.exists(expected), "expected snapshot at " + expected);
            assertArrayEquals(PAYLOAD, storage.read("OrderPlaced").orElseThrow());
        } finally {
            deleteWrapper(wrapper);
        }
    }

    @Test
    void newLayout_whenNothingApproved_readReturnsEmpty(@TempDir Path root) {
        var storage = new FileSnapshotStorage(SnapshotLayout.globalRoot(root.toString()), ".json");

        assertTrue(storage.read("Missing").isEmpty());
    }

    @Test
    void newLayout_usesSerializerExtensionInFileName(@TempDir Path root) {
        var storage = new FileSnapshotStorage(SnapshotLayout.globalRoot(root.toString()), ".csv");

        storage.store("OrderPlaced", PAYLOAD);

        var expected = root.resolve(PACKAGE_PATH).resolve(CALLER).resolve("OrderPlaced.snap.approved.csv");
        assertEquals(PAYLOAD.length, requireExists(expected));
    }

    private static long requireExists(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            throw new AssertionError("expected snapshot at " + path, e);
        }
    }

    private static void deleteWrapper(String wrapper) throws Exception {
        var dir = Path.of("src/test/java", PACKAGE_PATH, wrapper);
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
