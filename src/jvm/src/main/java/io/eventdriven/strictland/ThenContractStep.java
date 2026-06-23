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
    private final Object instance;
    private final @Nullable MessageSnapshot destination;
    private final String version;
    private final ContractContext context;

    ThenContractStep(Object instance, @Nullable MessageSnapshot destination, String version, ContractContext context) {
        this.instance = instance;
        this.destination = destination;
        this.version = version;
        this.context = context;
    }

    /**
     * Confirms the message still serializes exactly as it did when you last approved it, so nothing
     * reading it downstream breaks. A failure means the format changed: a field renamed, a date format
     * switched, a value newly dropped or added.
     *
     * <p>The first run creates the approved file from the current message for you to review and
     * commit; it lives in the committed committed file in git repository, so a later change to the format shows up in
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
        var data = new SnapshotData(context.serializer().serialize(instance));
        context.storage().store(snapshotLocation(), data);
    }

    private SnapshotLocation snapshotLocation() {
        var ext = context.serializer().fileExtension();
        return switch (destination) {
            case null ->
                SnapshotLocation.of(
                        context.typeMapper().name(instance.getClass()), version, SnapshotVariant.DEFAULT, ext);
            case MessageSnapshot.ByClass<?> b ->
                SnapshotLocation.of(
                        context.typeMapper().name(b.messageClass()), resolveVersion(b.version()), b.variant(), ext);
            case MessageSnapshot.ByMessageType b ->
                SnapshotLocation.of(MessageTypeName.of(b.messageType()), resolveVersion(b.version()), b.variant(), ext);
            case MessageSnapshot.ByPath b -> SnapshotLocation.ofRelativePath(b.path());
            case MessageSnapshot.OfInstance<?> ignored ->
                throw new IllegalArgumentException("a message in hand is not a snapshot destination");
        };
    }

    private String resolveVersion(String snapshotVersion) {
        return snapshotVersion.equals(MessageSnapshot.DEFAULT_VERSION) ? version : snapshotVersion;
    }
}
