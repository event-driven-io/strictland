package io.eventdriven.strictland;

import java.util.Optional;

/**
 * Maps between a message class and the logical name a snapshot is stored under. Supply your own when
 * the stored name is a versioned message type your event store records - {@code OrderPlaced_V1} - and
 * not the Java class name, so checks find the right approved file.
 *
 * <p>Hand an implementation to {@link SpecificationOptions#messageTypeMapper(MessageTypeMapper)}; the
 * default {@link #simpleName()} uses the class's simple name.
 *
 * {@snippet :
 * MessageTypeMapper byClassName = MessageTypeMapper.simpleName();
 * String name = byClassName.name(OrderPlaced.class); // "OrderPlaced"
 * }
 */
public interface MessageTypeMapper {

    /**
     * Returns the logical name a message of the given type is stored under, so a check can locate its
     * approved file.
     *
     * @param type the message class to name
     * @return the logical name for that type
     */
    String name(Class<?> type);

    /**
     * Resolves a logical name back to the message class it maps to, the reverse of {@link
     * #name(Class)}. It can only answer for names this mapper was given an explicit mapping for, so a
     * name-derived default like {@link #simpleName()} returns {@link Optional#empty()}.
     *
     * @param name the logical name to resolve
     * @return the message class registered for that name, or {@link Optional#empty()} when none is
     */
    Optional<Class<?>> type(String name);

    /**
     * A mapper that names a message after its class's simple name, the default Strictland uses when
     * you haven't registered logical message types. It maps a class to a name but, having no registry
     * to reverse, resolves no name back to a class.
     *
     * {@snippet :
     * MessageTypeMapper mapper = MessageTypeMapper.simpleName();
     * String name = mapper.name(OrderPlaced.class); // "OrderPlaced"
     * }
     *
     * @return a mapper keyed on the class's simple name
     */
    static MessageTypeMapper simpleName() {
        return new MessageTypeMapper() {
            @Override
            public String name(Class<?> type) {
                return type.getSimpleName();
            }

            @Override
            public Optional<Class<?>> type(String name) {
                return Optional.empty();
            }
        };
    }
}
