package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class StrictlandTest {

    @Test
    void greet_returnsExpectedMessage() {
        var strictland = new Strictland();
        assertEquals("Hello from Strictland!", strictland.greet());
    }
}
