package io.eventdriven.strictland;

import static java.time.ZoneOffset.UTC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class SerializationContractTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_DATE = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, UTC);

    private record OrderPlaced(UUID orderId, String customer, OffsetDateTime placedAt) {}

    private record OrderInitiatedV1(UUID orderId, OffsetDateTime initiatedAt) {}

    private record OrderInitiatedV2(UUID orderId, @Nullable String promotionCode, OffsetDateTime initiatedAt) {}

    private record OrderInitiatedV3(UUID orderId, OrderStatus status) {}

    private enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPED
    }

    private record ShipmentScheduledV1(UUID shipmentId, String recipientName, OffsetDateTime scheduledAt) {}

    private record EmptyMarkerEvent() {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = InvoiceIssued.class, name = "InvoiceIssued"),
        @JsonSubTypes.Type(value = InvoicePaid.class, name = "InvoicePaid")
    })
    private sealed interface InvoiceEvent permits InvoiceIssued, InvoicePaid {}

    private record InvoiceIssued(UUID invoiceId, BigDecimal amount) implements InvoiceEvent {}

    private record InvoicePaid(UUID invoiceId) implements InvoiceEvent {}

    private record MemberJoined(UUID userId, String email) {}

    private record UserOnboardedV2(
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("user_email") String email) {}

    @Nested
    final class ShapeIsPinned {
        @Test
        void given_eventWithData_whenSerialized_shapeIsPinned() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderPlaced(FIXED_ID, "Alice", FIXED_DATE))
                    .whenSerialized()
                    .thenContractIsUnchanged();
        }

        @Test
        void given_eventWithNullField_whenSerialized_nullRendersByDefault() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderInitiatedV2(FIXED_ID, null, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV2").variant("nullDefault"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_markerEventWithNoFields_whenSerialized_producesEmptyObject() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new EmptyMarkerEvent())
                    .whenSerializedAs(Snapshot.forMessageType("EmptyMarkerEvent"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_polymorphicEvent_whenSerialized_discriminatorIsPinned() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new InvoiceIssued(FIXED_ID, new BigDecimal("99.99")))
                    .whenSerializedAs(Snapshot.forMessageType("InvoiceIssuedEvent"))
                    .thenContractIsUnchanged();
        }
    }

    @Nested
    final class UsingYourOwnMapper {
        private final ObjectMapper timestampMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        @Test
        void given_customMapper_whenSerialized_thatMapperDrivesTheOutput() {
            MessageContract.specification(Json.Jackson.of(timestampMapper))
                    .given(new OrderInitiatedV1(FIXED_ID, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV1").variant("customMapper"))
                    .thenContractIsUnchanged();
        }
    }

    @Nested
    final class WhereTheGoldenFileLives {
        @Test
        void given_event_whenSerialized_goldenFileIsNamedAfterTheEventClass() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new MemberJoined(FIXED_ID, "alice@example.com"))
                    .whenSerializedAs(Snapshot.of(MemberJoined.class))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_event_whenSerialized_goldenFileNamedByMessageType() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new MemberJoined(FIXED_ID, "alice@example.com"))
                    .whenSerializedAs(Snapshot.forMessageType("MemberJoined"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_event_whenSerialized_goldenFileAtAnExplicitPath() {
            var path = Path.of(
                    "src/test/java/io/eventdriven/strictland/snapshots/WhereTheGoldenFileLives/MemberJoined_ByPath.snap.approved.json");

            MessageContract.specification(Json.Jackson.defaults())
                    .given(new MemberJoined(FIXED_ID, "alice@example.com"))
                    .whenSerializedAs(Snapshot.at(path))
                    .thenContractIsUnchanged();
        }
    }

    @Nested
    final class SerializerChangesAreCaught {
        private final ObjectMapper isoMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        private final ObjectMapper epochMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        private final ObjectMapper includeNullsMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        private final ObjectMapper omitNullsMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        private final ObjectMapper snakeCaseMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        private final ObjectMapper enumIndexMapper = new JsonMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);

        @Test
        void given_isoMapper_whenSerialized_datesAreIsoStrings() {
            MessageContract.specification(Json.Jackson.of(isoMapper))
                    .given(new OrderInitiatedV1(FIXED_ID, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV1").variant("isoDates"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_epochMapper_whenSerialized_datesAreEpochNumbers() {
            MessageContract.specification(Json.Jackson.of(epochMapper))
                    .given(new OrderInitiatedV1(FIXED_ID, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV1").variant("epochDates"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_nullInclusion_whenSerialized_nullFieldIsPresent() {
            MessageContract.specification(Json.Jackson.of(includeNullsMapper))
                    .given(new OrderInitiatedV2(FIXED_ID, null, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV2").variant("nullIncluded"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_nullExclusion_whenSerialized_nullFieldIsOmitted() {
            MessageContract.specification(Json.Jackson.of(omitNullsMapper))
                    .given(new OrderInitiatedV2(FIXED_ID, null, FIXED_DATE))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV2").variant("nullOmitted"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_snakeCaseMapper_whenSerialized_keysAreSnakeCase() {
            MessageContract.specification(Json.Jackson.of(snakeCaseMapper))
                    .given(new ShipmentScheduledV1(FIXED_ID, "Alice Smith", FIXED_DATE))
                    .whenSerializedAs(Snapshot.forMessageType("ShipmentScheduledV1"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_enumByName_whenSerialized_enumIsAString() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new OrderInitiatedV3(FIXED_ID, OrderStatus.CONFIRMED))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV3").variant("enumAsName"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_enumByIndex_whenSerialized_enumIsANumber() {
            MessageContract.specification(Json.Jackson.of(enumIndexMapper))
                    .given(new OrderInitiatedV3(FIXED_ID, OrderStatus.CONFIRMED))
                    .whenSerializedAs(
                            Snapshot.forMessageType("OrderInitiatedV3").variant("enumAsNumber"))
                    .thenContractIsUnchanged();
        }

        @Test
        void given_jsonPropertyRename_whenSerialized_logicalFieldNamesAreCaught() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new UserOnboardedV2(FIXED_ID, "alice@example.com"))
                    .whenSerializedAs(Snapshot.forMessageType("UserOnboardedV2"))
                    .thenContractIsUnchanged();
        }
    }
}
