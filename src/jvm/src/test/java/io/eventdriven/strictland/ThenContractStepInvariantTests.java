package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenContractStepInvariantTests {

    private record OrderPlaced(String orderId) {}

    private static ContractContext context() {
        return new ContractContext(
                new JacksonMessageSerializer(new JsonMapper()),
                new FileSnapshotStorage(SnapshotLayout.registry()),
                MessageTypeMapper.fullyQualifiedName());
    }

    @Test
    void whenTheDestinationIsAMessageInHand_thenContractIsUnchanged_rejectsIt() {
        var step = new ThenContractStep<>(
                new OrderPlaced("1"),
                MessageSnapshot.of(new OrderPlaced("1")),
                MessageSnapshot.DEFAULT_VERSION,
                context());

        assertThrows(IllegalArgumentException.class, step::thenContractIsUnchanged);
    }
}
