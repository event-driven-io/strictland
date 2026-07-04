package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
final class SnapshotApproveTests {

    private static final byte[] APPROVED = "{\"v\":1}".getBytes(UTF_8);
    private static final byte[] RECEIVED = "{\"v\":2}".getBytes(UTF_8);

    @Test
    void approve_promotesReceivedOverApprovedAndDeletesReceived(@TempDir Path root) throws IOException {
        var dir = Files.createDirectories(root.resolve("io/eventdriven/OrderPlaced"));
        var approved = dir.resolve("OrderPlaced.1.default.snap.approved.json");
        var received = dir.resolve("OrderPlaced.1.default.snap.received.json");
        Files.write(approved, APPROVED);
        Files.write(received, RECEIVED);

        var promoted = SnapshotApprove.approve(root);

        assertEquals(1, promoted);
        assertArrayEquals(RECEIVED, Files.readAllBytes(approved));
        assertFalse(Files.exists(received), "received should be gone after promotion");
    }

    @Test
    void approve_returnsZeroWhenThereIsNothingToPromote(@TempDir Path root) {
        assertEquals(0, SnapshotApprove.approve(root));
    }

    @Test
    void approve_returnsZeroForARootThatDoesNotExist(@TempDir Path root) {
        assertEquals(0, SnapshotApprove.approve(root.resolve("missing")));
    }

    @Test
    void approve_wrapsAnIoFailureWhilePromoting(@TempDir Path root) throws IOException {
        var dir = Files.createDirectories(root.resolve("Order"));
        Files.write(dir.resolve("Order.1.default.snap.approved.json"), APPROVED);
        Files.write(dir.resolve("Order.1.default.snap.received.json"), RECEIVED);
        try {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("r-xr-xr-x"));
        } catch (UnsupportedOperationException e) {
            Assumptions.abort("POSIX permissions are not supported here");
        }
        Assumptions.assumeFalse(Files.isWritable(dir), "directory is still writable (running as root?)");
        try {
            assertThrows(UncheckedIOException.class, () -> SnapshotApprove.approve(root));
        } finally {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    void resolveRoot_prefersTheArgumentThenThePropertyThenTheDefault() {
        assertEquals(Path.of("from-arg"), SnapshotApprove.resolveRoot(new String[] {"from-arg"}));
        assertEquals(Path.of("src/test/resources/contract-registry"), SnapshotApprove.resolveRoot(new String[] {}));
    }

    @Test
    void main_sweepsTheGivenRoot(@TempDir Path root) throws IOException {
        var dir = Files.createDirectories(root.resolve("Order"));
        Files.write(dir.resolve("Order.1.default.snap.approved.json"), APPROVED);
        Files.write(dir.resolve("Order.1.default.snap.received.json"), RECEIVED);

        SnapshotApprove.main(new String[] {root.toString()});

        assertArrayEquals(RECEIVED, Files.readAllBytes(dir.resolve("Order.1.default.snap.approved.json")));
        assertTrue(Files.exists(dir.resolve("Order.1.default.snap.approved.json")));
    }
}
