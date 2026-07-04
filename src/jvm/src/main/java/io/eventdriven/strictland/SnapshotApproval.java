package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

final class SnapshotApproval {

    static final String RECEIVED_MARKER = ".snap.received.";
    static final String APPROVED_MARKER = ".snap.approved.";

    private static final System.Logger LOGGER = System.getLogger(SnapshotApproval.class.getName());

    private SnapshotApproval() {}

    static void writeApproved(Path approved, Path received, byte[] payload) throws IOException {
        Files.createDirectories(requireNonNull(approved.getParent()));
        Files.write(approved, payload);
        Files.deleteIfExists(received);
        LOGGER.log(System.Logger.Level.INFO, () -> "Approved snapshot: " + approved);
    }

    static int approve(Path registryRoot) {
        if (!Files.isDirectory(registryRoot)) {
            return 0;
        }
        try (Stream<Path> tree = Files.walk(registryRoot)) {
            var received = tree.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(RECEIVED_MARKER))
                    .toList();
            for (var receivedFile : received) {
                var approvedName = receivedFile.getFileName().toString().replace(RECEIVED_MARKER, APPROVED_MARKER);
                var approved = receivedFile.resolveSibling(approvedName);
                Files.move(receivedFile, approved, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.log(System.Logger.Level.INFO, () -> "Approved snapshot: " + approved);
            }
            return received.size();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to promote snapshots under " + registryRoot, e);
        }
    }
}
