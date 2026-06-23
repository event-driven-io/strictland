package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * A message snapshot is what your message looks like once serialized: the JSON you reviewed and
 * approved. Strictland captures it the first time from your message, then checks every later run still
 * produces the same thing. An accidental change to the format fails the test.
 *
 * <p>Every check already uses a default snapshot, named after the message and kept in the contract
 * registry. You reach for this type only to point at a different one, or to hand the check a live
 * message. The factory methods give you the ways to pick one: a message in hand with {@link
 * #of(Object)}, by its message class with {@link #of(Class)}, by a message-type name with {@link
 * #forMessageType(String)}, by file path with {@link #at(Path)}, or by a variant label when one message
 * type has several snapshots. Pass the result to a {@link MessageContract#given(MessageSnapshot.ByClass)
 * given(...)} step to read an earlier version's data, or to {@link
 * GivenStep#whenSerializedAs(MessageSnapshot)} to choose which snapshot a serialization check compares
 * against.</p>
 */
public sealed interface MessageSnapshot
        permits MessageSnapshot.ByClass,
                MessageSnapshot.ByMessageType,
                MessageSnapshot.ByPath,
                MessageSnapshot.OfInstance {

    /** The version a snapshot carries when you don't pin one. */
    String DEFAULT_VERSION = "1";

    /**
     * A snapshot found by its message class, named after the class's simple name.
     *
     * @param <T> the message type the snapshot holds
     * @param messageClass the class whose name identifies the snapshot
     * @param version the version label the snapshot is pinned to, defaulting to {@value #DEFAULT_VERSION}
     * @param variant the variant of the message type data that will be snapshotted, defaulting to {@link
     *     SnapshotVariant#UNSET} for an unpinned family read
     */
    record ByClass<T>(Class<T> messageClass, String version, SnapshotVariant variant) implements MessageSnapshot {
        /**
         * Points at a snapshot's variant for the {@link #messageClass()}, naming it by a label.
         * @param variant the label naming the variant, used as the trailing segment of the snapshot's file name
         * @return snapshot variant for specific {@link #messageClass()}
         */
        public ByClass<T> variant(String variant) {
            return new ByClass<>(messageClass, version, SnapshotVariant.named(variant));
        }

        /**
         * Points at a snapshot's variant for the {@link #messageClass()}, given the variant directly.
         * @param variant the variant naming this snapshot
         * @return snapshot variant for specific {@link #messageClass()}
         */
        public ByClass<T> variant(SnapshotVariant variant) {
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
     * A snapshot found by a message-type name, for when it's named after a logical type, the one your
     * event store or message bus records, rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @param version the version label the snapshot is pinned to, defaulting to {@value #DEFAULT_VERSION}
     * @param variant the variant of the message type data that will be snapshotted, defaulting to {@link
     *     SnapshotVariant#UNSET} for an unpinned family read
     */
    record ByMessageType(String messageType, String version, SnapshotVariant variant) implements MessageSnapshot {
        /**
         * Points at a snapshot's variant for the {@link #messageType()}, naming it by a label.
         * @param variant the label naming the variant, used as the trailing segment of the snapshot's file name
         * @return snapshot variant for specific {@link #messageType()}
         */
        public ByMessageType variant(String variant) {
            return new ByMessageType(messageType, version, SnapshotVariant.named(variant));
        }

        /**
         * Points at a snapshot's variant for the {@link #messageType()}, given the variant directly.
         * @param variant the variant naming this snapshot
         * @return snapshot variant for specific {@link #messageType()}
         */
        public ByMessageType variant(SnapshotVariant variant) {
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
     * A snapshot found by an explicit file path, for one kept somewhere other than the default contract snapshots location.
     *
     * @param path the file to read the snapshot from
     */
    record ByPath(Path path) implements MessageSnapshot {}

    /**
     * A message your current code holds in hand, serialized at check time rather than read from the
     * registry. It is a valid {@code given(...)} source, since the bytes come straight from the message
     * you built, but it is not a snapshot destination: {@code whenSerializedAs(...)} compares against a
     * stored file, so it never accepts an in-hand message.
     *
     * @param <S> the type of the message under test
     * @param message the message your current code builds
     */
    record OfInstance<S>(S message) implements MessageSnapshot {}

    /**
     * Points at a snapshot by its message class, named after the class's simple name.
     *
     * <p>Use it to check your current code still reads what an older version wrote: pass the result to
     * {@link MessageContract#given(MessageSnapshot.ByClass)}, then read it as today's type with {@link
     * GivenStep#whenDeserializedAs(Class)}.</p>
     *
     * @param sourceType the class whose name identifies the snapshot
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} step
     */
    static <T> ByClass<T> of(Class<T> sourceType) {
        return new ByClass<>(sourceType, DEFAULT_VERSION, SnapshotVariant.UNSET);
    }

    /**
     * Wraps a message your current code holds in hand, for a check that serializes it at check time
     * rather than reading a saved file. {@link MessageContract#given(Object)} resolves through this
     * method; a {@link Class} argument resolves to the more specific {@link #of(Class)} instead, leaving a
     * class reference pointing at a stored snapshot.
     *
     * @param message the message your current code builds
     * @param <S> the type of the message
     * @return the in-hand source, ready for a {@code given(...)} step
     */
    static <S> OfInstance<S> of(S message) {
        return new OfInstance<>(message);
    }

    /**
     * Points at a snapshot by file path, for one kept somewhere other than the default contract snapshots location.
     *
     * @param path the file to read the snapshot from
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByPath at(Path path) {
        return new ByPath(path);
    }

    /**
     * Points at a snapshot by a message-type name, for when it's named after a logical type, the one
     * your event store or message bus records, rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static ByMessageType forMessageType(String messageType) {
        return new ByMessageType(messageType, DEFAULT_VERSION, SnapshotVariant.UNSET);
    }
}
