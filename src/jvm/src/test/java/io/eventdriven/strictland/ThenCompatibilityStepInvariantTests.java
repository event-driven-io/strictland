package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenCompatibilityStepInvariantTests {

    @Test
    void whenTheReferencedSnapshotIsAbsent_thenForwardCompatible_throws() {
        var context = new ContractContext(
                new JacksonMessageSerializer(new JsonMapper()),
                new FileSnapshotStorage(SnapshotLayout.registry()),
                MessageTypeMapper.fullyQualifiedName(),
                new SnapshotReviewer(SnapshotReview.off()),
                DiffReview.forReview(SnapshotReview.off()));
        var step = new ThenCompatibilityStep<>(
                MessageSnapshot.ofTypeNamed("NeverWritten"), MessageSnapshot.DEFAULT_VERSION, Object.class, context);

        assertThrows(RuntimeException.class, step::thenForwardCompatible);
    }
}
