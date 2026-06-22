package io.eventdriven.strictland;

import java.util.Optional;

/**
 * Maps between a message class and the logical name a snapshot is stored under. Supply your own when
 * the stored name is a versioned message type your event store records - {@code OrderPlaced_V1} - and
 * not the Java class name, so checks find the right approved file.
 *
 * <p>Hand an implementation to {@link SpecificationOptions#messageTypeMapper(MessageTypeMapper)}; the
 * default {@link #fullyQualifiedName()} uses the class's fully-qualified name.</p>
 *
 * <pre>
 * MessageTypeMapper byClassName = MessageTypeMapper.fullyQualifiedName();
 * MessageTypeName name = byClassName.name(OrderPlaced.class); // namespace com.acme.orders, short name OrderPlaced
 * </pre>
 */
public interface MessageTypeMapper {

    /**
     * Returns the message type name a message of the given type is stored under, split into its
     * namespace and short name, so a check can locate its approved file.
     *
     * @param type the message class to name
     * @return the message type name for that type
     */
    MessageTypeName name(Class<?> type);

    /**
     * Resolves a logical name back to the message class it maps to, the reverse of {@link
     * #name(Class)}. It can only answer for names this mapper was given an explicit mapping for, so a
     * name-derived default like {@link #fullyQualifiedName()} returns {@link Optional#empty()}.
     *
     * @param name the logical name to resolve
     * @return the message class registered for that name, or {@link Optional#empty()} when none is
     */
    Optional<Class<?>> type(String name);

    /**
     * A mapper that names a message after its class's fully-qualified canonical name, the default
     * Strictland uses when you haven't registered logical message types. It maps a class to a name but,
     * having no registry to reverse, resolves no name back to a class.
     *
     * <p>A nested type {@code Outer.Inner} is named {@code pkg.Outer.Inner}: dotted, never with a
     * {@code $}. It falls back to the binary name only for local or anonymous classes that have no
     * canonical name.</p>
     *
     * <pre>
     * MessageTypeMapper mapper = MessageTypeMapper.fullyQualifiedName();
     * MessageTypeName name = mapper.name(OrderPlaced.class); // namespace com.acme.orders, short name OrderPlaced
     * </pre>
     *
     * @return a mapper keyed on the class's fully-qualified canonical name
     */
    static MessageTypeMapper fullyQualifiedName() {
        return new MessageTypeMapper() {
            @Override
            public MessageTypeName name(Class<?> type) {
                return MessageTypeName.of(type);
            }

            @Override
            public Optional<Class<?>> type(String name) {
                return Optional.empty();
            }
        };
    }
}
