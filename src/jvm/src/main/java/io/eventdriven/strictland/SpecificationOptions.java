package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The options behind {@link MessageContract#specification(SpecificationOptions)}: how a message is
 * serialized, where its snapshot is kept, how its type is named, and which layout decides the
 * snapshot's path. If you're using JSON, {@link Json.Jackson} gives you one already built; you
 * assemble your own when you serialize another way, keep snapshots somewhere other than next to your
 * test, or name messages after your event store rather than the Java class.
 *
 * <p>Start from the serializer, the choice that has to match what your application ships. Every other
 * setting is optional and left unset by default, so it falls through to the global {@link Strictland}
 * default, then {@code strictland.properties}, then the built-in default, resolved once when the spec
 * builds its storage. You set one only when you want this spec to override that chain, with {@link
 * #snapshotStorage(SnapshotStorage)}, {@link #snapshotLayout(SnapshotLayout)}, or {@link
 * #messageTypeMapper(MessageTypeMapper)}. Each returns a new copy, so the options stay immutable.</p>
 *
 * <pre>
 * MessageContract.specification(
 *         SpecificationOptions.serializer(new CsvMessageSerializer())
 *             .snapshotLayout(SnapshotLayout.registry())
 *             .messageTypeMapper(MessageTypeMapper.fullyQualifiedName()))
 *     .given(new MemberJoined(memberId, "Alice"))
 *     .whenDeserializedAs(MemberJoined.class)
 *     .thenBackwardCompatible();
 * </pre>
 *
 * @param serializer turns a message into the bytes your application writes, and back
 * @param storage where approved snapshots are read from and written to, or null to resolve a layout
 * @param typeMapper names a message type for locating its snapshot, or null for the default
 * @param snapshotLayout decides where a snapshot's file lives, or null to fall through the config
 *     chain
 */
public record SpecificationOptions(
        MessageSerializer serializer,
        @Nullable SnapshotStorage storage,
        @Nullable MessageTypeMapper typeMapper,
        @Nullable SnapshotLayout snapshotLayout) {

    /**
     * Creates options from your serializer, with every other setting left unset. Providing the
     * serializer your application already uses keeps the check aligned with the bytes you ship. Snapshot
     * storage, type naming, and layout stay unset until you set them, falling back to the global {@link
     * Strictland} default, then {@code strictland.properties}, then the built-in default.
     *
     * @param serializer turns a message into the bytes your application writes, and back
     * @return options you can refine with {@link #snapshotStorage(SnapshotStorage)}, {@link
     *     #snapshotLayout(SnapshotLayout)}, and {@link #messageTypeMapper(MessageTypeMapper)}, then hand
     *     to {@link MessageContract#specification(SpecificationOptions)}
     */
    public static SpecificationOptions serializer(MessageSerializer serializer) {
        return new SpecificationOptions(serializer, null, null, null);
    }

    /**
     * Returns a copy with a different snapshot store, for keeping approved snapshots somewhere other
     * than the default contract registry. A store set this way is used as-is, ahead of any layout.
     *
     * @param storage where approved snapshots are read from and written to
     * @return a copy of these options using the given storage
     */
    public SpecificationOptions snapshotStorage(SnapshotStorage storage) {
        return new SpecificationOptions(serializer, storage, typeMapper, snapshotLayout);
    }

    /**
     * Returns a copy with a different type mapper, for naming snapshots after your event store's
     * message types rather than the Java class.
     *
     * @param typeMapper names a message type for locating its snapshot
     * @return a copy of these options using the given mapper
     */
    public SpecificationOptions messageTypeMapper(MessageTypeMapper typeMapper) {
        return new SpecificationOptions(serializer, storage, typeMapper, snapshotLayout);
    }

    /**
     * Returns a copy with a snapshot layout for this spec, overriding the global {@link Strictland}
     * default and {@code strictland.properties} for this check alone. It takes effect only when no
     * explicit {@link #snapshotStorage(SnapshotStorage)} is set, since a store decides its own paths.
     *
     * <pre>
     * SpecificationOptions options =
     *     Json.Jackson.defaults().snapshotLayout(SnapshotLayout.registry().rootPath("src/test/resources"));
     * </pre>
     *
     * @param layout decides where this spec's snapshot file lives
     * @return a copy of these options using the given layout
     */
    public SpecificationOptions snapshotLayout(SnapshotLayout layout) {
        return new SpecificationOptions(serializer, storage, typeMapper, layout);
    }
}
