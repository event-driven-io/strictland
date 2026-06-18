package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The check that the message's serialized format still matches what you approved.
 *
 * <p>Reached after {@link GivenStep#whenSerialized()}. Call {@link #thenContractIsUnchanged()} to
 * compare the output against the approved snapshot and fail the test when it has drifted.
 *
 * @param <S> the type of the message under test
 */
public class ThenContractStep<S> {
    private final S instance;
    private final @Nullable Snapshot destination;
    private final MessageSerializer serializer;
    private final SnapshotStorage storage;
    private final @Nullable SnapshotLocation location;
    private final MessageTypeMapper typeMapper;

    ThenContractStep(
            S instance,
            @Nullable Snapshot destination,
            MessageSerializer serializer,
            SnapshotStorage storage,
            @Nullable SnapshotLocation location,
            MessageTypeMapper typeMapper) {
        this.instance = instance;
        this.destination = destination;
        this.serializer = serializer;
        this.storage = storage;
        this.location = location;
        this.typeMapper = typeMapper;
    }

    /**
     * Confirms the message still serializes exactly as it did when you last approved it, so nothing
     * reading it downstream breaks. A failure means the format changed: a field renamed, a date format
     * switched, a value newly dropped or added.
     *
     * <p>The first run creates the approved file from the current message for you to review and
     * commit; it lives next to your test, so a later change to the format shows up in the same pull
     * request as the code that caused it.
     *
     * {@snippet :
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * }
     */
    public void thenContractIsUnchanged() {
        storage.store(snapshotKey(), serializer.serialize(instance));
    }

    private String snapshotKey() {
        return switch (destination) {
            case null -> byContract(typeMapper.name(instance.getClass()));
            case Snapshot.ByClass<?> b -> byContract(typeMapper.name(b.sourceType()));
            case Snapshot.ByMessageType b -> byContract(b.messageType());
            case Snapshot.ByVariant b -> key(typeMapper.name(instance.getClass()), b.label());
            case Snapshot.ByPath b -> b.path().toString();
        };
    }

    private String byContract(String contractName) {
        return key(contractName, contractName);
    }

    private String key(String messageType, String snapshotName) {
        return location != null ? location.resolve(messageType, snapshotName) : snapshotName;
    }
}
