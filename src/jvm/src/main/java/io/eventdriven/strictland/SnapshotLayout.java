package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Decides where an approved snapshot file lives, and what it's called, from the test that asks for
 * it and the message it locks down. It's a pure value: hand it the caller and the message, get back
 * a {@link Path}, with no I/O of its own.
 *
 * <p>Start from a strategy that says where the tree is rooted, then refine it. {@link #nextToTest()}
 * keeps snapshots beside your test sources, {@link #globalRoot(String)} gathers them under one
 * directory, and {@link #legacy()} reproduces the flat layout earlier Strictland wrote. The {@link
 * Grouping} chooses the folder under that root, and {@link #wrapperFolder(String)} renames the
 * segment that holds the groups. Each refinement returns a copy, so a layout stays immutable.
 *
 * {@snippet :
 * SnapshotLayout layout = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT);
 * Path path = layout.resolve(
 *     "com.acme.orders", "OrderTests", "OrderPlaced", null, ".json");
 * // src/test/java/com/acme/orders/snapshots/OrderPlaced/OrderPlaced.snap.approved.json
 * }
 *
 * @param strategy where the snapshot tree is rooted
 * @param grouping the folder a snapshot is grouped into under that root
 * @param wrapperFolder the segment that holds the group folders, ignored by {@link Strategy#LEGACY}
 * @param rootPath the directory {@link Strategy#GLOBAL_ROOT} roots the tree at, empty otherwise
 */
public record SnapshotLayout(Strategy strategy, Grouping grouping, String wrapperFolder, String rootPath) {

    private static final String SOURCE_ROOT = "src/test/java";
    private static final String DEFAULT_WRAPPER_FOLDER = "snapshots";
    private static final String SNAP_APPROVED_SUFFIX = ".snap.approved";
    private static final String LEGACY_SUFFIX = ".approved.txt";

    /**
     * Where {@link SnapshotLayout} roots the snapshot tree.
     *
     * {@snippet :
     * SnapshotLayout layout = SnapshotLayout.legacy();
     * boolean flat = layout.strategy() == SnapshotLayout.Strategy.LEGACY;
     * }
     */
    public enum Strategy {
        /** Beside your test sources, under {@code src/test/java/<package>/<wrapperFolder>}. */
        NEXT_TO_TEST,

        /** Under one directory you name, with the package path beneath it. */
        GLOBAL_ROOT,

        /**
         * The flat layout earlier Strictland wrote: {@code src/test/java/<package>/<leaf>.approved.txt},
         * with no wrapper folder, no grouping, and a {@code .txt} extension regardless of the
         * serializer's.
         */
        LEGACY
    }

    /**
     * A layout that keeps snapshots beside your test sources, grouped per test class under a {@code
     * snapshots} folder. This is the layout to reach for first: the approved file sits next to the test
     * that owns it, so a contract change shows up in the same place as the code that caused it.
     *
     * {@snippet :
     * SnapshotLayout layout = SnapshotLayout.nextToTest();
     * Path path = layout.resolve(
     *     "com.acme.orders", "OrderTests", "OrderPlaced", null, ".json");
     * // src/test/java/com/acme/orders/snapshots/OrderTests/OrderPlaced.snap.approved.json
     * }
     *
     * @return a next-to-test layout, grouped per test class
     */
    public static SnapshotLayout nextToTest() {
        return new SnapshotLayout(Strategy.NEXT_TO_TEST, Grouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, "");
    }

    /**
     * A layout that gathers every snapshot under one directory, with the test's package path beneath
     * it. Reach for this when you'd rather keep approved files out of your source folders, for instance
     * under {@code src/test/resources}.
     *
     * {@snippet :
     * SnapshotLayout layout = SnapshotLayout.globalRoot("src/test/resources/snapshots");
     * Path path = layout.resolve(
     *     "com.acme.orders", "OrderTests", "OrderPlaced", null, ".json");
     * // src/test/resources/snapshots/com/acme/orders/OrderTests/OrderPlaced.snap.approved.json
     * }
     *
     * @param rootPath the directory the snapshot tree is rooted at
     * @return a global-root layout rooted at {@code rootPath}, grouped per test class
     */
    public static SnapshotLayout globalRoot(String rootPath) {
        return new SnapshotLayout(Strategy.GLOBAL_ROOT, Grouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, rootPath);
    }

    /**
     * The flat layout earlier Strictland wrote, for repositories whose approved files predate
     * grouping. It puts {@code <leaf>.approved.txt} straight in the test's package folder, with no
     * wrapper, no group folder, and a {@code .txt} extension whatever the serializer's is.
     *
     * {@snippet :
     * SnapshotLayout layout = SnapshotLayout.legacy();
     * Path path = layout.resolve(
     *     "com.acme.orders", "OrderTests", "OrderPlaced", null, ".json");
     * // src/test/java/com/acme/orders/OrderPlaced.approved.txt
     * }
     *
     * @return the flat legacy layout
     */
    public static SnapshotLayout legacy() {
        return new SnapshotLayout(Strategy.LEGACY, Grouping.PER_TEST_CLASS, DEFAULT_WRAPPER_FOLDER, "");
    }

    /**
     * Returns a copy with a different grouping, for choosing whether snapshots sit together by test
     * class or by message contract. {@link Strategy#LEGACY} ignores grouping, since its layout is flat.
     *
     * {@snippet :
     * SnapshotLayout byContract = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT);
     * }
     *
     * @param grouping the folder a snapshot is grouped into
     * @return a copy of this layout using the given grouping
     */
    public SnapshotLayout grouping(Grouping grouping) {
        return new SnapshotLayout(strategy, grouping, wrapperFolder, rootPath);
    }

    /**
     * Returns a copy with a different wrapper folder, the segment that holds the group folders. The
     * default is {@code snapshots}; set it to match your convention. {@link Strategy#LEGACY} ignores it,
     * since its layout is flat.
     *
     * {@snippet :
     * SnapshotLayout underApproved = SnapshotLayout.nextToTest().wrapperFolder("approved");
     * }
     *
     * @param wrapperFolder the segment that holds the group folders
     * @return a copy of this layout using the given wrapper folder
     */
    public SnapshotLayout wrapperFolder(String wrapperFolder) {
        return new SnapshotLayout(strategy, grouping, wrapperFolder, rootPath);
    }

    /**
     * Resolves the committed approved-file path for a check, from the test that asks and the message it
     * locks down. The result ends in {@code .snap.approved<fileExtension>}, except under {@link
     * Strategy#LEGACY}, which always ends in {@code .approved.txt}.
     *
     * <p>The leaf is the {@code variantLabel} when given, otherwise the {@code messageTypeName}. {@link
     * Grouping#PER_TEST_CLASS} groups under the caller's simple name, {@link Grouping#PER_CONTRACT}
     * groups under the message type.
     *
     * {@snippet :
     * Path path = SnapshotLayout.nextToTest().resolve(
     *     "com.acme.orders", "OrderTests", "OrderPlaced", "withCoupon", ".json");
     * // src/test/java/com/acme/orders/snapshots/OrderTests/withCoupon.snap.approved.json
     * }
     *
     * @param callerPackage the package of the test asking for the snapshot
     * @param callerSimpleName the simple name of the test class asking for the snapshot
     * @param messageTypeName the logical name of the message being locked down
     * @param variantLabel an optional label distinguishing one snapshot from another in the same test
     * @param fileExtension the extension the serializer writes, including the leading dot
     * @return the committed approved-file path
     */
    public Path resolve(
            String callerPackage,
            String callerSimpleName,
            String messageTypeName,
            @Nullable String variantLabel,
            String fileExtension) {
        var packagePath = callerPackage.replace('.', '/');
        var leaf = variantLabel != null ? variantLabel : messageTypeName;

        if (strategy == Strategy.LEGACY) {
            return Path.of(SOURCE_ROOT, packagePath, leaf + LEGACY_SUFFIX);
        }

        var groupFolder = grouping == Grouping.PER_CONTRACT ? messageTypeName : callerSimpleName;
        var fileName = leaf + SNAP_APPROVED_SUFFIX + fileExtension;
        var root = strategy == Strategy.GLOBAL_ROOT
                ? Path.of(rootPath, packagePath)
                : Path.of(SOURCE_ROOT, packagePath, wrapperFolder);
        return root.resolve(groupFolder).resolve(fileName);
    }
}
