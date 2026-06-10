package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * A snapshot is what your message looks like once serialized - the JSON you reviewed and approved.
 * Strictland captures it the first time from your message, then checks every later run still produces
 * the same thing, so an accidental change to the format fails the test.
 *
 * <p>Every check already uses a default snapshot, named after the message and kept next to your test.
 * You reach for this type only to point at a different one: the factory methods give you the three
 * ways to pick one - by its message class with {@link #of(Class)}, by a message-type name with {@link
 * #forMessageType(String)}, or by file path with {@link #at(Path)}. Pass the result to a {@link
 * MessageContract#given(Snapshot.ByClass) given(...)} step to read an earlier version's data, or to
 * {@link GivenStep#whenSerialized(Snapshot)} to choose which snapshot a serialization check compares
 * against.
 */
public sealed interface Snapshot permits Snapshot.ByClass, Snapshot.ByMessageType, Snapshot.ByPath {

    /**
     * A snapshot found by its message class, named after the class's simple name.
     *
     * @param <T> the message type the snapshot holds
     * @param sourceType the class whose name identifies the snapshot
     */
    record ByClass<T>(Class<T> sourceType) implements Snapshot {}

    /**
     * A snapshot found by a message-type name, for when it's named after a logical type - the one your
     * event store or message bus records - rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @param sourceType the class the snapshot's data represents, or {@code null} if unspecified
     */
    record ByMessageType(String messageType, @Nullable Class<?> sourceType) implements Snapshot {}

    /**
     * A snapshot found by an explicit file path, for one kept somewhere other than next to your test.
     *
     * @param path the file to read the snapshot from
     * @param sourceType the class the snapshot's data represents, or {@code null} if unspecified
     */
    record ByPath(Path path, @Nullable Class<?> sourceType) implements Snapshot {}

    /**
     * Points at a snapshot by its message class, named after the class's simple name.
     *
     * <p>Use it to check your current code still reads what an older version wrote: pass the result to
     * {@link MessageContract#given(Snapshot.ByClass)}, then read it as today's type with {@link
     * GivenStep#whenDeserializedAs(Class)}.
     *
     * @param sourceType the class whose name identifies the snapshot
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} step
     */
    static <T> ByClass<T> of(Class<T> sourceType) {
        return new ByClass<>(sourceType);
    }

    /**
     * Points at a snapshot by file path, for one kept somewhere other than next to your test.
     *
     * @param path the file to read the snapshot from
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByPath at(Path path) {
        return new ByPath(path, null);
    }

    /**
     * Points at a snapshot by file path and records the class its data represents, so a {@code
     * given(...)} step knows what to read it as.
     *
     * @param path the file to read the snapshot from
     * @param sourceType the class the snapshot's data represents
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static <T> ByPath at(Path path, Class<T> sourceType) {
        return new ByPath(path, sourceType);
    }

    /**
     * Points at a snapshot by a message-type name, for when it's named after a logical type - the one
     * your event store or message bus records - rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByMessageType forMessageType(String messageType) {
        return new ByMessageType(messageType, null);
    }

    /**
     * Points at a snapshot by a message-type name and records the class its data represents, so a
     * {@code given(...)} step knows what to read it as.
     *
     * @param messageType the name identifying the snapshot
     * @param sourceType the class the snapshot's data represents
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static <T> ByMessageType forMessageType(String messageType, Class<T> sourceType) {
        return new ByMessageType(messageType, sourceType);
    }
}
