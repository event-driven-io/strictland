package io.eventdriven.strictland;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Re-baselines drifted snapshots in bulk by promoting every received payload over its approved
 * sibling, without re-running the tests. It is the filesystem twin of {@link ReviewMode#APPROVE}: run
 * it after a review to accept a batch of intentional changes at once.
 *
 * <p>Wire it into your build as a task that runs {@link #main(String[])} - a Gradle {@code JavaExec}
 * or Maven {@code exec:java} on the test classpath - so {@code ./gradlew approveSnapshots} sweeps the
 * registry. The registry root defaults to {@code src/test/resources/contract-registry} and can be
 * overridden by the first argument or the {@code strictland.review.root} system property.</p>
 *
 * <pre>
 * // src/jvm/build.gradle.kts
 * tasks.register&lt;JavaExec&gt;("approveSnapshots") {
 *     mainClass = "io.eventdriven.strictland.SnapshotApprove"
 *     classpath = sourceSets.test.get().runtimeClasspath
 * }
 * </pre>
 */
public final class SnapshotApprove {

    private static final String RECEIVED_MARKER = ".snap.received.";
    private static final String APPROVED_MARKER = ".snap.approved.";
    private static final String ROOT_PROPERTY = "strictland.review.root";
    private static final String DEFAULT_ROOT = "src/test/resources/contract-registry";

    private SnapshotApprove() {}

    /**
     * Promotes every {@code *.snap.received.*} file under {@code registryRoot} over its {@code
     * *.snap.approved.*} sibling and deletes the received file, returning how many it promoted. A root
     * that doesn't exist promotes nothing and returns {@code 0}.
     *
     * <pre>
     * int promoted = SnapshotApprove.approve(Path.of("src/test/resources/contract-registry"));
     * </pre>
     *
     * @param registryRoot the directory the snapshot registry is rooted at
     * @return the number of snapshots promoted
     */
    public static int approve(Path registryRoot) {
        if (!Files.isDirectory(registryRoot)) {
            return 0;
        }
        var logger = System.getLogger(SnapshotApprove.class.getName());
        try (Stream<Path> tree = Files.walk(registryRoot)) {
            var received = tree.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(RECEIVED_MARKER))
                    .toList();
            for (var receivedFile : received) {
                var approvedName = receivedFile.getFileName().toString().replace(RECEIVED_MARKER, APPROVED_MARKER);
                var approved = receivedFile.resolveSibling(approvedName);
                Files.move(receivedFile, approved, StandardCopyOption.REPLACE_EXISTING);
                logger.log(System.Logger.Level.INFO, () -> "Approved snapshot: " + approved);
            }
            return received.size();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to promote snapshots under " + registryRoot, e);
        }
    }

    /**
     * Sweeps the registry from the command line, printing how many snapshots it promoted. The root is
     * the first argument, else the {@code strictland.review.root} system property, else {@code
     * src/test/resources/contract-registry}.
     *
     * @param args an optional single argument: the registry root to sweep
     */
    public static void main(String[] args) {
        var root = resolveRoot(args);
        var promoted = approve(root);
        System.out.println("Approved " + promoted + " snapshot(s) under " + root);
    }

    static Path resolveRoot(String[] args) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }
        return Path.of(System.getProperty(ROOT_PROPERTY, DEFAULT_ROOT));
    }
}
