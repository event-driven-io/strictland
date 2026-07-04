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

    private static final MessageTypeName MESSAGE_TYPE = MessageTypeName.of("io.eventdriven.strictland.OrderPlaced");
    private static final MessageTypeName DOTLESS_TYPE = MessageTypeName.of("OrderPlaced");
    private static final byte[] PAYLOAD = "{\"id\":1}".getBytes(UTF_8);

    private static FileSnapshotStorage storageRootedAt(Path root) {
        return new FileSnapshotStorage(SnapshotLayout.registry().rootPath(root.toString()));
    }

    // A drift here should assert on files and the thrown error only, never open a real diff tool,
    // whatever machine the suite runs on - so wire a no-op launcher and a non-interactive gate.
    private static FileSnapshotStorage nonInteractiveStorage(SnapshotLayout layout) {
        return new FileSnapshotStorage(layout, SnapshotReview.auto(), (tool, received, approved) -> {}, () -> true);
    }

    private static Path registryFolder(Path root) {
        return root.resolve("contract-registry").resolve("io/eventdriven/strictland/OrderPlaced");
    }

    @Test
    void registry_resolvesUnderTheNamespaceFolderAndRoundTrips(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");

        storage.store(location, new SnapshotData(PAYLOAD));

        var expected = registryFolder(root).resolve("OrderPlaced.1.default.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
        assertArrayEquals(PAYLOAD, storage.read(location).bytes());
    }

    @Test
    void registry_aDotlessType_resolvesWithNoNamespaceFolderAndRoundTrips(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(DOTLESS_TYPE, "1", SnapshotVariant.UNSET, ".json");

        storage.store(location, new SnapshotData(PAYLOAD));

        var expected = root.resolve("contract-registry")
                .resolve("OrderPlaced")
                .resolve("OrderPlaced.1.default.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
        assertArrayEquals(PAYLOAD, storage.read(location).bytes());
    }

    @Test
    void registry_wrapperFolderOverride_replacesTheWrapperSegment(@TempDir Path root) {
        var storage = new FileSnapshotStorage(
                SnapshotLayout.registry().rootPath(root.toString()).wrapperFolder("approved"));
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");

        storage.store(location, new SnapshotData(PAYLOAD));

        var expected = root.resolve("approved")
                .resolve("io/eventdriven/strictland/OrderPlaced")
                .resolve("OrderPlaced.1.default.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
    }

    @Test
    void registry_aVariant_namesTheFileAndReadsBack(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.named("WithPromotion"), ".json");

        storage.store(location, new SnapshotData(PAYLOAD));

        var expected = registryFolder(root).resolve("OrderPlaced.1.WithPromotion.snap.approved.json");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
        assertArrayEquals(PAYLOAD, storage.read(location).bytes());
    }

    @Test
    void usesSerializerExtensionInFileName(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".csv");

        storage.store(location, new SnapshotData(PAYLOAD));

        var expected = registryFolder(root).resolve("OrderPlaced.1.default.snap.approved.csv");
        assertTrue(Files.exists(expected), "expected snapshot at " + expected);
    }

    @Test
    void read_whenNothingApproved_throws(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MessageTypeName.of("Missing"), "1", SnapshotVariant.UNSET, ".json");

        assertThrows(UncheckedIOException.class, () -> storage.read(location));
    }

    @Test
    void store_anUnchangedPayload_passesAndClearsAnyReceivedFile(@TempDir Path root) {
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");

        storage.store(location, new SnapshotData(PAYLOAD));
        storage.store(location, new SnapshotData(PAYLOAD));

        var approved = registryFolder(root).resolve("OrderPlaced.1.default.snap.approved.json");
        assertTrue(Files.exists(approved));
        assertTrue(Files.notExists(registryFolder(root).resolve("OrderPlaced.1.default.snap.received.json")));
    }

    @Test
    void store_aDriftedPayload_writesAReceivedFileAndThrows(@TempDir Path root) throws Exception {
        var storage = nonInteractiveStorage(SnapshotLayout.registry().rootPath(root.toString()));
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");
        storage.store(location, new SnapshotData(PAYLOAD));

        var drifted = "{\"id\":2}".getBytes(UTF_8);
        var error = assertThrows(AssertionError.class, () -> storage.store(location, new SnapshotData(drifted)));

        assertTrue(requireNonNull(error.getMessage()).contains("drift"));
        var received = registryFolder(root).resolve("OrderPlaced.1.default.snap.received.json");
        assertArrayEquals(drifted, Files.readAllBytes(received));
    }

    @Test
    void store_whenTheDirectoryCannotBeCreated_wrapsTheIoFailure(@TempDir Path root) throws Exception {
        var blocker = root.resolve("contract-registry");
        Files.writeString(blocker, "not a directory");
        var storage = storageRootedAt(root);
        var location = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.UNSET, ".json");

        assertThrows(UncheckedIOException.class, () -> storage.store(location, new SnapshotData(PAYLOAD)));
    }

    @Test
    void store_aBareRelativeLocation_writesAndDriftsWithoutAParentFolder() throws Exception {
        var storage =
                nonInteractiveStorage(SnapshotLayout.registry().rootPath("").wrapperFolder(""));
        var bareName = "FileSnapshotStorageLayoutTests-bare-" + System.nanoTime();
        var location = SnapshotLocation.ofRelativePath(Path.of(bareName + ".snap.approved.json"));
        var approved = Path.of(bareName + ".snap.approved.json");
        var received = Path.of(bareName + ".snap.received.json");
        try {
            storage.store(location, new SnapshotData(PAYLOAD));
            assertTrue(Files.exists(approved));

            var drifted = "{\"id\":2}".getBytes(UTF_8);
            assertThrows(AssertionError.class, () -> storage.store(location, new SnapshotData(drifted)));
            assertArrayEquals(drifted, Files.readAllBytes(received));
        } finally {
            Files.deleteIfExists(approved);
            Files.deleteIfExists(received);
        }
    }

    @Test
    void readAll_whenNothingMatchesThePrefix_returnsEmpty(@TempDir Path root) {
        var storage = storageRootedAt(root);

        var payloads = storage.readAll(SnapshotFilter.of(MESSAGE_TYPE, "1"));

        assertTrue(payloads.isEmpty());
    }

    @Test
    void readAll_whenTheFolderCannotBeListed_wrapsTheIoFailure(@TempDir Path root) throws Exception {
        var folder = registryFolder(root);
        Files.createDirectories(folder);
        try {
            Files.setPosixFilePermissions(folder, java.nio.file.attribute.PosixFilePermissions.fromString("-wx------"));
        } catch (UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("POSIX permissions are not supported here");
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(Files.isReadable(folder), "directory is still readable");
        var storage = storageRootedAt(root);
        try {
            assertThrows(UncheckedIOException.class, () -> storage.readAll(SnapshotFilter.of(MESSAGE_TYPE, "1")));
        } finally {
            Files.setPosixFilePermissions(folder, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    void readAll_returnsEveryMatchingApprovedFile_sortedByName(@TempDir Path root) throws Exception {
        var folder = registryFolder(root);
        Files.createDirectories(folder);
        var first = "{\"id\":1}".getBytes(UTF_8);
        var second = "{\"id\":2}".getBytes(UTF_8);
        var other = "{\"id\":9}".getBytes(UTF_8);
        Files.write(folder.resolve("OrderPlaced.1.B.snap.approved.json"), second);
        Files.write(folder.resolve("OrderPlaced.1.A.snap.approved.json"), first);
        Files.write(folder.resolve("OrderShipped.1.A.snap.approved.json"), other);
        Files.write(folder.resolve("OrderPlaced.1.A.snap.received.json"), other);
        var storage = storageRootedAt(root);

        var payloads = storage.readAll(SnapshotFilter.of(MESSAGE_TYPE, "1"));

        assertEquals(2, payloads.size());
        assertArrayEquals(first, payloads.get(0).bytes());
        assertArrayEquals(second, payloads.get(1).bytes());
    }

    @Test
    void read_aPinnedVariant_readsExactlyThatFile(@TempDir Path root) throws Exception {
        var folder = registryFolder(root);
        Files.createDirectories(folder);
        var withPromotion = "{\"id\":1}".getBytes(UTF_8);
        var noPromotion = "{\"id\":2}".getBytes(UTF_8);
        Files.write(folder.resolve("OrderPlaced.1.WithPromotion.snap.approved.json"), withPromotion);
        Files.write(folder.resolve("OrderPlaced.1.NoPromotion.snap.approved.json"), noPromotion);
        var storage = storageRootedAt(root);

        var pinned = SnapshotLocation.of(MESSAGE_TYPE, "1", SnapshotVariant.named("WithPromotion"), ".json");

        assertArrayEquals(withPromotion, storage.read(pinned).bytes());
    }
}
