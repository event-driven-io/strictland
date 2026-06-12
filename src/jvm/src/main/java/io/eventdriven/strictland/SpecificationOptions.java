package io.eventdriven.strictland;

/**
 * The three seams a specification runs on - how a message is serialized, where snapshots live, and
 * how a message type is named. Build one when you go beyond the JSON defaults: a non-Jackson
 * serializer, snapshots kept elsewhere, or names that follow your event store rather than the class.
 *
 * <p>Start from a serializer with {@link #serializer(MessageSerializer)}, then swap either of the
 * other two with the fluent with-ers, and hand the result to {@link
 * MessageContract#specification(SpecificationOptions)}.
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
     * Starts options from a serializer, your entry point when you go beyond the JSON defaults, with
     * file-backed snapshots and class-name typing until you swap them.
     *
     * @param serializer turns a message into the bytes your application writes, and back
     * @return options you can refine with the with-ers and hand to {@link
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
