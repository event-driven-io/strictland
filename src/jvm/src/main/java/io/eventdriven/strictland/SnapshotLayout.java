package io.eventdriven.strictland;

/**
 * Decides where the snapshot tree is rooted: the directory the registry lives in, and the wrapper folder
 * under it that holds the per-namespace folders. It is a pure value with no I/O of its own; the path of
 * any one snapshot is built by {@link SnapshotLocation} and joined under this root by {@link
 * FileSnapshotStorage}.
 *
 * <p>The layout is a single registry tree. Snapshots gather under {@code rootPath/wrapperFolder}, and a
 * message type's namespace becomes folders beneath that, ending in a folder named after the short
 * message type. So {@code com.acme.orders.OrderPlaced} resolves under {@code
 * src/test/resources/contract-registry/com/acme/orders/OrderPlaced}. A dotless message type has no
 * namespace folders. Each refinement returns a copy, so a layout stays immutable.</p>
 *
 * <pre>
 * SnapshotLayout layout = SnapshotLayout.registry();
 * // snapshots live under src/test/resources/contract-registry
 * </pre>
 *
 * @param rootPath the directory the snapshot tree is rooted at
 * @param wrapperFolder the segment under the root that holds the per-namespace folders
 */
public record SnapshotLayout(String rootPath, String wrapperFolder) {

    private static final String DEFAULT_ROOT_PATH = "src/test/resources";
    private static final String DEFAULT_WRAPPER_FOLDER = "contract-registry";

    /**
     * The default registry layout: snapshots under {@code src/test/resources/contract-registry}, with
     * the message type's namespace as folders beneath it.
     *
     * @return the default registry layout
     */
    public static SnapshotLayout registry() {
        return new SnapshotLayout(DEFAULT_ROOT_PATH, DEFAULT_WRAPPER_FOLDER);
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
        return new SnapshotLayout(rootPath, wrapperFolder);
    }

    /**
     * Returns a copy with a different wrapper folder, the segment under the root that holds the
     * per-namespace folders. The default is {@code contract-registry}; set it to match your convention.
     *
     * <pre>
     * SnapshotLayout underApproved = SnapshotLayout.registry().wrapperFolder("approved");
     * </pre>
     *
     * @param wrapperFolder the segment under the root that holds the per-namespace folders
     * @return a copy of this layout using the given wrapper folder
     */
    public SnapshotLayout wrapperFolder(String wrapperFolder) {
        return new SnapshotLayout(rootPath, wrapperFolder);
    }
}
