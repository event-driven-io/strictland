package io.eventdriven.strictland;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * File-backed snapshot storage. It is a thin sink: the DSL resolves a snapshot's committed path and
 * hands it in as the name, so this type only writes bytes there and reads them back. The layout, the
 * calling test, and any variant label are resolved before it is called.
 *
 * <p>The first run writes the approved file for you to review and commit. A later run compares the
 * payload against it: a match passes, a difference writes a sibling {@code .received} file for diffing
 * and fails the check.
 */
class FileSnapshotStorage implements SnapshotStorage {

    private static final String APPROVED_MARKER = ".approved.";
    private static final String RECEIVED_MARKER = ".received.";

    @Override
    public void store(String name, byte[] payload) {
        var approved = Path.of(name);
        if (approved.getParent() == null) {
            throw new IllegalArgumentException("Snapshot path must have a parent directory: " + name);
        }
        if (!name.contains(APPROVED_MARKER)) {
            throw new IllegalArgumentException("Snapshot path must end in .approved.<ext>: " + name);
        }
        var received = Path.of(name.replace(APPROVED_MARKER, RECEIVED_MARKER));
        try {
            if (!Files.exists(approved)) {
                Files.createDirectories(approved.getParent());
                Files.write(approved, payload);
                Files.deleteIfExists(received);
                return;
            }
            if (Arrays.equals(Files.readAllBytes(approved), payload)) {
                Files.deleteIfExists(received);
                return;
            }
            Files.createDirectories(received.getParent());
            Files.write(received, payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store snapshot at " + name, e);
        }
        throw new AssertionError(
                "Snapshot drift: " + name + " differs from the approved snapshot. See " + received + " to review.");
    }

    @Override
    public Optional<byte[]> read(String name) {
        try {
            return Optional.of(Files.readAllBytes(Path.of(name)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
