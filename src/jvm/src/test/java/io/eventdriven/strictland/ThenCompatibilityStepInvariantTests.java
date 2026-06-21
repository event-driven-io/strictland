package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenCompatibilityStepInvariantTests {

    @Test
    void whenBothSnapshotAndInstanceAreNull_thenForwardCompatible_throwsIllegalState() {
        var location = new SnapshotLocation(SnapshotLayout.registry(), ".json");
        var step = new ThenCompatibilityStep<Object, Object>(
                null,
                null,
                Snapshot.DEFAULT_VERSION,
                Object.class,
                new JacksonMessageSerializer(new JsonMapper()),
                new FileSnapshotStorage(),
                location,
                MessageTypeMapper.fullyQualifiedName());
        assertThrows(IllegalStateException.class, step::thenForwardCompatible);
    }
}
