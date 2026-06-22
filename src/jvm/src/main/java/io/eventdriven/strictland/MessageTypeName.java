package io.eventdriven.strictland;

/**
 * A message type's name split into its namespace and short name. The snapshot machinery names files by
 * the short name and lays out folders by the namespace.
 *
 * <p>The namespace is everything before the last dot, the short name everything after it. A dotless
 * logical name has an empty namespace and is its own short name.</p>
 *
 * @param namespace the part before the last dot, empty when the name has none
 * @param shortName the part after the last dot, the whole name when it has none
 */
public record MessageTypeName(String namespace, String shortName) {

    /**
     * Splits a message type's name on its last dot into a namespace and a short name. A fully-qualified
     * name like {@code com.acme.orders.OrderPlaced} splits into the namespace {@code com.acme.orders}
     * and the short name {@code OrderPlaced}. A dotless logical name keeps an empty namespace and is its
     * own short name.
     *
     * @param messageType the message type's name to split
     * @return the namespace and short name the message type's name carries
     */
    public static MessageTypeName of(String messageType) {
        var dot = messageType.lastIndexOf('.');
        return dot < 0
                ? new MessageTypeName("", messageType)
                : new MessageTypeName(messageType.substring(0, dot), messageType.substring(dot + 1));
    }

    /**
     * Splits a message class's name on its last dot into a namespace and a short name, the same way the
     * default fully-qualified type mapper names a class. It reads the class's canonical name, so a
     * packaged class like {@code com.acme.orders.OrderPlaced} yields the namespace {@code
     * com.acme.orders} and the short name {@code OrderPlaced}, and a nested class {@code Outer.Inner}
     * stays dotted as {@code pkg.Outer.Inner}, never with a {@code $}. A local or anonymous class has no
     * canonical name, so it falls back to the binary name.
     *
     * @param messageClass the message class whose name to split
     * @return the namespace and short name the message class's name carries
     */
    public static MessageTypeName of(Class<?> messageClass) {
        var canonical = messageClass.getCanonicalName();
        return of(canonical != null ? canonical : messageClass.getTypeName());
    }
}
