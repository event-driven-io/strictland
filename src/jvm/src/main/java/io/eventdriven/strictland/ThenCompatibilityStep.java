package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * The "then" step of the contract DSL that verifies forward and backward compatibility of deserialization.
 */
public class ThenCompatibilityStep<S, T> {
    private final @Nullable Snapshot snapshot;
    private final @Nullable S instance;
    private final Class<T> targetType;
    private final ObjectMapper mapper;

    ThenCompatibilityStep(@Nullable Snapshot snapshot, @Nullable S instance, Class<T> targetType, ObjectMapper mapper) {
        this.snapshot = snapshot;
        this.instance = instance;
        this.targetType = targetType;
        this.mapper = mapper;
    }

    public void thenForwardCompatible() {
        verifySharedFields(t -> {});
    }

    public void thenForwardCompatible(Consumer<T> extra) {
        verifySharedFields(extra);
    }

    public void thenBackwardCompatible() {
        verifySharedFields(t -> {});
    }

    public void thenBackwardCompatible(Consumer<T> extra) {
        verifySharedFields(extra);
    }

    private void verifySharedFields(Consumer<T> extra) {
        var sourceBytes = resolveSourceBytes();
        T deserialized;
        Map<String, Object> sourceMap;
        Map<String, Object> targetMap;
        try {
            deserialized = mapper.readValue(sourceBytes, targetType);
            if (deserialized == null) {
                throw new AssertionError("Deserialization as " + targetType.getSimpleName() + " returned empty");
            }
            sourceMap = toMap(sourceBytes);
            targetMap = toMap(mapper.writeValueAsBytes(deserialized));
        } catch (IOException e) {
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
                return mapper.writeValueAsBytes(instance);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Serialization of " + instance.getClass().getSimpleName() + " failed", e);
            }
        }
        if (snapshot == null) {
            throw new IllegalStateException("Either a snapshot or an instance is required");
        }
        var path =
                switch (snapshot) {
                    case Snapshot.ByClass<?> s ->
                        SnapshotPathResolver.resolve(s.sourceType().getSimpleName());
                    case Snapshot.ByMessageType s -> SnapshotPathResolver.resolve(s.messageType());
                    case Snapshot.ByPath s -> s.path();
                };
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read snapshot file: " + path, e);
        }
    }
}
