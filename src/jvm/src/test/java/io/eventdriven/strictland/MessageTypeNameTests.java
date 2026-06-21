package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class MessageTypeNameTests {

    @Test
    void of_aDottedName_splitsOnTheLastDotIntoNamespaceAndShortName() {
        var name = MessageTypeName.of("com.acme.orders.OrderPlaced");

        assertEquals("com.acme.orders", name.namespace());
        assertEquals("OrderPlaced", name.shortName());
    }

    @Test
    void of_aDotlessName_hasEmptyNamespaceAndIsItsOwnShortName() {
        var name = MessageTypeName.of("OrderPlaced");

        assertEquals("", name.namespace());
        assertEquals("OrderPlaced", name.shortName());
    }
}
