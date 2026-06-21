package io.eventdriven.strictland;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    @Override
    public List<byte[]> readAll(@Nullable Path folder, String prefix, @Nullable String variant) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            var matches = files.filter(path -> {
                        var name = path.getFileName().toString();
                        return name.startsWith(prefix)
                                && name.endsWith(".approved.txt")
                                && matchesVariant(name, variant);
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            var payloads = new ArrayList<byte[]>(matches.size());
            for (var match : matches) {
                payloads.add(Base64.getDecoder().decode(Files.readString(match, UTF_8)));
            }
            return List.copyOf(payloads);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean matchesVariant(String fileName, @Nullable String variant) {
        if (variant == null) {
            return true;
        }
        var base = fileName.substring(0, fileName.length() - ".approved.txt".length());
        return base.endsWith("." + variant);
    }

    private Path approvedPath(String name) {
        return dir.resolve(name + ".approved.txt");
    }
}
