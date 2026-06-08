package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.scrubbers.Scrubbers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

final class RawApprovalsExampleTests {
    private static final ObjectMapper MAPPER = new JsonMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private static final UUID FIXED_CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);

    record ShoppingCartConfirmed(
            UUID shoppingCartId, @Nullable String clientId, OffsetDateTime confirmedAt) {}

    @Test
    void shoppingCartConfirmed_withCompleteData_isCompatible() throws JsonProcessingException {
        var event = new ShoppingCartConfirmed(FIXED_CART_ID, "anonymised", FIXED_DATE);

        Approvals.verify(MAPPER.writeValueAsString(event));
    }

    @Test
    void shoppingCartConfirmed_withOnlyRequiredData_isCompatible() throws JsonProcessingException {
        var event = new ShoppingCartConfirmed(FIXED_CART_ID, null, FIXED_DATE);

        Approvals.verify(MAPPER.writeValueAsString(event));
    }

    @Test
    void shoppingCartConfirmed_withNonDeterministicId_isCompatible() throws JsonProcessingException {
        var event = new ShoppingCartConfirmed(randomUUID(), "anonymised", FIXED_DATE);

        var options = new Options(Scrubbers.scrubAll(Scrubbers::scrubGuid));

        Approvals.verify(MAPPER.writeValueAsString(event), options);
    }
}
