package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Strictland is a contract-testing library for the messages your code sends and stores: events,
 * commands, queue messages, HTTP requests and responses, and anything else you serialize for someone
 * else to read.
 *
 * <p>You write a small unit test that locks down a message's format. Later you rename a field, change
 * a type, or adjust how a value serializes; the code still compiles and your other tests pass, but
 * that one fails and points at what changed. You fix it in your build, before a consumer or a stored
 * event has hit the old format in production.
 *
 * <p>When a message changes by accident, a snapshot check ({@link ThenContractStep}) shows you
 * exactly what moved. When you evolve a message on purpose, a compatibility check ({@link
 * ThenCompatibilityStep}) confirms an old and a new version can still read each other's data.
 *
 * <p>Every check starts here and reads as a sentence:
 *
 * {@snippet :
 * MessageContract.specification()
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 * }
 *
 * <p>The approved file each check compares against lives next to your test code and is committed to
 * your repository, so a contract change shows up in the same pull request as the code that caused it.
 * {@link Snapshot} picks which file backs a check, and {@link PublicApiScanner} renders a package's
 * public API as text so you can approval-test the surface itself.
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

    /**
     * Creates a message-contract specification, your starting point for locking down a message's
     * format. Hand it the message with {@code given(...)}, then finish with a {@code then} check.
     *
     * <p>Uses Strictland's default serialization, which fits when your application hasn't customized
     * Jackson; otherwise use {@link #specification(ObjectMapper)} so the test matches the bytes you
     * ship.
     *
     * <p>For reference, the defaults write ISO-8601 dates, keep null fields, and ignore unknown
     * properties on read.
     *
     * @return a specification you attach the message under test to with {@code given(...)}
     */
    public static MessageContract specification() {
        return new MessageContract(DEFAULT_MAPPER);
    }

    /**
     * Creates a message-contract specification bound to your own Jackson mapper, your starting point
     * when the application serializes in a customized way. Hand it the message with {@code
     * given(...)}, then finish with a {@code then} check.
     *
     * <p>Pass the same {@link ObjectMapper} your application uses, so the test checks the exact bytes
     * you ship: snake_case naming, a custom date format, {@code NON_NULL} inclusion, and so on.
     * Against any other serializer you'd be pinning a shape your consumers never see.
     *
     * @param mapper the Jackson mapper your application serializes with
     * @return a specification you attach the message under test to with {@code given(...)}
     */
    public static MessageContract specification(ObjectMapper mapper) {
        return new MessageContract(mapper);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by its
     * type.
     *
     * <p>Use it to check your current code still reads what an older version wrote. {@code
     * Snapshot.of(OrderPlaced.class)} loads the approved file for that type, which you then read as
     * today's type with {@link GivenStep#whenDeserializedAs(Class)}. {@link Snapshot} lists the other
     * ways to locate one.
     *
     * @param snapshot the saved snapshot to read
     * @param <S> the message type the snapshot holds
     * @return the next step, where you choose what to check
     */
    public <S> GivenStep<S> given(Snapshot.ByClass<S> snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by a
     * message-type name.
     *
     * <p>Useful when the saved name is a logical message type rather than a Java class, such as the
     * type your event store records, or when one class has several saved versions. {@code
     * Snapshot.forMessageType("OrderPlaced_V1")} loads that file.
     *
     * @param snapshot the saved snapshot to read
     * @return the next step, where you choose what to check
     */
    public GivenStep<Object> given(Snapshot.ByMessageType snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by its file
     * path.
     *
     * <p>Reach for it when the approved file lives somewhere other than next to your test. {@code
     * Snapshot.at(path)} points straight at it.
     *
     * @param snapshot the saved snapshot to read
     * @return the next step, where you choose what to check
     */
    public GivenStep<Object> given(Snapshot.ByPath snapshot) {
        return new GivenStep<>(snapshot, null, mapper);
    }

    /**
     * Defines the version under contract: the message as your current code builds it.
     *
     * <p>From here, lock its shape so accidental changes get caught with {@link
     * GivenStep#whenSerialized()}, or read it as another version to check the two are compatible with
     * {@link GivenStep#whenDeserializedAs(Class)}.
     *
     * @param instance the message to pin or read with another version
     * @param <S> the type of the message
     * @return the next step, where you choose what to check
     */
    public <S> GivenStep<S> given(S instance) {
        return new GivenStep<>(null, instance, mapper);
    }
}
