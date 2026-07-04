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
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * File-backed snapshot storage, rooted at the {@code rootPath/wrapperFolder} a {@link SnapshotLayout}
 * lays out (defaulting to {@code test/resources/contract-registry}).
 * It stores approved snapshots in relative {@link SnapshotLocation} path under the root.
 *
 * <p>The first run writes the approved file for you to review and commit (with {@code .snap.approved} marker). A later run compares the
 * payload against it: a match passes, a difference is reviewed according to the {@link SnapshotReview}
 * this store was built with. In {@link ReviewMode#APPROVE} a difference re-baselines the approved file;
 * otherwise it writes a sibling {@code .snap.received} file, opens a diff tool when the run is local and
 * interactive, and fails the check with a message carrying an inline diff of what moved.</p>
 */
class FileSnapshotStorage implements SnapshotStorage {

    private static final String APPROVED_MARKER = ".approved.";
    private static final String RECEIVED_MARKER = ".received.";
    static final String SNAP_APPROVED_MARKER = ".snap.approved.";

    private static final System.Logger LOGGER = System.getLogger(FileSnapshotStorage.class.getName());

    private final Path root;
    private final SnapshotReview review;
    private final DiffLauncher launcher;
    private final BooleanSupplier nonInteractive;

    FileSnapshotStorage(SnapshotLayout layout) {
        this(layout, SnapshotReview.auto());
    }

    FileSnapshotStorage(SnapshotLayout layout, SnapshotReview review) {
        this(layout, review, new ProcessDiffLauncher(), CiDetector::isNonInteractive);
    }

    FileSnapshotStorage(
            SnapshotLayout layout, SnapshotReview review, DiffLauncher launcher, BooleanSupplier nonInteractive) {
        this.root = Path.of(layout.rootPath(), layout.wrapperFolder());
        this.review = review;
        this.launcher = launcher;
        this.nonInteractive = nonInteractive;
    }

    @Override
    public void store(SnapshotLocation location, SnapshotData data) {
        var approved = approvedPath(location);
        var fileName = approved.getFileName().toString();
        var received = approved.resolveSibling(fileName.replace(APPROVED_MARKER, RECEIVED_MARKER));
        var payload = data.bytes();
        byte[] approvedBytes;
        try {
            var parent = approved.getParent();
            if (!Files.exists(approved)) {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(approved, payload);
                Files.deleteIfExists(received);
                return;
            }
            approvedBytes = Files.readAllBytes(approved);
            if (Arrays.equals(approvedBytes, payload)) {
                Files.deleteIfExists(received);
                return;
            }
            if (review.mode() == ReviewMode.APPROVE) {
                Files.write(approved, payload);
                Files.deleteIfExists(received);
                LOGGER.log(System.Logger.Level.INFO, () -> "Approved snapshot: " + approved);
                return;
            }
            var receivedParent = received.getParent();
            if (receivedParent != null) {
                Files.createDirectories(receivedParent);
            }
            Files.write(received, payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store snapshot at " + approved, e);
        }
        maybeLaunch(received, approved);
        throw new AssertionError(driftMessage(approved, received, approvedBytes, payload));
    }

    private void maybeLaunch(Path received, Path approved) {
        if (review.mode() == ReviewMode.OFF || nonInteractive.getAsBoolean()) {
            return;
        }
        effectiveTool().ifPresent(tool -> launcher.launch(tool, received, approved));
    }

    private Optional<DiffTool> effectiveTool() {
        var explicit = review.tool();
        return explicit != null ? Optional.of(explicit) : DiffTools.detect();
    }

    private static String driftMessage(Path approved, Path received, byte[] approvedBytes, byte[] payload) {
        return "Snapshot drift: " + approved + " differs from the approved snapshot.\n\n"
                + SnapshotDiff.render(approvedBytes, payload) + "\n"
                + "received: " + received + "\n"
                + "approved: " + approved + "\n\n"
                + "To accept this change, re-run with -Dstrictland.review.mode=approve, "
                + "or save the received payload over the approved file in the diff tool.";
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
