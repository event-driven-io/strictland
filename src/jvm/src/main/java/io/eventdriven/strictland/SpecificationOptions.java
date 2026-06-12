package io.eventdriven.strictland;

/**
 * The options behind {@link MessageContract#specification(SpecificationOptions)}: how a message is
 * serialized, where its snapshot is kept, and how its type is named. If you're using JSON, {@link
 * Json.Jackson} gives you one already built; you assemble your own when you serialize another way,
 * keep snapshots somewhere other than next to your test, or name messages after your event store
 * rather than the Java class.
 *
 * <p>Start from the serializer, the choice that has to match what your application ships. The
 * snapshot storage and type mapper come with working defaults, a file next to your test named after
 * the class, so you set them only when you need to with {@link #snapshotStorage(SnapshotStorage)} or
 * {@link #messageTypeMapper(MessageTypeMapper)}. Each returns a new copy, so the options stay
 * immutable.
 *
 * {@snippet :
 * MessageContract.specification(
 *         SpecificationOptions.serializer(new CsvMessageSerializer())
 *             .snapshotStorage(Snapshots.files())
 *             .messageTypeMapper(MessageTypeMapper.simpleName()))
 *     .given(new MemberJoined(memberId, "Alice"))
 *     .whenDeserializedAs(MemberJoined.class)
 *     .thenBackwardCompatible();
 * }
 *
 * @param serializer turns a message into the bytes your application writes, and back
 * @param storage where approved snapshots are read from and written to
 * @param typeMapper names a message type for locating its snapshot
 */
public record SpecificationOptions(
        MessageSerializer serializer, SnapshotStorage storage, MessageTypeMapper typeMapper) {

    /**
     * Creates options from your serializer. Providing the serializer your application already uses
     * keeps the check aligned with the bytes you ship. Snapshot storage and type naming default to a
     * file next to your test named after the class, until you set them with {@link
     * #snapshotStorage(SnapshotStorage)} or {@link #messageTypeMapper(MessageTypeMapper)}.
     *
     * @param serializer turns a message into the bytes your application writes, and back
     * @return options you can refine with {@link #snapshotStorage(SnapshotStorage)} and {@link
     *     #messageTypeMapper(MessageTypeMapper)}, then hand to {@link
     *     MessageContract#specification(SpecificationOptions)}
     */
    public static SpecificationOptions serializer(MessageSerializer serializer) {
        return new SpecificationOptions(serializer, Snapshots.files(), MessageTypeMapper.simpleName());
    }

    /**
     * Returns a copy with a different snapshot store, for keeping approved snapshots somewhere other
     * than next to your test.
     *
     * @param storage where approved snapshots are read from and written to
     * @return a copy of these options using the given storage
     */
    public SpecificationOptions snapshotStorage(SnapshotStorage storage) {
        return new SpecificationOptions(serializer, storage, typeMapper);
    }

    /**
     * Returns a copy with a different type mapper, for naming snapshots after your event store's
     * message types rather than the Java class.
     *
     * @param typeMapper names a message type for locating its snapshot
     * @return a copy of these options using the given mapper
     */
    public SpecificationOptions messageTypeMapper(MessageTypeMapper typeMapper) {
        return new SpecificationOptions(serializer, storage, typeMapper);
    }
}
