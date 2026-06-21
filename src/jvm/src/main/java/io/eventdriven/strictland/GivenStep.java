package io.eventdriven.strictland;

import org.jspecify.annotations.Nullable;

/**
 * The version you put under contract with {@code given(...)}, ready for you to check.
 *
 * <p>From here, lock its shape so accidental changes get caught with {@link #whenSerialized()} (or
 * {@link #whenSerializedAs(Snapshot)} to choose the snapshot), or read it as another version to confirm
 * the two are compatible with {@link #whenDeserializedAs(Class)}.</p>
 *
 * @param <S> the type of the message under test
 */
public class GivenStep<S> {
    final @Nullable Snapshot snapshot;
    final @Nullable S instance;
    final String version;
    final MessageSerializer serializer;
    final SnapshotStorage storage;
    final @Nullable SnapshotLayout layout;
    final String fileExtension;
    final MessageTypeMapper typeMapper;

    GivenStep(
            @Nullable Snapshot snapshot,
            @Nullable S instance,
            String version,
            MessageSerializer serializer,
            SnapshotStorage storage,
            @Nullable SnapshotLayout layout,
            String fileExtension,
            MessageTypeMapper typeMapper) {
        this.snapshot = snapshot;
        this.instance = instance;
        this.version = version;
        this.serializer = serializer;
        this.storage = storage;
        this.layout = layout;
        this.fileExtension = fileExtension;
        this.typeMapper = typeMapper;
    }

    /**
     * Serializes the message you defined with {@code given(...)}.
     *
     * <p>The next step, {@link ThenContractStep#thenContractIsUnchanged()}, compares this serialized
     * output against an approved snapshot, named after the message's type and saved in the contract registry,
     * and fails the test if the format has changed.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * </pre>
     *
     * @return the step where you check the serialized result against the snapshot
     * @throws IllegalStateException if you started from a saved snapshot rather than a live instance,
     *     since there's nothing to serialize
     */
    public ThenContractStep<S> whenSerialized() {
        var instance = requireInstance();
        return new ThenContractStep<>(instance, null, version, serializer, storage, layout, fileExtension, typeMapper);
    }

    /**
     * Serializes the message, with the {@code then} step checking it against a snapshot you choose.
     *
     * <p>Like {@link #whenSerialized()}, but you pick the approved file, by message-type name, by
     * class, or by path, instead of letting it default to the class name. Useful when one class
     * produces several snapshots or the file lives elsewhere.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderInitiatedV2(id, null, initiatedAt))
     *     .whenSerializedAs(Snapshot.forMessageType("OrderInitiatedV2_NullField"))
     *     .thenContractIsUnchanged();
     * </pre>
     *
     * @param destination the approved snapshot the {@code then} step compares against
     * @return the step where you check the serialized result against the snapshot
     * @throws IllegalStateException if you started from a saved snapshot rather than a live instance,
     *     since there's nothing to serialize
     */
    public ThenContractStep<S> whenSerializedAs(Snapshot destination) {
        var instance = requireInstance();
        return new ThenContractStep<>(
                instance, destination, version, serializer, storage, layout, fileExtension, typeMapper);
    }

    /**
     * Serializes the message, with the {@code then} step checking it against a snapshot you choose.
     *
     * <p>Like {@link #whenSerialized()}, but you pick the approved file, by message-type name, by
     * class, or by path, instead of letting it default to the class name. Useful when one class
     * produces several snapshots or the file lives elsewhere.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderInitiatedV2(id, null, initiatedAt))
     *     .whenSerializedAs(SnapshotVariant.named("OrderInitiatedV2_NullField"))
     *     .thenContractIsUnchanged();
     * </pre>
     *
     * @param variant the named snapshot variant for the specific set of test data
     * @return the step where you check the serialized result against the snapshot
     * @throws IllegalStateException if you started from a saved snapshot rather than a live instance,
     *     since there's nothing to serialize
     */
    public ThenContractStep<S> whenSerializedAs(SnapshotVariant variant) {
        var instance = requireInstance();
        var label = ((SnapshotVariant.ByLabel) variant).name();
        return new ThenContractStep<>(
                instance,
                Snapshot.of(instance.getClass()).variant(label),
                version,
                serializer,
                storage,
                layout,
                fileExtension,
                typeMapper);
    }

    private S requireInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "whenSerialized() requires an instance - use MessageContract.given(instance), not MessageContract.given(Snapshot)");
        }
        return instance;
    }

    /**
     * Deserializes the message's data into another version, {@code targetType}.
     *
     * <p>The next {@code then} step compares the fields the two versions share and fails if a required
     * one is missing or a shared value changed. Pick the direction: {@link
     * ThenCompatibilityStep#thenForwardCompatible()} when an older reader takes newer data, {@link
     * ThenCompatibilityStep#thenBackwardCompatible()} when a newer reader takes older data.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
     *     .whenDeserializedAs(OrderPlaced.class)
     *     .thenForwardCompatible();
     * </pre>
     *
     * @param targetType the version to read the data as
     * @param <T> the target message type
     * @return the step where you check compatibility, choosing the direction
     */
    public <T> ThenCompatibilityStep<S, T> whenDeserializedAs(Class<T> targetType) {
        return new ThenCompatibilityStep<>(
                snapshot, instance, version, targetType, serializer, storage, layout, typeMapper);
    }
}
