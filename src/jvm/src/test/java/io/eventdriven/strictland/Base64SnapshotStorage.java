package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class Base64SnapshotStorage implements SnapshotStorage {
    private final Path dir;

    Base64SnapshotStorage(Path dir) {
        this.dir = dir;
    }

    @Override
    public void store(String name, byte[] payload) {
        var path = approvedPath(name);
        try {
            if (!Files.exists(path)) {
                var parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(path, Base64.getEncoder().encodeToString(payload), UTF_8);
                return;
            }
            var approved = Base64.getDecoder().decode(Files.readString(path, UTF_8));
            if (!Arrays.equals(approved, payload)) {
                throw new AssertionError(
                        "Snapshot drift for '" + name + "': stored payload differs from the approved snapshot");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<byte[]> read(String name) {
        var path = approvedPath(name);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Base64.getDecoder().decode(Files.readString(path, UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path approvedPath(String name) {
        return dir.resolve(name + ".approved.txt");
    }
}
