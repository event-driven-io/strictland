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
    private final MessageTypeMapper typeMapper;

    ThenContractStep(
            S instance,
            @Nullable Snapshot destination,
            MessageSerializer serializer,
            SnapshotStorage storage,
            MessageTypeMapper typeMapper) {
        this.instance = instance;
        this.destination = destination;
        this.serializer = serializer;
        this.storage = storage;
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
     * MessageContract.specification()
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * }
     */
    public void thenContractIsUnchanged() {
        storage.store(snapshotName(), serializer.serialize(instance));
    }

    private String snapshotName() {
        if (destination == null) {
            return typeMapper.name(instance.getClass());
        }
        return switch (destination) {
            case Snapshot.ByClass<?> b -> typeMapper.name(b.sourceType());
            case Snapshot.ByMessageType b -> b.messageType();
            case Snapshot.ByPath b -> b.path().toString();
        };
    }
}
