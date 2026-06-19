package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenCompatibilityStepInvariantTests {

    @Test
    void whenBothSnapshotAndInstanceAreNull_thenForwardCompatible_throwsIllegalState() {
        var location = new SnapshotLocation(
                SnapshotLayout.nextToTest(),
                ".json",
                TestClassNaming.SIMPLE,
                (packageName, sourceFileName) -> Path.of("ignored"));
        var step = new ThenCompatibilityStep<Object, Object>(
                null,
                null,
                Snapshot.DEFAULT_VERSION,
                Object.class,
                new JacksonMessageSerializer(new JsonMapper()),
                new FileSnapshotStorage(),
                location,
                MessageTypeMapper.simpleName());
        assertThrows(IllegalStateException.class, step::thenForwardCompatible);
    }
}
