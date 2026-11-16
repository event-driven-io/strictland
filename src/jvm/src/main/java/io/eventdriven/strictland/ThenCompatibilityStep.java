package io.eventdriven.strictland;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * The check that an old and a new version of the message still work together, so evolving it doesn't
 * strand the messages already in your store or on the wire.
 *
 * <p>Reached after {@link GivenStep#whenDeserializedAs(Class)}. Use {@link #thenBackwardCompatible()}
 * to confirm the newer version still reads a message the older one wrote - the events you stored last
 * year, a request already sent. Use {@link #thenForwardCompatible()} to confirm a reader that hasn't
 * upgraded yet still reads a message the newer version writes. Both compare the fields the two
 * versions share, at every depth, and fail if a required one is missing or a shared value changed.
 * A stored snapshot is first read as its original type, taken from its class or resolved by your
 * {@link MessageTypeMapper}. A snapshot referenced only by path, or by a type name the mapper can't
 * resolve, has no original type, so only its required fields are checked; assert its values through
 * the overloads that take a {@link Consumer}.</p>
 *
 * @param <S> the version you started from
 * @param <T> the version you check it against
 */
public class ThenCompatibilityStep<S, T> {
    private final MessageSnapshot source;
    private final String version;
    private final Class<T> targetType;
    private final ContractContext context;
    private final MessageSerializer serializer;

    ThenCompatibilityStep(MessageSnapshot source, String version, Class<T> targetType, ContractContext context) {
        this.source = source;
        this.version = version;
        this.targetType = targetType;
        this.context = context;
        this.serializer = context.serializer();
    }

    /**
     * Confirms a reader that hasn't upgraded yet can read a message the newer version writes, so you
     * can ship the new shape before everyone reading it has caught up. Fails when the newer version
     * drops or renames a field the older one needs, or writes a shared value differently.
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
     *     .whenDeserializedAs(OrderPlaced.class)
     *     .thenForwardCompatible();
     * </pre>
     */
    public void thenForwardCompatible() {
        verifySharedFields(t -> {});
    }

    /**
     * Confirms a reader that hasn't upgraded yet can read a message the newer version writes, and
     * hands you the deserialized result so you can assert on specific values too, not just the shared
     * fields. Fails when the newer version drops or renames a field the older one needs, or writes a
     * shared value differently.
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
     *     .whenDeserializedAs(OrderPlaced.class)
     *     .thenForwardCompatible(order -> assertEquals("Alice", order.customer()));
     * </pre>
     *
     * @param extra assertions to run on the deserialized message
     */
    public void thenForwardCompatible(Consumer<T> extra) {
        verifySharedFields(extra);
    }

    /**
     * Confirms the newer version can still read a message the older one wrote, so moving to it doesn't
     * break on the events you've already stored or the messages still in flight. Fails when the newer
     * version needs a field the older one never wrote, or reads a shared value differently.
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice"))
     *     .whenDeserializedAs(OrderPlacedWithCoupon.class)
     *     .thenBackwardCompatible();
     * </pre>
     */
    public void thenBackwardCompatible() {
        verifySharedFields(t -> {});
    }

    /**
     * Confirms the newer version can still read a message the older one wrote, and hands you the
     * deserialized result so you can assert on specific values too, not just the shared fields. Fails
     * when the newer version needs a field the older one never wrote, or reads a shared value
     * differently.
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice"))
     *     .whenDeserializedAs(OrderPlacedWithCoupon.class)
     *     .thenBackwardCompatible(order -> assertNull(order.couponCode()));
     * </pre>
     *
     * @param extra assertions to run on the deserialized message
     */
    public void thenBackwardCompatible(Consumer<T> extra) {
        verifySharedFields(extra);
    }

    private record Source(
            SnapshotData data,
            @Nullable Object instance,
            @Nullable Class<?> type) {}

    private void verifySharedFields(Consumer<T> extra) {
        for (var source : resolveSources()) {
            verifyOne(source, extra);
        }
    }

    private void verifyOne(Source source, Consumer<T> extra) {
        var target = read(source.data(), targetType);
        var original = source.instance();
        if (original == null && source.type() != null) {
            original = read(source.data(), source.type());
        }
        var failure = original != null
                ? StructuralComparison.difference(original, target)
                : StructuralComparison.missingRequiredField(target);
        if (failure != null) {
            throw new AssertionError(failure);
        }
        extra.accept(target);
    }

    private <R> R read(SnapshotData data, Class<R> type) {
        R value;
        try {
            value = serializer.deserialize(data.bytes(), type);
        } catch (UncheckedIOException e) {
            throw new RuntimeException("Deserialization as " + type.getSimpleName() + " failed", e);
        }
        if (value == null) {
            throw new AssertionError("Deserialization as " + type.getSimpleName() + " returned empty");
        }
        return value;
    }

    private List<Source> resolveSources() {
        var ext = "." + serializer.fileExtension();
        return switch (source) {
            case MessageSnapshot.OfInstance<?> v -> {
                var instance = v.message();
                try {
                    yield List.of(new Source(new SnapshotData(serializer.serialize(instance)), instance, null));
                } catch (UncheckedIOException e) {
                    throw new RuntimeException(
                            "Serialization of " + instance.getClass().getSimpleName() + " failed", e);
                }
            }
            case MessageSnapshot.ByPath byPath ->
                List.of(new Source(readOne(SnapshotLocation.ofRelativePath(byPath.path())), null, null));
            case MessageSnapshot.ByClass<?> s ->
                resolveFamilyOrVariant(
                        context.typeMapper().name(s.messageClass()),
                        resolveVersion(s.version()),
                        s.variant(),
                        ext,
                        s.messageClass());
            case MessageSnapshot.ByMessageType s ->
                resolveFamilyOrVariant(
                        MessageTypeName.of(s.messageType()),
                        resolveVersion(s.version()),
                        s.variant(),
                        ext,
                        context.typeMapper().type(s.messageType()).orElse(null));
        };
    }

    private List<Source> resolveFamilyOrVariant(
            MessageTypeName messageType,
            String resolvedVersion,
            SnapshotVariant variant,
            String ext,
            @Nullable Class<?> type) {
        if (variant instanceof SnapshotVariant.Unset) {
            var payloads = context.storage().readAll(SnapshotFilter.of(messageType, resolvedVersion));
            if (payloads.isEmpty()) {
                throw new RuntimeException(
                        "Cannot read snapshot file: " + messageType.shortName() + "." + resolvedVersion + ".");
            }
            return payloads.stream()
                    .map(payload -> new Source(payload, null, type))
                    .toList();
        }
        return List.of(
                new Source(readOne(SnapshotLocation.of(messageType, resolvedVersion, variant, ext)), null, type));
    }

    private SnapshotData readOne(SnapshotLocation location) {
        return context.storage().read(location);
    }

    private String resolveVersion(String snapshotVersion) {
        return snapshotVersion.equals(MessageSnapshot.DEFAULT_VERSION) ? version : snapshotVersion;
    }
}
