package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
final class DeepStructureCompatibilityTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final Location LOCATION = new Location("Main St 1", "01234");
    private static final OrderShipped ORDER_SHIPPED = new OrderShipped(FIXED_ID, new Recipient("Alice", LOCATION));

    private record Location(String street, String zipCode) {}

    private record Recipient(String name, Location location) {}

    private record OrderShipped(UUID orderId, Recipient recipient) {}

    private static String failureOf(Runnable check) {
        var error = assertThrows(AssertionError.class, check::run);
        return requireNonNull(error.getMessage());
    }

    @Nested
    final class EvolvingAThreeLevelRecord {
        private record LocationWithCountry(
                String street, String zipCode, @Nullable String country) {}

        private record RecipientWithCountry(String name, LocationWithCountry location) {}

        private record OrderShippedWithCountry(UUID orderId, RecipientWithCountry recipient) {}

        private record LocationWithRequiredCountry(String street, String zipCode, String country) {}

        private record RecipientWithRequiredCountry(String name, LocationWithRequiredCountry location) {}

        private record OrderShippedWithRequiredCountry(UUID orderId, RecipientWithRequiredCountry recipient) {}

        private record LocationWithNumericZip(String street, int zipCode) {}

        private record RecipientWithNumericZip(String name, LocationWithNumericZip location) {}

        private record OrderShippedWithNumericZip(UUID orderId, RecipientWithNumericZip recipient) {}

        private record LocationWithStreetName(@Nullable String streetName, String zipCode) {}

        private record RecipientWithStreetName(String name, LocationWithStreetName location) {}

        private record OrderShippedWithStreetName(UUID orderId, RecipientWithStreetName recipient) {}

        private record LocationWithoutZip(String street) {}

        private record RecipientWithoutZip(String name, LocationWithoutZip location) {}

        private record OrderShippedWithoutZip(UUID orderId, RecipientWithoutZip recipient) {}

        @Test
        void givenAShippedOrder_whenTheInnermostRecordAddsAnOptionalField_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(ORDER_SHIPPED)
                    .whenDeserializedAs(OrderShippedWithCountry.class)
                    .thenBackwardCompatible();
        }

        @Test
        void givenAShippedOrder_whenTheInnermostRecordAddsARequiredField_thenFailsAtItsPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(ORDER_SHIPPED)
                    .whenDeserializedAs(OrderShippedWithRequiredCountry.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Required field 'recipient.location.country'"), message);
        }

        @Test
        void givenAShippedOrder_whenTheInnermostZipCodeBecomesNumeric_thenFailsAtItsPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(ORDER_SHIPPED)
                    .whenDeserializedAs(OrderShippedWithNumericZip.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Field 'recipient.location.zipCode' value differs"), message);
        }

        @Test
        void givenAShippedOrder_whenTheInnermostStreetIsRenamed_thenStructurallyIncompatibleAtItsPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(ORDER_SHIPPED)
                    .whenDeserializedAs(OrderShippedWithStreetName.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Structural incompatibility detected at 'recipient.location'"), message);
        }

        @Test
        void givenAShippedOrder_whenAnOlderReaderDropsTheInnermostZipCode_thenForwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(ORDER_SHIPPED)
                    .whenDeserializedAs(OrderShippedWithoutZip.class)
                    .thenForwardCompatible(order -> assertEquals(
                            "Main St 1", order.recipient().location().street()));
        }

        @Test
        void givenAStoredShippedOrder_whenTheInnermostRecordAddsARequiredField_thenFailsAtItsPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(MessageSnapshot.at(Path.of("DeepStructures/OrderShippedV1.reference.snap.approved.json")))
                    .whenDeserializedAs(OrderShippedWithRequiredCountry.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Required field 'recipient.location.country'"), message);
        }
    }

    @Nested
    final class EvolvingAListOfNestedRecords {
        private record Price(int amount, String currency) {}

        private record CartLine(String sku, Price price) {}

        private record CartCheckedOut(UUID cartId, List<CartLine> lines) {}

        private record PriceInCents(long amount, String currency) {}

        private record CartLineInCents(String sku, PriceInCents price) {}

        private record CartCheckedOutInCents(UUID cartId, List<CartLineInCents> lines) {}

        private record CartCheckedOutAsSet(UUID cartId, Set<CartLineInCents> lines) {}

        private record CartLineWithWarehouse(String sku, Price price, String warehouse) {}

        private record CartCheckedOutWithWarehouse(UUID cartId, List<CartLineWithWarehouse> lines) {}

        record CartCheckedOutSortedBySku(UUID cartId, List<CartLine> lines) {
            CartCheckedOutSortedBySku {
                lines = lines.stream()
                        .sorted(Comparator.comparing(CartLine::sku))
                        .toList();
            }
        }

        record CartCheckedOutWithoutDuplicates(UUID cartId, List<CartLine> lines) {
            CartCheckedOutWithoutDuplicates {
                lines = lines.stream().distinct().toList();
            }
        }

        private static final CartCheckedOut CHECKED_OUT = new CartCheckedOut(
                FIXED_ID,
                List.of(
                        new CartLine("pen", new Price(5, "EUR")),
                        new CartLine("book", new Price(30, "EUR")),
                        new CartLine("ink", new Price(12, "EUR"))));

        private static final CartCheckedOut CHECKED_OUT_WITH_DUPLICATE = new CartCheckedOut(
                FIXED_ID, List.of(new CartLine("pen", new Price(5, "EUR")), new CartLine("pen", new Price(5, "EUR"))));

        @Test
        void givenCartLines_whenTheNestedPriceIsWidened_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT)
                    .whenDeserializedAs(CartCheckedOutInCents.class)
                    .thenBackwardCompatible();
        }

        @Test
        void givenCartLines_whenReadIntoASetOfEvolvedLines_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT)
                    .whenDeserializedAs(CartCheckedOutAsSet.class)
                    .thenBackwardCompatible(cart -> assertEquals(3, cart.lines().size()));
        }

        @Test
        void givenADuplicatedCartLine_whenReadIntoASet_thenFailsAtTheListPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT_WITH_DUPLICATE)
                    .whenDeserializedAs(CartCheckedOutAsSet.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Field 'lines' value differs"), message);
        }

        @Test
        void givenCartLines_whenTheReaderReordersThem_thenFailsAtTheFirstReorderedLine() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT)
                    .whenDeserializedAs(CartCheckedOutSortedBySku.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Field 'lines[0].sku' value differs"), message);
        }

        @Test
        void givenADuplicatedCartLine_whenTheReaderDropsDuplicates_thenFailsAtTheListPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT_WITH_DUPLICATE)
                    .whenDeserializedAs(CartCheckedOutWithoutDuplicates.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Field 'lines' value differs"), message);
        }

        @Test
        void givenCartLines_whenEachLineAddsARequiredWarehouse_thenFailsAtTheFirstLine() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(CHECKED_OUT)
                    .whenDeserializedAs(CartCheckedOutWithWarehouse.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Required field 'lines[0].warehouse'"), message);
        }
    }

    @Nested
    final class EvolvingAMapOfNestedRecords {
        private record CustomerProfiled(UUID customerId, Map<String, Location> addresses) {}

        private record ProfileLocationWithCountry(
                String street, String zipCode, @Nullable String country) {}

        private record CustomerProfiledWithCountry(
                UUID customerId, Map<String, ProfileLocationWithCountry> addresses) {}

        private record ProfileLocationWithNumericZip(String street, int zipCode) {}

        private record CustomerProfiledWithNumericZip(
                UUID customerId, Map<String, ProfileLocationWithNumericZip> addresses) {}

        private static final CustomerProfiled PROFILED = new CustomerProfiled(
                FIXED_ID, new TreeMap<>(Map.of("home", LOCATION, "work", new Location("Dock 2", "98765"))));

        @Test
        void givenAddressesByLabel_whenEachAddressAddsAnOptionalCountry_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(PROFILED)
                    .whenDeserializedAs(CustomerProfiledWithCountry.class)
                    .thenBackwardCompatible();
        }

        @Test
        void givenAddressesByLabel_whenTheZipCodeBecomesNumeric_thenFailsAtTheEntryPath() {
            var message = failureOf(() -> MessageContract.specification(Json.Jackson.defaults())
                    .given(PROFILED)
                    .whenDeserializedAs(CustomerProfiledWithNumericZip.class)
                    .thenBackwardCompatible());

            assertTrue(message.contains("Field 'addresses[home].zipCode' value differs"), message);
        }
    }

    @Nested
    final class EvolvingNestedScalars {
        private enum PaymentMethod {
            CARD,
            TRANSFER
        }

        private record PaymentDetails(String method, int installments) {}

        private record PaymentSettled(UUID paymentId, PaymentDetails details) {}

        private record PaymentDetailsWithMethodEnum(PaymentMethod method, long installments) {}

        private record PaymentSettledWithMethodEnum(UUID paymentId, PaymentDetailsWithMethodEnum details) {}

        static final class ScoresRecorded {
            public final UUID studentId;
            public final int[] scores;

            ScoresRecorded(UUID studentId, int[] scores) {
                this.studentId = studentId;
                this.scores = scores.clone();
            }
        }

        private record ScoresRecordedAsList(UUID studentId, List<Long> scores) {}

        @Test
        void givenNestedPaymentDetails_whenTheMethodBecomesAnEnumAndCountIsWidened_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new PaymentSettled(FIXED_ID, new PaymentDetails("CARD", 3)))
                    .whenDeserializedAs(PaymentSettledWithMethodEnum.class)
                    .thenBackwardCompatible(payment ->
                            assertEquals(PaymentMethod.CARD, payment.details().method()));
        }

        @Test
        void givenScoresAsAnArray_whenReadAsAListOfLongs_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(new ScoresRecorded(FIXED_ID, new int[] {90, 75, 82}))
                    .whenDeserializedAs(ScoresRecordedAsList.class)
                    .thenBackwardCompatible(scores -> assertEquals(List.of(90L, 75L, 82L), scores.scores()));
        }
    }

    @Nested
    final class EvolvingAGraphWithBackReferences {
        static final class Folder {
            public String name = "";

            @JsonManagedReference
            public List<Document> documents = new ArrayList<>();
        }

        static final class Document {
            public String title = "";

            @JsonBackReference
            public @Nullable Folder folder;
        }

        static final class FolderWithOwner {
            public String name = "";
            public @Nullable String owner;

            @JsonManagedReference
            public List<DocumentWithAuthor> documents = new ArrayList<>();
        }

        static final class DocumentWithAuthor {
            public String title = "";
            public @Nullable String author;

            @JsonBackReference
            public @Nullable FolderWithOwner folder;
        }

        private static Folder folderWith(String... titles) {
            var folder = new Folder();
            folder.name = "Contracts";
            for (var title : titles) {
                var document = new Document();
                document.title = title;
                document.folder = folder;
                folder.documents.add(document);
            }
            return folder;
        }

        @Test
        void givenAFolderWhoseDocumentsPointBack_whenBothSidesAddOptionalFields_thenBackwardCompatible() {
            MessageContract.specification(Json.Jackson.defaults())
                    .given(folderWith("NDA", "Offer"))
                    .whenDeserializedAs(FolderWithOwner.class)
                    .thenBackwardCompatible(folder -> assertSame(folder, folder.documents.get(0).folder));
        }
    }
}
