package io.eventdriven.strictland;

/**
 * Decides how {@link SnapshotLayout} names the test class inside an owned snapshot's file name. Pick
 * the one based on how you tell two tests apart: by their simple class name, or by the
 * fully-qualified one when simple names collide across packages.
 *
 * <p>It changes only the discriminator written into the file name, never the folder a snapshot is
 * grouped under: the folder always uses the simple class name.</p>
 *
 * <pre>
 * SnapshotLayout qualified = SnapshotLayout
 *     .nextToTest()
 *     .testClassNaming(TestClassNaming.FULL);
 * </pre>
 */
public enum TestClassNaming {

    /**
     * Names the test class by its simple name, the short form that reads cleanly when no two test
     * classes share it.
     */
    SIMPLE,

    /**
     * Names the test class by its fully-qualified name, package included, so two tests with the same
     * simple name in different packages keep distinct snapshot file names.
     */
    FULL
}
