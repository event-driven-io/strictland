package io.eventdriven.strictland;

/**
 * A message type's name split into its namespace and short name. The snapshot machinery names files by
 * the short name and lays out folders by the namespace, so splitting once here keeps that math in one
 * place.
 *
 * <p>The namespace is everything before the last dot, the short name everything after it. A dotless
 * logical name has an empty namespace and is its own short name.</p>
 *
 * @param namespace the part before the last dot, empty when the name has none
 * @param shortName the part after the last dot, the whole name when it has none
 */
record MessageTypeName(String namespace, String shortName) {

    /**
     * Splits a message type's name on its last dot into a namespace and a short name. A fully-qualified
     * name like {@code com.acme.orders.OrderPlaced} splits into the namespace {@code com.acme.orders}
     * and the short name {@code OrderPlaced}. A dotless logical name keeps an empty namespace and is its
     * own short name.
     *
     * @param messageType the message type's name to split
     * @return the namespace and short name the message type's name carries
     */
    static MessageTypeName of(String messageType) {
        var dot = messageType.lastIndexOf('.');
        return dot < 0
                ? new MessageTypeName("", messageType)
                : new MessageTypeName(messageType.substring(0, dot), messageType.substring(dot + 1));
    }
}
