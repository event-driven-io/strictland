package io.eventdriven.strictland;

/**
 * Turns a message into bytes and back, the one thing the core knows about a serialization format.
 * Supply your own when your application doesn't serialize with Jackson - a Protobuf, Avro, or CSV
 * encoding - and the contract checks run against the exact bytes you ship.
 *
 * <p>Hand an implementation to {@link SpecificationOptions#serializer(MessageSerializer)}; for JSON,
 * reach for {@link Json.Jackson} instead of writing one.
 *
 * {@snippet :
 * MessageContract.specification(SpecificationOptions.serializer(new CsvMessageSerializer()))
 *     .given(new MemberJoined(memberId, "Alice"))
 *     .whenDeserializedAs(MemberJoined.class)
 *     .thenBackwardCompatible();
 * }
 */
public interface MessageSerializer {

    /**
     * Serializes a message to the bytes your application writes, so a contract check pins the shape
     * consumers actually see.
     *
     * @param value the message to serialize
     * @return the serialized bytes
     */
    byte[] serialize(Object value);

    /**
     * Reads bytes back into a message of the given type, so a compatibility check can confirm one
     * version still reads another's data.
     *
     * @param bytes the serialized message to read
     * @param type the type to read the bytes as
     * @param <T> the message type to produce
     * @return the deserialized message
     */
    <T> T deserialize(byte[] bytes, Class<T> type);
}
