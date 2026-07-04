package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * Re-baselines drifted snapshots in bulk by promoting every received payload over its approved
 * sibling, without re-running the tests. It is the filesystem twin of {@code
 * -Dstrictland.review.mode=approve}: run it after a review to accept a batch of intentional changes at
 * once.
 *
 * <p>Wire it into your build as a task that runs {@link #main(String[])} - a Gradle {@code JavaExec}
 * or Maven {@code exec:java} on the test classpath. The registry root defaults to {@code
 * src/test/resources/contract-registry} and can be overridden by the first argument or the {@code
 * strictland.review.root} system property.</p>
 *
 * <pre>
 * tasks.register&lt;JavaExec&gt;("approveSnapshots") {
 *     mainClass = "io.eventdriven.strictland.SnapshotApprove"
 *     classpath = sourceSets.test.get().runtimeClasspath
 * }
 * </pre>
 */
public final class SnapshotApprove {

    private static final String ROOT_PROPERTY = "strictland.review.root";
    private static final String DEFAULT_ROOT = "src/test/resources/contract-registry";

    private SnapshotApprove() {}

    /**
     * Sweeps the registry from the command line, printing how many snapshots it promoted. The root is
     * the first argument, else the {@code strictland.review.root} system property, else {@code
     * src/test/resources/contract-registry}.
     *
     * @param args an optional single argument: the registry root to sweep
     */
    public static void main(String[] args) {
        var root = resolveRoot(args);
        var promoted = SnapshotApproval.approve(root);
        System.out.println("Approved " + promoted + " snapshot(s) under " + root);
    }

    static Path resolveRoot(String[] args) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }
        return Path.of(System.getProperty(ROOT_PROPERTY, DEFAULT_ROOT));
    }
}
