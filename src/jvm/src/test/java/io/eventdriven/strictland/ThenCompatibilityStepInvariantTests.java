package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenCompatibilityStepInvariantTests {

    @Test
    void whenBothSnapshotAndInstanceAreNull_thenForwardCompatible_throwsIllegalState() {
        var step = new ThenCompatibilityStep<Object, Object>(null, null, Object.class, new JsonMapper());
        assertThrows(IllegalStateException.class, step::thenForwardCompatible);
    }
}
