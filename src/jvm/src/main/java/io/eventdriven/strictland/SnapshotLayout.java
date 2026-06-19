package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Decides where an approved snapshot file lives, and what it's called, from the test that asks for it
 * and the message it locks down. It is a pure value: hand it the test source directory, the caller,
 * and the message, get back a {@link Path}, with no I/O of its own.
 *
 * <p>A layout decides two orthogonal axes. The {@link SnapshotRoot} location says where the tree is
 * rooted: {@link #nextToTest()} keeps snapshots beside your test sources, {@link #globalRoot(String)}
 * gathers them under one directory. The {@link SnapshotGrouping} chooses the folder under that root,
 * and {@link #wrapperFolder(String)} renames the segment that holds the groups. For a flat shape with
 * snapshots straight in the test's source directory, use {@code
 * nextToTest().grouping(SnapshotGrouping.NONE).wrapperFolder("")}. Each refinement returns a copy, so a
 * layout stays immutable.</p>
 *
 * <pre>
 * SnapshotLayout layout = SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT);
 * Path path = layout.resolve(
 *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "OrderPlaced", ".json");
 * // src/test/java/com/acme/orders/snapshots/OrderPlaced/OrderPlaced.snap.approved.json
 * </pre>
 *
 * @param location where the snapshot tree is rooted
 * @param grouping the folder a snapshot is grouped into under that root
 * @param wrapperFolder the segment that holds the group folders, dropped when empty
 * @param rootPath the directory {@link SnapshotRoot#GLOBAL_ROOT} roots the tree at, empty otherwise
 * @param testClassNaming how the test class is named in an owned snapshot's file name; the folder
 *     grouping always uses the simple name regardless
 */
public record SnapshotLayout(
        SnapshotRoot location,
        SnapshotGrouping grouping,
        String wrapperFolder,
        String rootPath,
        TestClassNaming testClassNaming) {

    private static final String DEFAULT_WRAPPER_FOLDER = "snapshots";
    private static final String SNAP_APPROVED_SUFFIX = ".snap.approved";

    /**
     * A layout that keeps snapshots beside your test sources, grouped per test class under a {@code
     * snapshots} folder. The approved file sits next to the test that owns it, so a contract change
     * shows up in the same place as the code that caused it.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayout.nextToTest();
     * Path path = layout.resolve(
     *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "OrderPlaced", ".json");
     * // src/test/java/com/acme/orders/snapshots/OrderTests/OrderPlaced.snap.approved.json
     * </pre>
     *
     * @return a next-to-test layout, grouped per test class
     */
    public static SnapshotLayout nextToTest() {
        return new SnapshotLayout(
                SnapshotRoot.NEXT_TO_TEST,
                SnapshotGrouping.PER_TEST_CLASS,
                DEFAULT_WRAPPER_FOLDER,
                "",
                TestClassNaming.SIMPLE);
    }

    /**
     * A layout that gathers every snapshot under one directory, with the test's package path beneath
     * it. Use it to keep approved files out of your source folders, for instance under {@code
     * src/test/resources}.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayout.globalRoot("src/test/resources/snapshots");
     * Path path = layout.resolve(
     *     null, "com.acme.orders", "OrderTests", "OrderPlaced", "OrderPlaced", ".json");
     * // src/test/resources/snapshots/com/acme/orders/OrderTests/OrderPlaced.snap.approved.json
     * </pre>
     *
     * @param rootPath the directory the snapshot tree is rooted at
     * @return a global-root layout rooted at {@code rootPath}, grouped per test class
     */
    public static SnapshotLayout globalRoot(String rootPath) {
        return new SnapshotLayout(
                SnapshotRoot.GLOBAL_ROOT,
                SnapshotGrouping.PER_TEST_CLASS,
                DEFAULT_WRAPPER_FOLDER,
                rootPath,
                TestClassNaming.SIMPLE);
    }

    /**
     * Returns a copy with a different grouping, for choosing whether snapshots sit together by test
     * class, by message contract, or under no group folder at all.
     *
     * <pre>
     * SnapshotLayout byContract = SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT);
     * </pre>
     *
     * @param grouping the folder a snapshot is grouped into
     * @return a copy of this layout using the given grouping
     */
    public SnapshotLayout grouping(SnapshotGrouping grouping) {
        return new SnapshotLayout(location, grouping, wrapperFolder, rootPath, testClassNaming);
    }

    /**
     * Returns a copy with a different wrapper folder, the segment that holds the group folders. The
     * default is {@code snapshots}; set it to match your convention, or set it empty to drop the
     * segment entirely.
     *
     * <pre>
     * SnapshotLayout underApproved = SnapshotLayout.nextToTest().wrapperFolder("approved");
     * </pre>
     *
     * @param wrapperFolder the segment that holds the group folders
     * @return a copy of this layout using the given wrapper folder
     */
    public SnapshotLayout wrapperFolder(String wrapperFolder) {
        return new SnapshotLayout(location, grouping, wrapperFolder, rootPath, testClassNaming);
    }

    /**
     * Returns a copy that names the test class differently inside an owned snapshot's file name, for
     * keeping snapshots distinct when two test classes share a simple name. It changes only the file
     * name, never the folder a snapshot is grouped under.
     *
     * <pre>
     * SnapshotLayout qualified = SnapshotLayout.nextToTest().testClassNaming(TestClassNaming.FULL);
     * </pre>
     *
     * @param testClassNaming how the test class is named in an owned snapshot's file name
     * @return a copy of this layout using the given test-class naming
     */
    public SnapshotLayout testClassNaming(TestClassNaming testClassNaming) {
        return new SnapshotLayout(location, grouping, wrapperFolder, rootPath, testClassNaming);
    }

    /**
     * Resolves the committed approved-file path for a check, from the test that asks and the snapshot's
     * identity. The result ends in {@code .snap.approved<fileExtension>}.
     *
     * <p>The {@code snapshotName} names the file; {@code messageType} is what {@link
     * SnapshotGrouping#PER_CONTRACT} groups it under, while {@link SnapshotGrouping#PER_TEST_CLASS} groups under the
     * caller's simple name and {@link SnapshotGrouping#NONE} adds no group folder. For an ordinary
     * snapshot the file and message names are the same; they differ only when one message type has
     * several snapshots that each need their own name. The {@code testSourceDir} anchors {@link
     * SnapshotRoot#NEXT_TO_TEST} and already includes the caller's package path; {@link
     * SnapshotRoot#GLOBAL_ROOT} ignores it and composes its own root from {@code rootPath} and the package.</p>
     *
     * <pre>
     * Path path = SnapshotLayout.nextToTest().grouping(SnapshotGrouping.PER_CONTRACT).resolve(
     *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "withCoupon", ".json");
     * // src/test/java/com/acme/orders/snapshots/OrderPlaced/withCoupon.snap.approved.json
     * </pre>
     *
     * @param testSourceDir the test's source directory, package path included, anchoring {@link
     *     SnapshotRoot#NEXT_TO_TEST}; may be {@code null} for {@link SnapshotRoot#GLOBAL_ROOT}, which does
     *     not use it
     * @param callerPackage the package of the test asking for the snapshot
     * @param callerSimpleName the simple name of the test class asking for the snapshot
     * @param messageType the message type the snapshot belongs to, grouping it under {@link
     *     SnapshotGrouping#PER_CONTRACT}
     * @param snapshotName the base name of the snapshot's file
     * @param fileExtension the extension the serializer writes, including the leading dot
     * @return the committed approved-file path
     */
    public Path resolve(
            @Nullable Path testSourceDir,
            String callerPackage,
            String callerSimpleName,
            String messageType,
            String snapshotName,
            String fileExtension) {
        var fileName = snapshotName + SNAP_APPROVED_SUFFIX + fileExtension;
        var root = location == SnapshotRoot.GLOBAL_ROOT
                ? Path.of(rootPath, callerPackage.replace('.', '/'))
                : rootNextToTest(testSourceDir);
        var grouped =
                switch (grouping) {
                    case NONE -> root;
                    case PER_CONTRACT -> root.resolve(messageType);
                    case PER_TEST_CLASS -> root.resolve(callerSimpleName);
                };
        return grouped.resolve(fileName);
    }

    private Path rootNextToTest(@Nullable Path testSourceDir) {
        var testSource = requireTestSourceDir(testSourceDir);
        return wrapperFolder.isEmpty() ? testSource : testSource.resolve(wrapperFolder);
    }

    private Path requireTestSourceDir(@Nullable Path testSourceDir) {
        if (testSourceDir == null) {
            throw new IllegalArgumentException(location + " needs a test source directory to anchor the snapshot tree");
        }
        return testSourceDir;
    }
}
