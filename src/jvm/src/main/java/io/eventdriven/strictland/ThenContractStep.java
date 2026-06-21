package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The check that the message's serialized format still matches what you approved.
 *
 * <p>Reached after {@link GivenStep#whenSerialized()}. Call {@link #thenContractIsUnchanged()} to
 * compare the output against the approved snapshot and fail the test when it has drifted.</p>
 *
 * @param <S> the type of the message under test
 */
public class ThenContractStep<S> {
    private final S instance;
    private final @Nullable Snapshot destination;
    private final String version;
    private final MessageSerializer serializer;
    private final SnapshotStorage storage;
    private final @Nullable SnapshotLayout layout;
    private final String fileExtension;
    private final MessageTypeMapper typeMapper;

    ThenContractStep(
            S instance,
            @Nullable Snapshot destination,
            String version,
            MessageSerializer serializer,
            SnapshotStorage storage,
            @Nullable SnapshotLayout layout,
            String fileExtension,
            MessageTypeMapper typeMapper) {
        this.instance = instance;
        this.destination = destination;
        this.version = version;
        this.serializer = serializer;
        this.storage = storage;
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.typeMapper = typeMapper;
    }

    /**
     * Confirms the message still serializes exactly as it did when you last approved it, so nothing
     * reading it downstream breaks. A failure means the format changed: a field renamed, a date format
     * switched, a value newly dropped or added.
     *
     * <p>The first run creates the approved file from the current message for you to review and
     * commit; it lives in the committed contract registry, so a later change to the format shows up in
     * the same pull request as the code that caused it.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * </pre>
     */
    public void thenContractIsUnchanged() {
        storage.store(snapshotKey(), serializer.serialize(instance));
    }

    private String snapshotKey() {
        return switch (destination) {
            case null -> writePath(SnapshotName.of(typeMapper.name(instance.getClass()), version, null));
            case Snapshot.ByClass<?> b ->
                writePath(SnapshotName.of(typeMapper.name(b.messageClass()), resolveVersion(b.version()), b.variant()));
            case Snapshot.ByMessageType b ->
                writePath(SnapshotName.of(b.messageType(), resolveVersion(b.version()), b.variant()));
            case Snapshot.ByPath b -> b.path().toString();
        };
    }

    private String writePath(SnapshotName name) {
        return layout == null
                ? name.base()
                : name.resolve(layout, fileExtension).toString();
    }

    private String resolveVersion(String snapshotVersion) {
        return snapshotVersion.equals(Snapshot.DEFAULT_VERSION) ? version : snapshotVersion;
    }
}
