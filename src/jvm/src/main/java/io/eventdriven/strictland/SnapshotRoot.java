package io.eventdriven.strictland;

/**
 * Says where {@link SnapshotLayout} roots the snapshot tree, the first of the two orthogonal axes a
 * layout decides: the root here, the folder under it in {@link SnapshotGrouping}. Pick the one based
 * on where you want approved files to live: beside your test sources, or under one directory you
 * name.
 *
 * <pre>
 * SnapshotLayout layout = SnapshotLayout.globalRoot("src/test/resources/snapshots");
 * boolean global = layout.location() == SnapshotRoot.GLOBAL_ROOT;
 * </pre>
 */
public enum SnapshotRoot {

    /** Beside your test sources, under {@code <testSourceDir>/<wrapperFolder>}. */
    NEXT_TO_TEST,

    /** Under one directory you name, with the package path beneath it. */
    GLOBAL_ROOT
}
