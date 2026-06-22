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

    @Test
    void of_aPackagedClass_splitsItsCanonicalNameIntoPackageAndSimpleName() {
        var name = MessageTypeName.of(MessageTypeNameTests.class);

        assertEquals("io.eventdriven.strictland", name.namespace());
        assertEquals("MessageTypeNameTests", name.shortName());
    }

    @Test
    void of_aNestedClass_namesItDottedUnderItsEnclosingClass() {
        var name = MessageTypeName.of(Nested.class);

        assertEquals("io.eventdriven.strictland.MessageTypeNameTests", name.namespace());
        assertEquals("Nested", name.shortName());
    }

    @Test
    void of_aLocalClassWithoutACanonicalName_fallsBackToItsBinaryName() {
        class Local {}

        var name = MessageTypeName.of(Local.class);

        assertEquals("io.eventdriven.strictland", name.namespace());
        assertEquals("MessageTypeNameTests$1Local", name.shortName());
    }

    private static final class Nested {}
}
