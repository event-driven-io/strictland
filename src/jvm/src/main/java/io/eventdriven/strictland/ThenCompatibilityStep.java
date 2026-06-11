package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
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
 * versions share and fail if a required one is missing or a shared value changed.
 *
 * @param <S> the version you started from
 * @param <T> the version you check it against
 */
public class ThenCompatibilityStep<S, T> {
    private final @Nullable Snapshot snapshot;
    private final @Nullable S instance;
    private final Class<T> targetType;
    private final MessageSerializer serializer;
    private final ObjectMapper mapper;
    private final SnapshotStorage storage;
    private final MessageTypeMapper typeMapper;

    ThenCompatibilityStep(
            @Nullable Snapshot snapshot,
            @Nullable S instance,
            Class<T> targetType,
            MessageSerializer serializer,
            ObjectMapper mapper,
            SnapshotStorage storage,
            MessageTypeMapper typeMapper) {
        this.snapshot = snapshot;
        this.instance = instance;
        this.targetType = targetType;
        this.serializer = serializer;
        this.mapper = mapper;
        this.storage = storage;
        this.typeMapper = typeMapper;
    }

    /**
     * Confirms a reader that hasn't upgraded yet can read a message the newer version writes, so you
     * can ship the new shape before everyone reading it has caught up. Fails when the newer version
     * drops or renames a field the older one needs, or writes a shared value differently.
     *
     * {@snippet :
     * MessageContract.specification()
     *     .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
     *     .whenDeserializedAs(OrderPlaced.class)
     *     .thenForwardCompatible();
     * }
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
     * {@snippet :
     * MessageContract.specification()
     *     .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
     *     .whenDeserializedAs(OrderPlaced.class)
     *     .thenForwardCompatible(order -> assertEquals("Alice", order.customer()));
     * }
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
     * {@snippet :
     * MessageContract.specification()
     *     .given(new OrderPlaced(orderId, "Alice"))
     *     .whenDeserializedAs(OrderPlacedWithCoupon.class)
     *     .thenBackwardCompatible();
     * }
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
     * {@snippet :
     * MessageContract.specification()
     *     .given(new OrderPlaced(orderId, "Alice"))
     *     .whenDeserializedAs(OrderPlacedWithCoupon.class)
     *     .thenBackwardCompatible(order -> assertNull(order.couponCode()));
     * }
     *
     * @param extra assertions to run on the deserialized message
     */
    public void thenBackwardCompatible(Consumer<T> extra) {
        verifySharedFields(extra);
    }

    private void verifySharedFields(Consumer<T> extra) {
        var sourceBytes = resolveSourceBytes();
        T deserialized;
        Map<String, Object> sourceMap;
        Map<String, Object> targetMap;
        try {
            deserialized = serializer.deserialize(sourceBytes, targetType);
            if (deserialized == null) {
                throw new AssertionError("Deserialization as " + targetType.getSimpleName() + " returned empty");
            }
            sourceMap = toMap(sourceBytes);
            targetMap = toMap(serializer.serialize(deserialized));
        } catch (UncheckedIOException | IOException e) {
            throw new RuntimeException("Deserialization as " + targetType.getSimpleName() + " failed", e);
        }
        assertRequiredFieldsSatisfied(sourceMap);
        assertSharedFieldsMatch(sourceMap, targetMap);
        extra.accept(deserialized);
    }

    private void assertRequiredFieldsSatisfied(Map<String, Object> sourceMap) {
        if (!targetType.isRecord()) return;
        for (var component : targetType.getRecordComponents()) {
            if (component.getAnnotatedType().getDeclaredAnnotation(Nullable.class) != null) continue;
            if (!sourceMap.containsKey(component.getName()) || sourceMap.get(component.getName()) == null) {
                throw new AssertionError("Required field '"
                        + component.getName()
                        + "' in "
                        + targetType.getSimpleName()
                        + " is null after deserialization. Source had keys: "
                        + sourceMap.keySet());
            }
        }
    }

    private void assertSharedFieldsMatch(Map<String, Object> sourceMap, Map<String, Object> targetMap) {
        var sourceOnly = new ArrayList<>(sourceMap.keySet());
        sourceOnly.removeAll(targetMap.keySet());
        var targetOnly = new ArrayList<>(targetMap.keySet());
        targetOnly.removeAll(sourceMap.keySet());
        var diagnostics = " [source-only: " + sourceOnly + ", target-only: " + targetOnly + "]";

        if (!sourceOnly.isEmpty() && !targetOnly.isEmpty()) {
            throw new AssertionError(
                    "Structural incompatibility detected: source and target have different unmapped fields"
                            + diagnostics);
        }

        var sharedKeys = new HashSet<>(sourceMap.keySet());
        sharedKeys.retainAll(targetMap.keySet());

        for (var key : sharedKeys) {
            assertEquals(
                    sourceMap.get(key),
                    targetMap.get(key),
                    "Field '" + key + "' value differs between versions" + diagnostics);
        }
    }

    private Map<String, Object> toMap(byte[] bytes) throws IOException {
        return mapper.readValue(bytes, new TypeReference<>() {});
    }

    private byte[] resolveSourceBytes() {
        if (instance != null) {
            try {
                return serializer.serialize(instance);
            } catch (UncheckedIOException e) {
                throw new RuntimeException(
                        "Serialization of " + instance.getClass().getSimpleName() + " failed", e);
            }
        }
        if (snapshot == null) {
            throw new IllegalStateException("Either a snapshot or an instance is required");
        }
        var name =
                switch (snapshot) {
                    case Snapshot.ByClass<?> s -> typeMapper.name(s.sourceType());
                    case Snapshot.ByMessageType s -> s.messageType();
                    case Snapshot.ByPath s -> s.path().toString();
                };
        return storage.read(name).orElseThrow(() -> new RuntimeException("Cannot read snapshot file: " + name));
    }
}
