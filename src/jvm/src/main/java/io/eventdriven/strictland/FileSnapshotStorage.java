package io.eventdriven.strictland;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * File-backed snapshot storage. It is a thin wrapper that reads and stores snapshots.
 *
 * <p>The first run writes the approved file for you to review and commit. A later run compares the
 * payload against it: a match passes, a difference writes a sibling {@code .received} file for diffing
 * and fails the check.</p>
 *
 * <p> `FileSnapshotStorage` is a thin wrapper that just writes bytes and reads them back.
 * The path, layout, and variant are resolved by the caller before the bytes reach storage.</p>
 */
class FileSnapshotStorage implements SnapshotStorage {

    private static final String APPROVED_MARKER = ".approved.";
    private static final String RECEIVED_MARKER = ".received.";
    static final String SNAP_APPROVED_MARKER = ".snap.approved.";

    @Override
    public void store(String pathWithName, byte[] payload) {
        var approved = Path.of(pathWithName);
        if (approved.getParent() == null) {
            throw new IllegalArgumentException("Snapshot path must have a parent directory: " + pathWithName);
        }
        if (!pathWithName.contains(APPROVED_MARKER)) {
            throw new IllegalArgumentException("Snapshot path must end in .approved.<ext>: " + pathWithName);
        }
        var received = Path.of(pathWithName.replace(APPROVED_MARKER, RECEIVED_MARKER));
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
            throw new UncheckedIOException("Failed to store snapshot at " + pathWithName, e);
        }
        throw new AssertionError("Snapshot drift: " + pathWithName + " differs from the approved snapshot. See "
                + received + " to review.");
    }

    @Override
    public Optional<byte[]> read(String pathWithName) {
        try {
            return Optional.of(Files.readAllBytes(Path.of(pathWithName)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<byte[]> readAll(@Nullable Path folder, String prefix, @Nullable String variant) {
        if (folder == null || !Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            var matches = files.filter(path -> {
                        var name = path.getFileName().toString();
                        return name.startsWith(prefix)
                                && name.contains(APPROVED_MARKER)
                                && matchesVariant(name, variant);
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            var payloads = new ArrayList<byte[]>(matches.size());
            for (var match : matches) {
                payloads.add(Files.readAllBytes(match));
            }
            return List.copyOf(payloads);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list snapshots in " + folder, e);
        }
    }

    private static boolean matchesVariant(String fileName, @Nullable String variant) {
        if (variant == null) {
            return true;
        }
        var markerAt = fileName.indexOf(SNAP_APPROVED_MARKER);
        var base = markerAt < 0 ? fileName : fileName.substring(0, markerAt);
        return base.endsWith("." + variant);
    }
}
