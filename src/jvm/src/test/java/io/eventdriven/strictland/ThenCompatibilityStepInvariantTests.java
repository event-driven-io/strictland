package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.UUID;
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

    private record OrderPlaced(UUID orderId, String customer) {}

    private record OrderPlacedWithFailingAccessor(UUID orderId, String customer) {
        @Override
        @SuppressWarnings("UnusedMethod")
        public String customer() {
            throw new IllegalStateException("customer is not loaded");
        }
    }

    @Test
    void whenTheTargetAccessorThrows_thenBackwardCompatible_throwsNamingTheField() {
        var error = assertThrows(
                RuntimeException.class,
                () -> MessageContract.specification(Json.Jackson.defaults())
                        .given(new OrderPlaced(UUID.randomUUID(), "Alice"))
                        .whenDeserializedAs(OrderPlacedWithFailingAccessor.class)
                        .thenBackwardCompatible());

        var message = requireNonNull(error.getMessage());
        assertTrue(message.contains("Cannot read field 'customer' of OrderPlacedWithFailingAccessor"), message);
    }
}
