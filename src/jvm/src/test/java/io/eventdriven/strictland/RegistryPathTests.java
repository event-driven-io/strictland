package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.membership.Membership;
import com.acme.orders.OrderPlaced;
import io.eventdriven.strictland.examples.shipping.ShipmentScheduled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

// Each test pins its own snapshot, then reads it back and asserts the resolved on-disk path. Each scenario
// demonstrates one namespace-to-folders strategy: a non-strictland root, a deep strictland package, a nested
// record under a clean encloser, and a dotless logical name that lands flat at the registry root. The pinned
// canonical file is shared idempotently with the writer suites, so the events are built with identical field
// values and the bytes match whichever suite runs first.
@NullMarked
final class RegistryPathTests {

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SHIPMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);

    @Test
    void aTopLevelEventInANonStrictlandRoot_isPinnedUnderItsNamespaceFolders() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new OrderPlaced(FIXED_ID, "Alice", FIXED_DATE))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.of(OrderPlaced.class))
                .whenDeserializedAs(OrderPlaced.class)
                .thenBackwardCompatible(order -> assertEquals("Alice", order.customer()));

        assertTrue(
                Files.exists(
                        Path.of(
                                "src/test/resources/contract-registry/com/acme/orders/OrderPlaced/OrderPlaced.1.default.snap.approved.json")));
    }

    @Test
    void aDeeplyNestedStrictlandPackage_isPinnedUnderItsFullNamespace() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new ShipmentScheduled(SHIPMENT_ID, "Alice Smith", FIXED_DATE))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.of(ShipmentScheduled.class))
                .whenDeserializedAs(ShipmentScheduled.class)
                .thenBackwardCompatible(shipment -> assertEquals("Alice Smith", shipment.recipientName()));

        assertTrue(
                Files.exists(
                        Path.of(
                                "src/test/resources/contract-registry/io/eventdriven/strictland/examples/shipping/ShipmentScheduled/ShipmentScheduled.1.default.snap.approved.json")));
    }

    @Test
    void aNestedRecordInACleanOuterType_isPinnedUnderItsEncloser() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new Membership.MemberJoined(FIXED_ID, "alice@example.com"))
                .whenSerialized()
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.of(Membership.MemberJoined.class))
                .whenDeserializedAs(Membership.MemberJoined.class)
                .thenBackwardCompatible(member -> assertEquals("alice@example.com", member.email()));

        assertTrue(
                Files.exists(
                        Path.of(
                                "src/test/resources/contract-registry/com/acme/membership/Membership/MemberJoined/MemberJoined.1.default.snap.approved.json")));
    }

    @Test
    void aDotlessLogicalName_isPinnedFlatAtTheRegistryRoot() {
        MessageContract.specification(Json.Jackson.defaults())
                .given(new Membership.MemberJoined(FIXED_ID, "alice@example.com"))
                .whenSerializedAs(MessageSnapshot.ofTypeNamed("MemberJoined"))
                .thenContractIsUnchanged();

        MessageContract.specification(Json.Jackson.defaults())
                .given(MessageSnapshot.ofTypeNamed("MemberJoined"))
                .whenDeserializedAs(Membership.MemberJoined.class)
                .thenBackwardCompatible(member -> assertEquals("alice@example.com", member.email()));

        assertTrue(Files.exists(Path.of(
                "src/test/resources/contract-registry/MemberJoined/MemberJoined.1.default.snap.approved.json")));
    }
}
