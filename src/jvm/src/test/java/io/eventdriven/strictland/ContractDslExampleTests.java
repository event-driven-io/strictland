package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

final class ContractDslExampleTests {
    private static final UUID FIXED_CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);
    private static final Contract CONTRACT = Contract.specification();

    record ShoppingCartConfirmedV1(
            UUID shoppingCartId, @Nullable String clientId, OffsetDateTime confirmedAt) {}

    record ShoppingCartConfirmedV2(
            UUID shoppingCartId,
            @Nullable String clientId,
            OffsetDateTime confirmedAt,
            @Nullable String initializedBy) {}

    @Test
    void shoppingCartConfirmedV1_withCompleteData_contractIsUnchanged() {
        CONTRACT.given(new ShoppingCartConfirmedV1(FIXED_CART_ID, "anonymised", FIXED_DATE))
                .whenSerialized()
                .thenContractIsUnchanged();
    }

    @Test
    void shoppingCartConfirmedV1_withNullClientId_contractIsUnchanged() {
        CONTRACT.given(new ShoppingCartConfirmedV1(FIXED_CART_ID, null, FIXED_DATE))
                .whenSerialized(Snapshot.forMessageType("ShoppingCartConfirmedV1_NullClientId"))
                .thenContractIsUnchanged();
    }

    @Test
    void shoppingCartConfirmedV2_withRequiredData_contractIsUnchanged() {
        CONTRACT.given(new ShoppingCartConfirmedV2(FIXED_CART_ID, "anonymised", FIXED_DATE, null))
                .whenSerialized(Snapshot.forMessageType("ShoppingCartConfirmedV2_WithRequiredData"))
                .thenContractIsUnchanged();
    }

    @Test
    void shoppingCartConfirmedV2_withCompleteData_contractIsUnchanged() {
        CONTRACT.given(new ShoppingCartConfirmedV2(FIXED_CART_ID, "anonymised", FIXED_DATE, "Oskar"))
                .whenSerialized()
                .thenContractIsUnchanged();
    }

    @Test
    void givenV1Event_whenReadAsV2_thenForwardCompatible() {
        CONTRACT.given(Snapshot.of(ShoppingCartConfirmedV1.class))
                .whenDeserializedAs(ShoppingCartConfirmedV2.class)
                .thenForwardCompatible(v2 -> assertNull(v2.initializedBy()));
    }

    @Test
    void givenV2Event_whenReadAsV1_thenBackwardCompatible() {
        CONTRACT.given(new ShoppingCartConfirmedV2(FIXED_CART_ID, "anonymised", FIXED_DATE, "admin"))
                .whenDeserializedAs(ShoppingCartConfirmedV1.class)
                .thenBackwardCompatible();
    }
}
