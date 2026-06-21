package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class FileSnapshotStorageLayoutTests {

    private static final String MESSAGE_TYPE = "io.eventdriven.strictland.OrderPlaced";
    private static final byte[] PAYLOAD = "{\"id\":1}".getBytes(UTF_8);

    private static Path registryFolder(Path root) {
        return root.resolve("contract-registry").resolve("io/eventdriven/strictland/OrderPlaced");
    }

    @Test
    void registry_resolvesUnderTheNamespaceFolderAndRoundTrips(@TempDir Path root) {
        var layout = SnapshotLayout.registry().rootPath(root.toString());

        var key = SnapshotName.of(MESSAGE_TYPE, "1", null)
                .resolve(layout, ".json")
                .toString();

        var expected = registryFolder(root).resolve("OrderPlaced.1.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void registry_aDotlessType_resolvesWithNoNamespaceFolderAndRoundTrips(@TempDir Path root) {
        var layout = SnapshotLayout.registry().rootPath(root.toString());

        var key = SnapshotName.of("OrderPlaced", "1", null)
                .resolve(layout, ".json")
                .toString();

        var expected =
                root.resolve("contract-registry").resolve("OrderPlaced").resolve("OrderPlaced.1.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void registry_wrapperFolderOverride_replacesTheWrapperSegment(@TempDir Path root) {
        var layout = SnapshotLayout.registry().rootPath(root.toString()).wrapperFolder("approved");

        var key = SnapshotName.of(MESSAGE_TYPE, "1", null)
                .resolve(layout, ".json")
                .toString();

        var expected = root.resolve("approved")
                .resolve("io/eventdriven/strictland/OrderPlaced")
                .resolve("OrderPlaced.1.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void registry_aVariant_namesTheFileAndReadsBack(@TempDir Path root) {
        var layout = SnapshotLayout.registry().rootPath(root.toString());

        var key = SnapshotName.of(MESSAGE_TYPE, "1", "WithPromotion")
                .resolve(layout, ".json")
                .toString();

        var expected = registryFolder(root).resolve("OrderPlaced.1.WithPromotion.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void usesSerializerExtensionInFileName(@TempDir Path root) {
        var layout = SnapshotLayout.registry().rootPath(root.toString());

        var key =
                SnapshotName.of(MESSAGE_TYPE, "1", null).resolve(layout, ".csv").toString();

        assertTrue(key.endsWith(".snap.approved.csv"), key);
    }

    @Test
    void store_writesBytesAtTheResolvedPath_andReadReturnsThem(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();
        var key = dir.resolve("Direct.snap.approved.json").toString();

        storage.store(key, PAYLOAD);

        assertTrue(Files.exists(Path.of(key)));
        assertArrayEquals(PAYLOAD, storage.read(key).orElseThrow());
    }

    @Test
    void read_whenNothingApproved_returnsEmpty(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();

        assertTrue(storage.read(dir.resolve("Missing.snap.approved.json").toString())
                .isEmpty());
    }

    @Test
    void store_rejectsAPathWithoutTheApprovedMarker(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();
        var key = dir.resolve("no-marker.json").toString();

        assertThrows(IllegalArgumentException.class, () -> storage.store(key, PAYLOAD));
    }

    @Test
    void store_rejectsAPathWithoutAParentDirectory() {
        var storage = new FileSnapshotStorage();

        assertThrows(IllegalArgumentException.class, () -> storage.store("OrphanLeaf.approved.txt", PAYLOAD));
    }

    @Test
    void store_anUnchangedPayload_passesAndClearsAnyReceivedFile(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();
        var key = dir.resolve("Stable.snap.approved.json").toString();

        storage.store(key, PAYLOAD);
        storage.store(key, PAYLOAD);

        assertTrue(Files.exists(Path.of(key)));
        assertTrue(Files.notExists(dir.resolve("Stable.snap.received.json")));
    }

    @Test
    void store_aDriftedPayload_writesAReceivedFileAndThrows(@TempDir Path dir) throws Exception {
        var storage = new FileSnapshotStorage();
        var key = dir.resolve("Drifting.snap.approved.json").toString();
        storage.store(key, PAYLOAD);

        var error = assertThrows(AssertionError.class, () -> storage.store(key, "{\"id\":2}".getBytes(UTF_8)));

        assertTrue(requireNonNull(error.getMessage()).contains("drift"));
        assertArrayEquals("{\"id\":2}".getBytes(UTF_8), Files.readAllBytes(dir.resolve("Drifting.snap.received.json")));
    }

    @Test
    void store_whenTheDirectoryCannotBeCreated_wrapsTheIoFailure(@TempDir Path dir) throws Exception {
        var blocker = dir.resolve("blocker");
        Files.writeString(blocker, "not a directory");
        var storage = new FileSnapshotStorage();
        var key = blocker.resolve("child.snap.approved.json").toString();

        assertThrows(UncheckedIOException.class, () -> storage.store(key, PAYLOAD));
    }

    @Test
    void readAll_whenNothingMatchesThePrefix_returnsEmpty(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir, "OrderPlaced.1.", null);

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_whenFolderDoesNotExist_returnsEmpty(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir.resolve("missing"), "OrderPlaced.1.", null);

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_whenTheFolderCannotBeListed_wrapsTheIoFailure(@TempDir Path dir) throws Exception {
        var unreadable = Files.createDirectory(dir.resolve("unreadable"));
        try {
            Files.setPosixFilePermissions(
                    unreadable, java.nio.file.attribute.PosixFilePermissions.fromString("-wx------"));
        } catch (UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("POSIX permissions are not supported here");
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(Files.isReadable(unreadable), "directory is still readable");
        var storage = new FileSnapshotStorage();
        try {
            assertThrows(UncheckedIOException.class, () -> storage.readAll(unreadable, "OrderPlaced.1.", null));
        } finally {
            Files.setPosixFilePermissions(
                    unreadable, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    void readAll_whenFolderIsNull_returnsEmpty() {
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(null, "OrderPlaced.1.", null);

        assertTrue(payloads.isEmpty());
    }

    @Test
    void defaultReadAll_readsTheExactPrefixName_forACustomStoreKeyedByName() {
        var stored = "{\"id\":7}".getBytes(UTF_8);
        var storage = new SnapshotStorage() {
            @Override
            public void store(String name, byte[] payload) {}

            @Override
            public java.util.Optional<byte[]> read(String name) {
                return name.equals("OrderPlaced.1.") ? java.util.Optional.of(stored) : java.util.Optional.empty();
            }
        };

        var payloads = storage.readAll(null, "OrderPlaced.1.", null);

        assertEquals(1, payloads.size());
        assertArrayEquals(stored, payloads.get(0));
    }

    @Test
    void defaultReadAll_whenTheExactPrefixNameIsAbsent_returnsEmpty() {
        var storage = new SnapshotStorage() {
            @Override
            public void store(String name, byte[] payload) {}

            @Override
            public java.util.Optional<byte[]> read(String name) {
                return java.util.Optional.empty();
            }
        };

        assertTrue(storage.readAll(null, "Missing.1.", null).isEmpty());
    }

    @Test
    void readAll_returnsEveryMatchingApprovedFile_sortedByName(@TempDir Path dir) throws Exception {
        var first = "{\"id\":1}".getBytes(UTF_8);
        var second = "{\"id\":2}".getBytes(UTF_8);
        var other = "{\"id\":9}".getBytes(UTF_8);
        Files.write(dir.resolve("OrderPlaced.1.B.snap.approved.json"), second);
        Files.write(dir.resolve("OrderPlaced.1.A.snap.approved.json"), first);
        Files.write(dir.resolve("OrderShipped.1.A.snap.approved.json"), other);
        Files.write(dir.resolve("OrderPlaced.1.A.snap.received.json"), other);
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir, "OrderPlaced.1.", null);

        assertEquals(2, payloads.size());
        assertArrayEquals(first, payloads.get(0));
        assertArrayEquals(second, payloads.get(1));
    }

    @Test
    void readAll_withVariant_returnsOnlyTheMatchingVariantFile(@TempDir Path dir) throws Exception {
        var withPromotion = "{\"id\":1}".getBytes(UTF_8);
        var noPromotion = "{\"id\":2}".getBytes(UTF_8);
        Files.write(dir.resolve("OrderPlaced.1.WithPromotion.snap.approved.json"), withPromotion);
        Files.write(dir.resolve("OrderPlaced.1.NoPromotion.snap.approved.json"), noPromotion);
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir, "OrderPlaced.1.", "WithPromotion");

        assertEquals(1, payloads.size());
        assertArrayEquals(withPromotion, payloads.get(0));
    }

    @Test
    void readAll_withVariant_thatMatchesNothing_isEmpty(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("OrderPlaced.1.WithPromotion.snap.approved.json"), "{\"id\":1}".getBytes(UTF_8));
        Files.write(dir.resolve("OrderPlaced.1.NoPromotion.snap.approved.json"), "{\"id\":2}".getBytes(UTF_8));
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir, "OrderPlaced.1.", "DoesNotExist");

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_withVariant_whenNoSnapMarker_matchesAgainstTheWholeFileName(@TempDir Path dir) throws Exception {
        var payload = "{\"id\":1}".getBytes(UTF_8);
        Files.write(dir.resolve("OrderPlaced.1.approved.json.WithPromotion"), payload);
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(dir, "OrderPlaced.1.", "WithPromotion");

        assertEquals(1, payloads.size());
        assertArrayEquals(payload, payloads.get(0));
    }

    private static void assertRoundTrips(String key) {
        var storage = new FileSnapshotStorage();
        storage.store(key, PAYLOAD);
        assertArrayEquals(PAYLOAD, storage.read(key).orElseThrow());
    }
}
