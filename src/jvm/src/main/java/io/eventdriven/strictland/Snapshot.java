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
 * #forMessageType(String)}, by file path with {@link #at(Path)}, or by a variant label with {@link
 * #variant(String)} when one message type has several snapshots. Pass the result to a {@link
 * MessageContract#given(Snapshot.ByClass) given(...)} step to read an earlier version's data, or to
 * {@link GivenStep#whenSerialized(Snapshot)} to choose which snapshot a serialization check compares
 * against.</p>
 */
public sealed interface Snapshot permits Snapshot.ByClass, Snapshot.ByMessageType, Snapshot.ByPath, Snapshot.ByVariant {

    /**
     * A snapshot found by its message class, named after the class's simple name.
     *
     * @param <T> the message type the snapshot holds
     * @param messageClass the class whose name identifies the snapshot
     */
    record ByClass<T>(Class<T> messageClass) implements Snapshot {}

    /**
     * A snapshot found by a message-type name, for when it's named after a logical type - the one your
     * event store or message bus records - rather than a Java class.
     *
     * @param messageType the name identifying the snapshot
     */
    record ByMessageType(String messageType) implements Snapshot {}

    /**
     * A snapshot found by an explicit file path, for one kept somewhere other than next to your test.
     *
     * @param path the file to read the snapshot from
     */
    record ByPath(Path path) implements Snapshot {}

    /**
     * A snapshot distinguished by a variant label, for when one message type has several approved
     * snapshots that must sit side by side without overwriting each other. The label becomes the leaf
     * file name, so it reads as documentation of what the variant captures.
     *
     * @param label the label naming the variant, used as the snapshot's leaf file name
     * @param messageClass the class the snapshot's data represents, or {@code null} if unspecified
     */
    record ByVariant(String label, @Nullable Class<?> messageClass) implements Snapshot {}

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
        return new ByClass<>(sourceType);
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
        return new ByMessageType(messageType);
    }

    /**
     * Points at a snapshot by a variant label, so one message type can carry several approved snapshots
     * that sit beside each other instead of overwriting one file. The label becomes the leaf file name
     * and is recorded as documentation of what the variant captures, for instance a null promotion or
     * an epoch date.
     *
     * <p>Pass it to {@link GivenStep#whenSerialized(Snapshot)} to pin one variant of the message you
     * gave the spec.</p>
     *
     * <pre>
     * MessageContract.specification(options)
     *     .given(new OrderInitiated(ORDER_ID, "Alice", "NONE"))
     *     .whenSerialized(Snapshot.variant("NoPromotion"))
     *     .thenContractIsUnchanged();
     * </pre>
     *
     * @param label the label naming the variant, used as the snapshot's leaf file name
     * @return the snapshot, ready for a {@code whenSerialized(...)} step
     */
    static ByVariant variant(String label) {
        return new ByVariant(label, null);
    }

    /**
     * Points at a snapshot by a variant label and records the class its data represents, so a {@code
     * given(...)} step can read that one variant back by its label.
     *
     * <pre>
     * MessageContract.specification(options)
     *     .given(Snapshot.variant("WithPromotion", OrderInitiated.class))
     *     .whenDeserializedAs(OrderInitiated.class)
     *     .thenBackwardCompatible(order -> assertEquals("WELCOME", order.promotion()));
     * </pre>
     *
     * @param label the label naming the variant, used as the snapshot's leaf file name
     * @param sourceType the class the snapshot's data represents
     * @param <T> the message type the snapshot holds
     * @return the snapshot, ready for a {@code given(...)} or {@code whenSerialized(...)} step
     */
    static <T> ByVariant variant(String label, Class<T> sourceType) {
        return new ByVariant(label, sourceType);
    }
}
