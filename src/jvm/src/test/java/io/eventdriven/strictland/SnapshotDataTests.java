package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotDataTests {

    @Test
    void bytes_returnsThePayloadItWasBuiltFrom() {
        var payload = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);

        var data = new SnapshotData(payload);

        assertArrayEquals(payload, data.bytes());
    }

    @Test
    void bytes_holdsTheSameArrayReference() {
        var payload = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);

        var data = new SnapshotData(payload);

        assertSame(payload, data.bytes());
    }
}
