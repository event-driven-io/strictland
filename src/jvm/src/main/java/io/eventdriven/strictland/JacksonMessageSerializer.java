package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;

record JacksonMessageSerializer(ObjectMapper mapper) implements MessageSerializer {
    @Override
    public byte[] serialize(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        try {
            return mapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
