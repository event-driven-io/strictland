package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * Decides where an approved snapshot file lives, and what it's called, from the message it locks down
 * and the snapshot's base name. It is a pure value: hand it the message type, the base name, and the
 * serializer's extension, get back a {@link Path}, with no I/O of its own.
 *
 * <p>The layout is a single registry tree. Snapshots gather under {@code rootPath/wrapperFolder}, and
 * the message type's namespace becomes folders beneath that, ending in a folder named after the short
 * message type. So {@code com.acme.orders.OrderPlaced} resolves under {@code
 * src/test/resources/contract-snapshots/com/acme/orders/OrderPlaced}. A dotless message type has no
 * namespace folders. Each refinement returns a copy, so a layout stays immutable.</p>
 *
 * <pre>
 * SnapshotLayout layout = SnapshotLayout.registry();
 * Path path = layout.resolve("com.acme.orders.OrderPlaced", "OrderPlaced", ".json");
 * // src/test/resources/contract-snapshots/com/acme/orders/OrderPlaced/OrderPlaced.snap.approved.json
 * </pre>
 *
 * @param rootPath the directory the snapshot tree is rooted at
 * @param wrapperFolder the segment under the root that holds the per-namespace folders
 * @param testClassNaming how the test class is named in an owned snapshot's file name
 */
public record SnapshotLayout(String rootPath, String wrapperFolder, TestClassNaming testClassNaming) {

    private static final String DEFAULT_ROOT_PATH = "src/test/resources";
    private static final String DEFAULT_WRAPPER_FOLDER = "contract-snapshots";
    private static final String SNAP_APPROVED_SUFFIX = ".snap.approved";

    /**
     * The default registry layout: snapshots under {@code src/test/resources/contract-snapshots}, with
     * the message type's namespace as folders beneath it and the test class named by its simple name.
     *
     * <pre>
     * SnapshotLayout layout = SnapshotLayout.registry();
     * Path path = layout.resolve("com.acme.orders.OrderPlaced", "OrderPlaced", ".json");
     * // src/test/resources/contract-snapshots/com/acme/orders/OrderPlaced/OrderPlaced.snap.approved.json
     * </pre>
     *
     * @return the default registry layout
     */
    public static SnapshotLayout registry() {
        return new SnapshotLayout(DEFAULT_ROOT_PATH, DEFAULT_WRAPPER_FOLDER, TestClassNaming.SIMPLE);
    }

    /**
     * Returns a copy rooted at a different directory, for keeping the snapshot tree somewhere other than
     * {@code src/test/resources}.
     *
     * <pre>
     * SnapshotLayout underModule = SnapshotLayout.registry().rootPath("modules/orders/src/test/resources");
     * </pre>
     *
     * @param rootPath the directory the snapshot tree is rooted at
     * @return a copy of this layout rooted at the given directory
     */
    public SnapshotLayout rootPath(String rootPath) {
        return new SnapshotLayout(rootPath, wrapperFolder, testClassNaming);
    }

    /**
     * Returns a copy with a different wrapper folder, the segment under the root that holds the
     * per-namespace folders. The default is {@code contract-snapshots}; set it to match your convention.
     *
     * <pre>
     * SnapshotLayout underApproved = SnapshotLayout.registry().wrapperFolder("approved");
     * </pre>
     *
     * @param wrapperFolder the segment under the root that holds the per-namespace folders
     * @return a copy of this layout using the given wrapper folder
     */
    public SnapshotLayout wrapperFolder(String wrapperFolder) {
        return new SnapshotLayout(rootPath, wrapperFolder, testClassNaming);
    }

    /**
     * Returns a copy that names the test class differently inside an owned snapshot's file name, for
     * keeping snapshots distinct when two test classes share a simple name. It changes only the file
     * name, never the folder a snapshot resolves under.
     *
     * <pre>
     * SnapshotLayout qualified = SnapshotLayout.registry().testClassNaming(TestClassNaming.FULL);
     * </pre>
     *
     * @param testClassNaming how the test class is named in an owned snapshot's file name
     * @return a copy of this layout using the given test-class naming
     */
    public SnapshotLayout testClassNaming(TestClassNaming testClassNaming) {
        return new SnapshotLayout(rootPath, wrapperFolder, testClassNaming);
    }

    /**
     * Resolves the committed approved-file path for a check, from the message type, the snapshot's base
     * name, and the serializer's extension. The result ends in {@code .snap.approved<fileExtension>}.
     *
     * <p>The folder mirrors the message type's namespace, everything before the last dot, as directories
     * under {@code rootPath/wrapperFolder}, then a folder named after the short message type, the part
     * after the last dot. A dotless message type has no namespace folders.</p>
     *
     * <pre>
     * Path path = SnapshotLayout.registry().resolve("com.acme.orders.OrderPlaced", "withCoupon", ".json");
     * // src/test/resources/contract-snapshots/com/acme/orders/OrderPlaced/withCoupon.snap.approved.json
     * </pre>
     *
     * @param messageType the message type's fully-qualified name, whose namespace and short name shape
     *     the folder
     * @param snapshotName the base name of the snapshot's file
     * @param fileExtension the extension the serializer writes, including the leading dot
     * @return the committed approved-file path
     */
    public Path resolve(String messageType, String snapshotName, String fileExtension) {
        var fileName = snapshotName + SNAP_APPROVED_SUFFIX + fileExtension;
        var folder = Path.of(rootPath, wrapperFolder);
        var namespace = SnapshotName.namespace(messageType);
        if (!namespace.isEmpty()) {
            folder = folder.resolve(namespace.replace('.', '/'));
        }
        return folder.resolve(SnapshotName.shortName(messageType)).resolve(fileName);
    }
}
