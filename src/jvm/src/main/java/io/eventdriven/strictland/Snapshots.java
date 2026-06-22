package io.eventdriven.strictland;

/**
 * The storage presets for where approved snapshots live, so you pick one by name rather than wiring a
 * {@link SnapshotStorage} yourself. {@link #files()} is the default, keeping each snapshot in a file
 * in the committed contract registry.
 *
 * <pre>
 * MessageContract.specification(
 *         SpecificationOptions.serializer(new CsvMessageSerializer())
 *             .snapshotStorage(Snapshots.files()))
 *     .given(new MemberJoined(memberId, "Alice"))
 *     .whenDeserializedAs(MemberJoined.class)
 *     .thenBackwardCompatible();
 * </pre>
 */
public interface Snapshots {

    /**
     * Storage that keeps each approved snapshot in a committed contract registry, under
     * {@code src/test/resources/contract-registry} by default, so a contract change lands in the same
     * pull request as the code that caused it. This is what a specification uses unless you swap it.
     *
     * @return file-backed snapshot storage
     */
    static SnapshotStorage files() {
        return new FileSnapshotStorage(SnapshotLayout.registry());
    }
}
