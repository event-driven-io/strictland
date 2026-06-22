package io.eventdriven.strictland;

/**
 * Which variant of a message type's data a snapshot holds, when one type and version keeps several
 * snapshots. Modelling it as a type rather than a bare string means an unset variant and a named one are
 * distinct cases the compiler tells apart, so a family read can never collide with a real label.
 *
 * <p>Most checks leave it {@link #UNSET}: an unset variant on a read means "every variant of this type
 * and version" (a family read), and on a write lands under the reserved {@link #DEFAULT} label. You set a
 * {@link ByLabel} variant only when one type and version needs more than one snapshot, naming each so
 * they sit side by side in the registry.</p>
 */
public sealed interface SnapshotVariant
        permits SnapshotVariant.ByLabel, SnapshotVariant.Unset, SnapshotVariant.Default {

    /** No variant is pinned: a family read marker, and an unnamed write that lands under {@link #DEFAULT}. */
    SnapshotVariant UNSET = new Unset();

    /** The variant an unnamed snapshot is written under, when no label is given. */
    SnapshotVariant DEFAULT = new Default();

    /**
     * A named snapshot variant (e.g. "WithOnlyRequiredData"), whose label is the trailing segment of the
     * snapshot's file name.
     *
     * @param name the label naming the variant of the snapshotted message
     */
    record ByLabel(String name) implements SnapshotVariant {}

    /**
     * No variant pinned: a family read, every variant of one message type and version, and an unnamed
     * write that lands under {@link #DEFAULT}. Use the {@link #UNSET} constant rather than building one.
     */
    record Unset() implements SnapshotVariant {}

    /**
     * The variant an unnamed snapshot is written under, kept distinct from any {@link ByLabel} so the
     * default case is a type the compiler tells apart rather than a magic label. Use the {@link #DEFAULT}
     * constant rather than building one.
     */
    record Default() implements SnapshotVariant {}

    /**
     * Names a snapshot variant by its label, for a check that keeps several snapshots of one message type
     * and version side by side.
     *
     * @param name the label naming the variant of the snapshotted message
     * @return the named variant
     */
    static ByLabel named(String name) {
        return new ByLabel(name);
    }
}
