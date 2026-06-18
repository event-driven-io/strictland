package io.eventdriven.strictland;

/**
 * Converts a message to the bytes your application writes, and reads them back. Strictland provides a
 * Jackson serializer for JSON out of the box; for another format you implement this interface, as the
 * CSV and simple-binary serializers in the test suite show. The contract checks then run against the
 * exact bytes you ship.
 *
 * <p>Pass your application's serializer to {@link SpecificationOptions#serializer(MessageSerializer)}.
 * If you're using JSON then you can use the built-in {@link Json.Jackson#of} instead of writing your
 * own.</p>
 *
 * <pre>
 * MessageContract.specification(SpecificationOptions.serializer(new CsvMessageSerializer()))
 *     .given(new MemberJoined(memberId, "Alice"))
 *     .whenDeserializedAs(MemberJoined.class)
 *     .thenBackwardCompatible();
 * </pre>
 */
public interface MessageSerializer {

    /**
     * Serializes a message to the bytes your application writes. A contract check compares those bytes
     * against the approved snapshot.
     *
     * @param value the message to serialize
     * @return the serialized bytes
     */
    byte[] serialize(Object value);

    /**
     * Reads bytes back into a message of the given type. A compatibility check uses it to confirm one
     * version still reads another's data.
     *
     * @param bytes the serialized message to read
     * @param type the type to read the bytes as
     * @param <T> the message type to produce
     * @return the deserialized message
     */
    <T> T deserialize(byte[] bytes, Class<T> type);

    /**
     * The extension Strictland gives the snapshot file your bytes are written to, so a JSON snapshot
     * lands as {@code OrderPlaced.json} rather than a generic {@code .txt}. Override it to match your
     * format and your approved files read at a glance. The leading dot is part of the contract: return
     * {@code ".json"}, not {@code "json"}.
     *
     * <pre>
     * assertEquals(".csv", new CsvMessageSerializer().fileExtension());
     * </pre>
     *
     * @return the snapshot file extension, including the leading dot
     */
    default String fileExtension() {
        return ".txt";
    }
}
