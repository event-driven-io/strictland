package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Entry point for the contract DSL used to verify event schema versioning and serialization compatibility.
 */
public class MessageContract {
    private static final ObjectMapper DEFAULT_MAPPER = new JsonMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final ObjectMapper mapper;

    private MessageContract(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static MessageContract specification() {
        return new MessageContract(DEFAULT_MAPPER);
    }

    public static MessageContract specification(ObjectMapper mapper) {
        return new MessageContract(mapper);
    }

    public <S> GivenStep<S> given(Snapshot.ByClass<S> snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    public GivenStep<Object> given(Snapshot.ByMessageType snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    public GivenStep<Object> given(Snapshot.ByPath snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    public <S> GivenStep<S> given(S instance) {
        return new GivenStep<>(null, instance, mapper);
    }
}
