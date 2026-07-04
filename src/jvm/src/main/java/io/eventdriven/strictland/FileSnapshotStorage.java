package io.eventdriven.strictland;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * File-backed snapshot storage, rooted at the {@code rootPath/wrapperFolder} a {@link SnapshotLayout}
 * lays out (defaulting to {@code test/resources/contract-registry}).
 * It stores approved snapshots in relative {@link SnapshotLocation} path under the root.
 *
 * <p>The first run writes the approved file for you to review and commit (with {@code .snap.approved}
 * marker) and reports {@link SnapshotResult.Approved}. A later run compares the payload against it: a
 * match reports {@link SnapshotResult.Unchanged}, a difference writes a sibling {@code .snap.received}
 * file and reports {@link SnapshotResult.Drifted}. Storage only reads and writes; what a drift means is
 * decided by the caller.</p>
 */
class FileSnapshotStorage implements SnapshotStorage {

    private static final String APPROVED_MARKER = ".approved.";
    private static final String RECEIVED_MARKER = ".received.";

    private final Path root;

    FileSnapshotStorage(SnapshotLayout layout) {
        this.root = Path.of(layout.rootPath(), layout.wrapperFolder());
    }

    @Override
    public SnapshotResult store(SnapshotLocation location, SnapshotData data) {
        var approved = approvedPath(location);
        var received = receivedSibling(approved);
        var payload = data.bytes();
        try {
            var parent = approved.getParent();
            if (!Files.exists(approved)) {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(approved, payload);
                Files.deleteIfExists(received);
                return new SnapshotResult.Approved(location);
            }
            var approvedBytes = Files.readAllBytes(approved);
            if (Arrays.equals(approvedBytes, payload)) {
                Files.deleteIfExists(received);
                return new SnapshotResult.Unchanged(location);
            }
            var receivedParent = received.getParent();
            if (receivedParent != null) {
                Files.createDirectories(receivedParent);
            }
            Files.write(received, payload);
            return new SnapshotResult.Drifted(location, new SnapshotData(approvedBytes), data, approved, received);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store snapshot at " + approved, e);
        }
    }

    @Override
    public void approve(SnapshotLocation location, SnapshotData data) {
        var approved = approvedPath(location);
        try {
            SnapshotApproval.writeApproved(approved, receivedSibling(approved), data.bytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to approve snapshot at " + approved, e);
        }
    }

    private static Path receivedSibling(Path approved) {
        return approved.resolveSibling(approved.getFileName().toString().replace(APPROVED_MARKER, RECEIVED_MARKER));
    }

    @Override
    public SnapshotData read(SnapshotLocation location) {
        var approved = approvedPath(location);
        try {
            return new SnapshotData(Files.readAllBytes(approved));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read snapshot file: " + approved, e);
        }
    }

    @Override
    public List<SnapshotData> readAll(SnapshotFilter filter) {
        var folder = join(root, filter.path());
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            var matches = files.filter(path -> {
                        var name = path.getFileName().toString();
                        return name.startsWith(filter.namePrefix()) && name.contains(APPROVED_MARKER);
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            var payloads = new ArrayList<SnapshotData>(matches.size());
            for (var match : matches) {
                payloads.add(new SnapshotData(Files.readAllBytes(match)));
            }
            return List.copyOf(payloads);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list snapshots in " + folder, e);
        }
    }

    private Path approvedPath(SnapshotLocation location) {
        return join(root, location.path()).resolve(location.name() + ".snap.approved" + location.extension());
    }

    private static Path join(Path base, List<String> path) {
        var resolved = base;
        for (var segment : path) {
            resolved = resolved.resolve(segment);
        }
        return resolved;
    }
}
