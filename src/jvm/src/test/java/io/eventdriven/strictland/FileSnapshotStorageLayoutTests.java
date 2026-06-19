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

    private static final String PACKAGE_PATH = "io/eventdriven/strictland";
    private static final String CALLER = "FileSnapshotStorageLayoutTests";
    private static final byte[] PAYLOAD = "{\"id\":1}".getBytes(UTF_8);

    private static SnapshotLocation anchoredAt(Path testSourceDir, SnapshotLayout layout, String extension) {
        return new SnapshotLocation(layout, extension, (packageName, sourceFileName) -> testSourceDir);
    }

    @Test
    void globalRoot_perTestClass_resolvesUnderRootAndRoundTrips(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.globalRoot(root.toString()), ".json");

        var key = location.resolve("OrderPlaced", "OrderPlaced");

        var expected = root.resolve(PACKAGE_PATH).resolve(CALLER).resolve("OrderPlaced.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void globalRoot_perContract_groupsUnderMessageType(@TempDir Path root) {
        var location = new SnapshotLocation(
                SnapshotLayout.globalRoot(root.toString()).grouping(SnapshotGrouping.PER_CONTRACT), ".json");

        var key = location.resolve("OrderPlaced", "OrderPlaced");

        var expected = root.resolve(PACKAGE_PATH).resolve("OrderPlaced").resolve("OrderPlaced.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void nextToTest_perTestClass_anchorsOnTheTestSourceDir(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest(), ".json");

        var key = location.resolve("OrderPlaced", "OrderPlaced");

        var expected = src.resolve("snapshots").resolve(CALLER).resolve("OrderPlaced.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void nextToTest_perContract_groupsUnderMessageType(@TempDir Path src) {
        var location = anchoredAt(src, SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT), ".json");

        var key = location.resolve("OrderPlaced", "OrderPlaced");

        var expected = src.resolve("snapshots").resolve("OrderPlaced").resolve("OrderPlaced.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void perContract_aLeafDistinctFromTheGroup_namesTheFileAndReadsBack(@TempDir Path root) {
        var location = new SnapshotLocation(
                SnapshotLayout.globalRoot(root.toString()).grouping(SnapshotGrouping.PER_CONTRACT), ".json");

        var key = location.resolve("OrderInitiated", "WithPromotion");

        var expected = root.resolve(PACKAGE_PATH).resolve("OrderInitiated").resolve("WithPromotion.snap.approved.json");
        assertEquals(expected.toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void noneGrouping_emptyWrapper_usesTheLeafAsTheFileName(@TempDir Path src) {
        var location = anchoredAt(
                src, SnapshotLayout.nextToTest().grouping(SnapshotGrouping.NONE).wrapperFolder(""), ".json");

        var key = location.resolve("OrderInitiated", "FlatLeaf");

        assertEquals(src.resolve("FlatLeaf.snap.approved.json").toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void noneGrouping_emptyWrapper_whenLeafEqualsTheGroup_keepsTheContractNameAsFileName(@TempDir Path src) {
        var location = anchoredAt(
                src, SnapshotLayout.nextToTest().grouping(SnapshotGrouping.NONE).wrapperFolder(""), ".json");

        var key = location.resolve("FlatContract", "FlatContract");

        assertEquals(src.resolve("FlatContract.snap.approved.json").toString(), key);
        assertRoundTrips(key);
    }

    @Test
    void usesSerializerExtensionInFileName(@TempDir Path root) {
        var location = new SnapshotLocation(SnapshotLayout.globalRoot(root.toString()), ".csv");

        var key = location.resolve("OrderPlaced", "OrderPlaced");

        assertTrue(key.endsWith("OrderPlaced.snap.approved.csv"), key);
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

        var payloads = storage.readAll(new SnapshotReadLocation(dir, "OrderPlaced.1."));

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_whenFolderDoesNotExist_returnsEmpty(@TempDir Path dir) {
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(new SnapshotReadLocation(dir.resolve("missing"), "OrderPlaced.1."));

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
            assertThrows(
                    UncheckedIOException.class,
                    () -> storage.readAll(new SnapshotReadLocation(unreadable, "OrderPlaced.1.")));
        } finally {
            Files.setPosixFilePermissions(
                    unreadable, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    void readAll_whenFolderIsNull_returnsEmpty() {
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(new SnapshotReadLocation(null, "OrderPlaced.1."));

        assertTrue(payloads.isEmpty());
    }

    @Test
    void resolve_whenNoLayoutIsConfigured_failsBecauseTheLocationCannotPlaceTheFile() {
        var location = new SnapshotLocation(
                null, ".json", TestClassNaming.SIMPLE, (packageName, sourceFileName) -> Path.of(""));

        assertThrows(IllegalStateException.class, () -> location.resolve("OrderPlaced", "OrderPlaced"));
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

        var payloads = storage.readAll(new SnapshotReadLocation(null, "OrderPlaced.1."));

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

        assertTrue(storage.readAll(new SnapshotReadLocation(null, "Missing.1.")).isEmpty());
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

        var payloads = storage.readAll(new SnapshotReadLocation(dir, "OrderPlaced.1."));

        assertEquals(2, payloads.size());
        assertArrayEquals(first, payloads.get(0));
        assertArrayEquals(second, payloads.get(1));
    }

    @Test
    void readAll_withVariant_returnsOnlyTheMatchingVariantFile(@TempDir Path dir) throws Exception {
        var withPromotion = "{\"id\":1}".getBytes(UTF_8);
        var noPromotion = "{\"id\":2}".getBytes(UTF_8);
        Files.write(dir.resolve("OrderPlaced.1.SomeTest.someTest.WithPromotion.snap.approved.json"), withPromotion);
        Files.write(dir.resolve("OrderPlaced.1.SomeTest.someTest.NoPromotion.snap.approved.json"), noPromotion);
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(new SnapshotReadLocation(dir, "OrderPlaced.1.", "WithPromotion"));

        assertEquals(1, payloads.size());
        assertArrayEquals(withPromotion, payloads.get(0));
    }

    @Test
    void readAll_withVariant_thatMatchesNothing_isEmpty(@TempDir Path dir) throws Exception {
        Files.write(
                dir.resolve("OrderPlaced.1.SomeTest.someTest.WithPromotion.snap.approved.json"),
                "{\"id\":1}".getBytes(UTF_8));
        Files.write(
                dir.resolve("OrderPlaced.1.SomeTest.someTest.NoPromotion.snap.approved.json"),
                "{\"id\":2}".getBytes(UTF_8));
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(new SnapshotReadLocation(dir, "OrderPlaced.1.", "DoesNotExist"));

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_withVariant_whenNoSnapMarker_matchesAgainstTheWholeFileName(@TempDir Path dir) throws Exception {
        var payload = "{\"id\":1}".getBytes(UTF_8);
        Files.write(dir.resolve("OrderPlaced.1.SomeTest.approved.json.WithPromotion"), payload);
        var storage = new FileSnapshotStorage();

        var payloads = storage.readAll(new SnapshotReadLocation(dir, "OrderPlaced.1.", "WithPromotion"));

        assertEquals(1, payloads.size());
        assertArrayEquals(payload, payloads.get(0));
    }

    private static void assertRoundTrips(String key) {
        var storage = new FileSnapshotStorage();
        storage.store(key, PAYLOAD);
        assertArrayEquals(PAYLOAD, storage.read(key).orElseThrow());
    }
}
