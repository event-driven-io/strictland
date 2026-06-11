package io.eventdriven.strictland;

interface MessageSerializer {
    byte[] serialize(Object value);

    <T> T deserialize(byte[] bytes, Class<T> type);
}
