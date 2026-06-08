package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

/**
 * The "given" step of the contract DSL, holding either a snapshot or an instance to work with.
 */
public class GivenStep<S> {
    final @Nullable Snapshot snapshot;
    final @Nullable S instance;
    final ObjectMapper mapper;

    GivenStep(@Nullable Snapshot snapshot, @Nullable S instance, ObjectMapper mapper) {
        this.snapshot = snapshot;
        this.instance = instance;
        this.mapper = mapper;
    }

    public ThenContractStep<S> whenSerialized() {
        var instance = requireInstance();
        return new ThenContractStep<>(instance, null, mapper);
    }

    public ThenContractStep<S> whenSerialized(Snapshot destination) {
        var instance = requireInstance();
        return new ThenContractStep<>(instance, destination, mapper);
    }

    private S requireInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "whenSerialized() requires an instance — use Contract.given(instance), not Contract.given(Snapshot)");
        }
        return instance;
    }

    public <T> ThenCompatibilityStep<S, T> whenDeserializedAs(Class<T> targetType) {
        return new ThenCompatibilityStep<>(snapshot, instance, targetType, mapper);
    }
}
