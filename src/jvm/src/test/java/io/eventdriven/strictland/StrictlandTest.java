package io.eventdriven.strictland;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrictlandTest {

    @Test
    void greet_returnsExpectedMessage() {
        var strictland = new Strictland();
        assertEquals("Hello from Strictland!", strictland.greet());
    }
}
