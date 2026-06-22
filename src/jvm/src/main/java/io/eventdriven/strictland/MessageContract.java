package io.eventdriven.strictland;

/**
 * Strictland is a contract-testing library for the messages your code sends and stores: events,
 * commands, queue messages, HTTP requests and responses, and anything else you serialize for someone
 * else to read.
 *
 * <p>You write a small unit test that locks down a message's format. Later you rename a field, change
 * a type, or adjust how a value serializes; the code still compiles and your other tests pass, but
 * that one fails and points at what changed. You fix it in your build, before a consumer or a stored
 * event has hit the old format in production.</p>
 *
 * <p>When a message changes by accident, a snapshot check ({@link ThenContractStep}) shows you
 * exactly what moved. When you evolve a message on purpose, a compatibility check ({@link
 * ThenCompatibilityStep}) confirms an old and a new version can still read each other's data.</p>
 *
 * <p>Every check starts here and reads as a sentence:</p>
 *
 * <pre>
 * MessageContract.specification(Json.Jackson.defaults())
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 * </pre>
 *
 * <p>The approved file each check compares against lives in a committed contract registry, under
 * {@code src/test/resources/contract-registry} by default, so a contract change shows up in the same
 * pull request as the code that caused it.
 * {@link MessageSnapshot} picks which file backs a check, and {@link PublicApiScanner} renders a
 * package's public API as text so you can approval-test the surface itself.</p>
 */
public class MessageContract {
    private final ContractContext context;

    private MessageContract(SpecificationOptions options) {
        var serializer = options.serializer();
        var configuredStorage = options.storage();
        var storage = configuredStorage != null
                ? configuredStorage
                : new FileSnapshotStorage(SnapshotLayoutResolver.resolve(options.snapshotLayout()));
        var configuredTypeMapper = options.typeMapper();
        var typeMapper = configuredTypeMapper != null ? configuredTypeMapper : MessageTypeMapper.fullyQualifiedName();
        this.context = new ContractContext(serializer, storage, typeMapper);
    }

    /**
     * Creates a message-contract specification, your starting point for locking down a message's
     * format. Hand it the message with {@code given(...)}, then finish with a {@code then} check.
     *
     * <p>Through the options you define how a message is serialized: its format and the serializer's
     * configuration. Using the serializer your application already uses keeps the contract aligned
     * with the bytes you actually ship. If you're using JSON, you can plug in your own Jackson mapper
     * with {@link Json.Jackson#of(com.fasterxml.jackson.databind.ObjectMapper) Json.Jackson.of(mapper)},
     * or use Strictland's default with {@link Json.Jackson#defaults()}. For another format, implement
     * {@link MessageSerializer} and pass it through {@link SpecificationOptions}.</p>
     *
     * <pre>
     * MessageContract.specification(Json.Jackson.defaults())
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * </pre>
     *
     * @param options how a message is serialized, where its snapshot lives, and how its type is named
     * @return a specification you attach the message under test to with {@code given(...)}
     */
    public static MessageContract specification(SpecificationOptions options) {
        return new MessageContract(options);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by its
     * type.
     *
     * <p>Use it to check your current code still reads what an older version wrote. {@code
     * MessageSnapshot.of(OrderPlaced.class)} loads the approved file for that type, which you then read
     * as today's type with {@link GivenStep#whenDeserializedAs(Class)}. {@link MessageSnapshot} lists the
     * other ways to locate one.</p>
     *
     * @param snapshot the saved snapshot to read
     * @param <S> the message type the snapshot holds
     * @return the next step, where you choose what to check
     */
    public <S> GivenStep<S> given(MessageSnapshot.ByClass<S> snapshot) {
        return new GivenStep<>(snapshot, snapshot.version(), context);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by a
     * message-type name.
     *
     * <p>Useful when the saved name is a logical message type rather than a Java class, such as the
     * type your event store records, or when one class has several saved versions. {@code
     * MessageSnapshot.forMessageType("OrderPlaced_V1")} loads that file.</p>
     *
     * @param snapshot the saved snapshot to read
     * @return the next step, where you choose what to check
     */
    public GivenStep<Object> given(MessageSnapshot.ByMessageType snapshot) {
        return new GivenStep<>(snapshot, snapshot.version(), context);
    }

    /**
     * Defines the version under contract: an earlier one you saved as a snapshot, located by its file
     * path.
     *
     * <p>Reach for it when the approved file lives somewhere other than the default contract registry. {@code
     * MessageSnapshot.at(path)} points straight at it.</p>
     *
     * @param snapshot the saved snapshot to read
     * @return the next step, where you choose what to check
     */
    public GivenStep<Object> given(MessageSnapshot.ByPath snapshot) {
        return new GivenStep<>(snapshot, MessageSnapshot.DEFAULT_VERSION, context);
    }

    /**
     * Defines the version under contract: the message as your current code builds it.
     *
     * <p>From here, lock its shape so accidental changes get caught with {@link
     * GivenStep#whenSerialized()}, or read it as another version to check the two are compatible with
     * {@link GivenStep#whenDeserializedAs(Class)}.</p>
     *
     * @param instance the message to pin or read with another version
     * @param <S> the type of the message
     * @return the next step, where you choose what to check
     */
    public <S> GivenStep<S> given(S instance) {
        return new GivenStep<>(MessageSnapshot.of(instance), MessageSnapshot.DEFAULT_VERSION, context);
    }

    /**
     * Defines the version under contract: the message as your current code builds it, pinned to a
     * snapshot version you name.
     *
     * <p>Like {@link #given(Object)}, but the snapshot's name carries the {@code version} you give
     * rather than the default, so several versions of one message type keep distinct snapshots. A {@code
     * whenSerializedAs(...)} snapshot that pins its own version overrides this one.</p>
     *
     * @param instance the message to pin or read with another version
     * @param version the snapshot version the check pins to
     * @param <S> the type of the message
     * @return the next step, where you choose what to check
     */
    public <S> GivenStep<S> given(S instance, String version) {
        return new GivenStep<>(MessageSnapshot.of(instance), version, context);
    }
}
