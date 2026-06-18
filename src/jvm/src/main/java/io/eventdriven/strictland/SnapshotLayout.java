package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Decides where an approved snapshot file lives, and what it's called, from the test that asks for it
 * and the message it locks down. It is a pure value: hand it the test source directory, the caller,
 * and the message, get back a {@link Path}, with no I/O of its own.
 *
 * <p>Start from a strategy that says where the tree is rooted, then refine it. {@link #nextToTest()}
 * keeps snapshots beside your test sources, {@link #globalRoot(String)} gathers them under one
 * directory, and {@link #flat()} keeps the original flat shape Strictland has always written. The
 * {@link SnapshotGrouping} chooses the folder under that root, and {@link #wrapperFolder(String)} renames the
 * segment that holds the groups. Each refinement returns a copy, so a layout stays immutable.</p>
 *
 * <pre>
 * SnapshotLayout layout = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT);
 * Path path = layout.resolve(
 *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "OrderPlaced", ".json");
 * // src/test/java/com/acme/orders/snapshots/OrderPlaced/OrderPlaced.snap.approved.json
 * </pre>
 *
 * @param strategy where the snapshot tree is rooted
 * @param grouping the folder a snapshot is grouped into under that root
 * @param wrapperFolder the segment that holds the group folders, ignored by {@link Strategy#FLAT}
 * @param rootPath the directory {@link Strategy#GLOBAL_ROOT} roots the tree at, empty otherwise
 */
public record SnapshotLayout(Strategy strategy, SnapshotGrouping grouping, String wrapperFolder, String rootPath) {

    private static final String DEFAULT_WRAPPER_FOLDER = "snapshots";
    private static final String SNAP_APPROVED_SUFFIX = ".snap.approved";
    private static final String FLAT_SUFFIX = ".approved.txt";

    /**
     * Where {@link SnapshotLayout} roots the snapshot tree.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayout.flat();
     * boolean flat = layout.strategy() == SnapshotLayout.Strategy.FLAT;
     * </pre>
     */
    public enum Strategy {
        /** Beside your test sources, under {@code <testSourceDir>/<wrapperFolder>}. */
        NEXT_TO_TEST,

        /** Under one directory you name, with the package path beneath it. */
        GLOBAL_ROOT,

        /**
         * The original flat shape: {@code <testSourceDir>/<snapshotName>.approved.txt}, with no wrapper folder,
         * no grouping, and a {@code .txt} extension regardless of the serializer's.
         */
        FLAT
    }

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
        return new SnapshotLayout(Strategy.NEXT_TO_TEST, SnapshotGrouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, "");
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
                Strategy.GLOBAL_ROOT, SnapshotGrouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, rootPath);
    }

    /**
     * The flat layout Strictland has always written, the original shape kept for projects that want
     * today's behaviour. It puts {@code <snapshotName>.approved.txt} straight in the test's source directory,
     * with no wrapper, no group folder, and a {@code .txt} extension whatever the serializer's is.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayout.flat();
     * Path path = layout.resolve(
     *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "OrderPlaced", ".json");
     * // src/test/java/com/acme/orders/OrderPlaced.approved.txt
     * </pre>
     *
     * @return the flat layout
     */
    public static SnapshotLayout flat() {
        return new SnapshotLayout(Strategy.FLAT, SnapshotGrouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, "");
    }

    /**
     * Returns a copy with a different grouping, for choosing whether snapshots sit together by test
     * class or by message contract. {@link Strategy#FLAT} ignores grouping, since its layout is flat.
     *
     * <pre>
     * SnapshotLayout byContract = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT);
     * </pre>
     *
     * @param grouping the folder a snapshot is grouped into
     * @return a copy of this layout using the given grouping
     */
    public SnapshotLayout grouping(SnapshotGrouping grouping) {
        return new SnapshotLayout(strategy, grouping, wrapperFolder, rootPath);
    }

    /**
     * Returns a copy with a different wrapper folder, the segment that holds the group folders. The
     * default is {@code snapshots}; set it to match your convention. {@link Strategy#FLAT} ignores it,
     * since its layout is flat.
     *
     * <pre>
     * SnapshotLayout underApproved = SnapshotLayout.nextToTest().wrapperFolder("approved");
     * </pre>
     *
     * @param wrapperFolder the segment that holds the group folders
     * @return a copy of this layout using the given wrapper folder
     */
    public SnapshotLayout wrapperFolder(String wrapperFolder) {
        return new SnapshotLayout(strategy, grouping, wrapperFolder, rootPath);
    }

    /**
     * Resolves the committed approved-file path for a check, from the test that asks and the snapshot's
     * identity. The result ends in {@code .snap.approved<fileExtension>}, except under {@link
     * Strategy#FLAT}, which always ends in {@code .approved.txt}.
     *
     * <p>The {@code snapshotName} names the file; {@code messageType} is what {@link
     * SnapshotGrouping#PER_CONTRACT} groups it under, while {@link SnapshotGrouping#PER_TEST_CLASS} groups under the
     * caller's simple name. For an ordinary snapshot the two are the same; they differ only when one
     * message type has several snapshots that each need their own name. The {@code testSourceDir} anchors
     * the test-relative strategies and already includes the caller's package path; {@link
     * Strategy#GLOBAL_ROOT} ignores it and composes its own root from {@code rootPath} and the package.</p>
     *
     * <pre>
     * Path path = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT).resolve(
     *     Path.of("src/test/java/com/acme/orders"), "com.acme.orders", "OrderTests", "OrderPlaced", "withCoupon", ".json");
     * // src/test/java/com/acme/orders/snapshots/OrderPlaced/withCoupon.snap.approved.json
     * </pre>
     *
     * @param testSourceDir the test's source directory, package path included, anchoring {@link
     *     Strategy#NEXT_TO_TEST} and {@link Strategy#FLAT}; may be {@code null} for {@link
     *     Strategy#GLOBAL_ROOT}, which does not use it
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
        if (strategy == Strategy.FLAT) {
            return requireTestSourceDir(testSourceDir).resolve(snapshotName + FLAT_SUFFIX);
        }

        var groupFolder = grouping == SnapshotGrouping.PER_CONTRACT ? messageType : callerSimpleName;
        var fileName = snapshotName + SNAP_APPROVED_SUFFIX + fileExtension;
        var root = strategy == Strategy.GLOBAL_ROOT
                ? Path.of(rootPath, callerPackage.replace('.', '/'))
                : requireTestSourceDir(testSourceDir).resolve(wrapperFolder);
        return root.resolve(groupFolder).resolve(fileName);
    }

    private Path requireTestSourceDir(@Nullable Path testSourceDir) {
        if (testSourceDir == null) {
            throw new IllegalArgumentException(strategy + " needs a test source directory to anchor the snapshot tree");
        }
        return testSourceDir;
    }
}
