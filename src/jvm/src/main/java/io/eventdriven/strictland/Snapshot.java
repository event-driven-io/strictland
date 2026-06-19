package io.eventdriven.strictland;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * A snapshot is what your message looks like once serialized - the JSON you reviewed and approved.
 * Strictland captures it the first time from your message, then checks every later run still produces
 * the same thing, so an accidental change to the format fails the test.
 *
 * <p>Every check already uses a default snapshot, named after the message and kept next to your test.
 * You reach for this type only to point at a different one: the factory methods give you the ways to
 * pick one - by its message class with {@link #of(Class)}, by a message-type name with {@link
 * #forMessageType(String)}, by file path with {@link #at(Path)}, or by a variant label when one message type has several snapshots.
 * Pass the result to a {@link
 * MessageContract#given(Snapshot.ByClass) given(...)} step to read an earlier version's data, or to
 * {@link GivenStep#whenSerializedAs(Snapshot)} to choose which snapshot a serialization check compares
 * against.</p>
 */
public sealed interface Snapshot permits Snapshot.ByClass, Snapshot.ByMessageType, Snapshot.ByPath {

    /** The version a snapshot carries when you don't pin one. */
    String DEFAULT_VERSION = "1";

    /**
     * A snapshot found by its message class, named after the class's simple name.
     *
     * @param <T> the message type the snapshot holds
     * @param messageClass the class whose name identifies the snapshot
     * @param version the version label the snapshot is pinned to, defaulting to {@value #DEFAULT_VERSION}
     * @param variant the label naming the variant of the message type data that will be snapshotted
     */
    record ByClass<T>(
            Class<T> messageClass, String version, @Nullable String variant) implements Snapshot {
        /**
         * Points at a snapthot's variant and records for the a {@link #messageClass()}.
         * @param variant the label naming the variant, used as the snapshot's leaf file name
         * @return snapshot variant for specific {@link #messageClass()}
         */
        public ByClass<T> variant(String variant) {
            return new ByClass<>(messageClass, version, variant);
        }

        /**
         * Pins the snapshot to a message type version.
         * @param version the version label the snapshot is pinned to
         * @return snapshot pinned to {@code version} for the same {@link #messageClass()}
         */
        public ByClass<T> version(String version) {
            return new ByClass<>(messageClass, version, variant);
        }
    }

    /**
     * A snapshot found by a message-type name, for when it's named after a logical type - the one your
     * event store or message bus records - rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @param version the version label the snapshot is pinned to, defaulting to {@value #DEFAULT_VERSION}
     * @param variant the label naming the variant of the message type data that will be snapshotted
     */
    record ByMessageType(
            String messageType, String version, @Nullable String variant) implements Snapshot {
        /**
         * Points at a snapthot's variant and records for the a {@link #messageType()}.
         * @param variant the label naming the variant, used as the snapshot's leaf file name
         * @return snapshot variant for specific {@link #messageType()}
         */
        public ByMessageType variant(String variant) {
            return new ByMessageType(messageType, version, variant);
        }

        /**
         * Pins the snapshot to a version, the segment that lets several versions of one message type
         * keep distinct snapshots.
         * @param version the version label the snapshot is pinned to
         * @return snapshot pinned to {@code version} for the same {@link #messageType()}
         */
        public ByMessageType version(String version) {
            return new ByMessageType(messageType, version, variant);
        }
    }

    /**
     * A snapshot found by an explicit file path, for one kept somewhere other than next to your test.
     *
     * @param path the file to read the snapshot from
     */
    record ByPath(Path path) implements Snapshot {}

    /**
     * Points at a snapshot by its message class, named after the class's simple name.
     *
     * <p>Use it to check your current code still reads what an older version wrote: pass the result to
     * {@link MessageContract#given(Snapshot.ByClass)}, then read it as today's type with {@link
     * GivenStep#whenDeserializedAs(Class)}.</p>
     *
     * @param sourceType the class whose name identifies the snapshot
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} step
     */
    static <T> ByClass<T> of(Class<T> sourceType) {
        return new ByClass<>(sourceType, DEFAULT_VERSION, null);
    }

    /**
     * Points at a snapshot by file path, for one kept somewhere other than next to your test.
     *
     * @param path the file to read the snapshot from
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByPath at(Path path) {
        return new ByPath(path);
    }

    /**
     * Points at a snapshot by a message-type name, for when it's named after a logical type - the one
     * your event store or message bus records - rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByMessageType forMessageType(String messageType) {
        return new ByMessageType(messageType, DEFAULT_VERSION, null);
    }
}
