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
    private static final String RECEIVED_SUFFIX = ".received.txt";

    private final Path dir;

    Base64SnapshotStorage(Path dir) {
        this.dir = dir;
    }

    @Override
    public SnapshotResult store(SnapshotLocation location, SnapshotData data) {
        var payload = data.bytes();
        var path = approvedPath(location.name());
        try {
            if (!Files.exists(path)) {
                var parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(path, encode(payload), UTF_8);
                return new SnapshotResult.Approved(location);
            }
            var approved = Base64.getDecoder().decode(Files.readString(path, UTF_8));
            if (Arrays.equals(approved, payload)) {
                return new SnapshotResult.Unchanged(location);
            }
            var received = receivedPath(location.name());
            Files.writeString(received, encode(payload), UTF_8);
            return new SnapshotResult.Drifted(location, new SnapshotData(approved), data, path, received);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void approve(SnapshotLocation location, SnapshotData data) {
        var path = approvedPath(location.name());
        try {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, encode(data.bytes()), UTF_8);
            Files.deleteIfExists(receivedPath(location.name()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String encode(byte[] payload) {
        return Base64.getEncoder().encodeToString(payload);
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

    private Path receivedPath(String name) {
        return dir.resolve(name + RECEIVED_SUFFIX);
    }
}
