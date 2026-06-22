package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotFilterTests {

    @Test
    void of_aDottedType_pathIsTheNamespacePartsThenTheShortName() {
        var filter = SnapshotFilter.of(MessageTypeName.of("com.acme.orders.OrderPlaced"), "1");

        assertEquals(List.of("com", "acme", "orders", "OrderPlaced"), filter.path());
    }

    @Test
    void of_aDottedType_namePrefixIsShortNameDotVersionDot() {
        var filter = SnapshotFilter.of(MessageTypeName.of("com.acme.orders.OrderPlaced"), "2");

        assertEquals("OrderPlaced.2.", filter.namePrefix());
    }

    @Test
    void of_aDotlessType_pathIsJustTheShortName() {
        var filter = SnapshotFilter.of(MessageTypeName.of("OrderPlaced"), "1");

        assertEquals(List.of("OrderPlaced"), filter.path());
    }

    @Test
    void of_aDotlessType_namePrefixIsShortNameDotVersionDot() {
        var filter = SnapshotFilter.of(MessageTypeName.of("OrderPlaced"), "1");

        assertEquals("OrderPlaced.1.", filter.namePrefix());
    }
}
