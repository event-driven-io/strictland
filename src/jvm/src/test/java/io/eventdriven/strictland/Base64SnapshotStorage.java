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
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class Base64SnapshotStorage implements SnapshotStorage {
    private static final String APPROVED_SUFFIX = ".approved.txt";

    private final Path dir;

    Base64SnapshotStorage(Path dir) {
        this.dir = dir;
    }

    @Override
    public void store(SnapshotLocation location, SnapshotData data) {
        var payload = data.bytes();
        var path = approvedPath(location.name());
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
                throw new AssertionError("Snapshot drift for '" + location.name()
                        + "': stored payload differs from the approved snapshot");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public SnapshotData read(SnapshotLocation location) {
        var path = approvedPath(location.name());
        try {
            return new SnapshotData(Base64.getDecoder().decode(Files.readString(path, UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read snapshot file: " + path, e);
        }
    }

    @Override
    public List<SnapshotData> readAll(SnapshotFilter filter) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            var matches = files.filter(path -> {
                        var name = path.getFileName().toString();
                        return name.startsWith(filter.namePrefix()) && name.endsWith(APPROVED_SUFFIX);
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            var payloads = new ArrayList<SnapshotData>(matches.size());
            for (var match : matches) {
                payloads.add(new SnapshotData(Base64.getDecoder().decode(Files.readString(match, UTF_8))));
            }
            return List.copyOf(payloads);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path approvedPath(String name) {
        return dir.resolve(name + APPROVED_SUFFIX);
    }
}
