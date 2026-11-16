package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@NullMarked
@SuppressWarnings("UnusedVariable")
final class StructuralComparisonTests {
    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private record Case(
            String name,
            boolean compatible,
            @Nullable Object source,
            @Nullable Object target) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> cases() {
        var sharedCycle = cycle("a");
        return Stream.of(
                new Case("both null", true, null, null),
                new Case("source null", false, null, "a"),
                new Case("target null", false, "a", null),
                new Case("equal strings", true, "abc", "abc"),
                new Case("string case", false, "abc", "ABC"),
                new Case("string whitespace", false, "abc ", "abc"),
                new Case("char array is not a string", false, new char[] {'a', 'b'}, "ab"),
                new Case("string is not a char array", false, "ab", new char[] {'a', 'b'}),
                new Case("number is not a string", false, 1, "1"),
                new Case("string is not a number", false, "1", 1),
                new Case("int and long", true, 1, 1L),
                new Case("int and decimal with scale", true, 1, new BigDecimal("1.00")),
                new Case("double and float", true, 0.5d, 0.5f),
                new Case("decimal and float", true, new BigDecimal("0.1"), 0.1f),
                new Case("integer mismatch", false, 1, 2),
                new Case("lost fraction", false, 1.5d, 1),
                new Case("lossy float conversion", false, 16777217, 16777216f),
                new Case("NaN across widths", true, Double.NaN, Float.NaN),
                new Case("infinity across widths", true, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                new Case("NaN is not a number value", false, Double.NaN, 1.0d),
                new Case("number value is not NaN", false, 1.0d, Double.NaN),
                new Case("boolean is not a number", false, true, 1),
                new Case("char is not a number", false, 'a', 97),
                new Case("same enum constant", true, Status.ACTIVE, Status.ACTIVE),
                new Case("constant of evolved enum", true, Status.ACTIVE, StatusWithSuspended.ACTIVE),
                new Case("different enum constant", false, Status.ACTIVE, StatusWithSuspended.SUSPENDED),
                new Case("string read as enum", true, "ACTIVE", Status.ACTIVE),
                new Case("enum written as string", true, Status.ACTIVE, "ACTIVE"),
                new Case("enum is not its ordinal", false, Status.ACTIVE, 0),
                new Case("same JDK value", true, FIXED_ID, FIXED_ID),
                new Case(
                        "same platform JDK value",
                        true,
                        java.sql.Date.valueOf("2024-01-01"),
                        java.sql.Date.valueOf("2024-01-01")),
                new Case("value object read as scalar", false, new CustomerId(FIXED_ID), FIXED_ID),
                new Case("different JDK value", false, FIXED_ID, OTHER_ID),
                new Case(
                        "same date time",
                        true,
                        OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)),
                new Case("same record", true, new Person("Alice", 30), new Person("Alice", 30)),
                new Case("record member mismatch", false, new Person("Alice", 30), new Person("Bob", 30)),
                new Case(
                        "target adds optional member",
                        true,
                        new Person("Alice", 30),
                        new PersonWithNickname("Alice", 30, null)),
                new Case(
                        "target adds required member",
                        false,
                        new Person("Alice", 30),
                        new PersonWithEmail("Alice", 30, "alice@example.com")),
                new Case(
                        "required member was null in source",
                        false,
                        new PersonWithNullableName(null, 30),
                        new Person("Alice", 30)),
                new Case("target drops member", true, new Person("Alice", 30), new PersonWithoutAge("Alice")),
                new Case("renamed member", false, new Person("Alice", 30), new PersonRenamed("Alice", 30)),
                new Case(
                        "both sides have exclusive members",
                        false,
                        new PersonWithNickname("Alice", 30, "Al"),
                        new PersonWithAlias("Alice", 30, null)),
                new Case("widened member", true, new Person("Alice", 30), new PersonWithLongAge("Alice", 30L)),
                new Case("record read as class", true, new Person("Alice", 30), new PersonClass("Alice", 30)),
                new Case("class read as record", true, new PersonClass("Alice", 30), new Person("Alice", 30)),
                new Case(
                        "inherited members",
                        true,
                        new PersonEntity(FIXED_ID, "Alice"),
                        new PersonWithId(FIXED_ID, "Alice")),
                new Case("static members ignored", true, new NamedWithConstant("Alice"), new Named("Alice")),
                new Case(
                        "custom equals accepts",
                        true,
                        new Money(BigDecimal.TEN, "EUR", "first"),
                        new Money(BigDecimal.TEN, "EUR", "second")),
                new Case(
                        "custom equals rejects",
                        false,
                        new Money(BigDecimal.TEN, "EUR", "first"),
                        new Money(BigDecimal.TEN, "USD", "first")),
                new Case("empty records", true, new Marker(), new Marker()),
                new Case(
                        "nested optional member",
                        true,
                        new Customer("Alice", new Address("Main St 1", "01234")),
                        new CustomerWithCountry("Alice", new AddressWithCountry("Main St 1", "01234", null))),
                new Case(
                        "nested value mismatch",
                        false,
                        new Customer("Alice", new Address("Main St 1", "01234")),
                        new CustomerWithNumericZip("Alice", new AddressWithNumericZip("Main St 1", 1234))),
                new Case("same list", true, List.of(1, 2), List.of(1, 2)),
                new Case("reordered list", false, List.of(1, 2), List.of(2, 1)),
                new Case("list count mismatch", false, List.of(1, 2), List.of(1)),
                new Case("widened list elements", true, List.of(1, 2), List.of(1L, 2L)),
                new Case("empty lists", true, List.of(), new ArrayList<>()),
                new Case(
                        "list of evolved records",
                        true,
                        List.of(new Person("Alice", 30), new Person("Bob", 40)),
                        List.of(new PersonWithNickname("Alice", 30, null), new PersonWithNickname("Bob", 40, null))),
                new Case("null list element", true, listOf("a", null), listOf("a", null)),
                new Case("null list element mismatch", false, listOf("a", null), List.of("a", "b")),
                new Case("set in any order", true, Set.of(1, 2, 3), linkedSetOf(3, 1, 2)),
                new Case("list read as set", true, List.of(3, 1, 2), linkedSetOf(1, 2, 3)),
                new Case("set written as list", true, linkedSetOf(1, 2, 3), List.of(3, 1, 2)),
                new Case("duplicate lost in set", false, List.of(1, 1, 2), linkedSetOf(1, 2)),
                new Case("set element mismatch", false, Set.of(1, 2), Set.of(1, 3)),
                new Case(
                        "set of evolved records",
                        true,
                        Set.of(new Person("Alice", 30), new Person("Bob", 40)),
                        Set.of(new PersonWithNickname("Bob", 40, null), new PersonWithNickname("Alice", 30, null))),
                new Case("null set element", true, setWithNull("a"), setWithNull("a")),
                new Case("array read as list", true, new int[] {1, 2}, List.of(1L, 2L)),
                new Case("reordered array", false, new int[] {1, 2}, new int[] {2, 1}),
                new Case("jagged arrays", true, new int[][] {{1, 2}, {3}}, List.of(List.of(1L, 2L), List.of(3L))),
                new Case("byte arrays", true, new byte[] {1, 2}, new byte[] {1, 2}),
                new Case("map key order", true, mapOf("a", 1, "b", 2), mapOf("b", 2, "a", 1)),
                new Case("map missing key", false, mapOf("a", 1, "b", 2), mapOf("a", 1, "c", 2)),
                new Case("map extra key", false, Map.of("a", 1), mapOf("a", 1, "b", 2)),
                new Case("map value mismatch", false, Map.of("a", 1), Map.of("a", 2)),
                new Case("map key value association", false, mapOf("a", 1, "b", 2), mapOf("a", 2, "b", 1)),
                new Case("widened map values", true, Map.of("a", 1), Map.of("a", 1L)),
                new Case(
                        "map of evolved records",
                        true,
                        Map.of("home", new Address("Main St 1", "01234")),
                        Map.of("home", new AddressWithCountry("Main St 1", "01234", null))),
                new Case("evolved enum keys", true, Map.of(Status.ACTIVE, 1), Map.of(StatusWithSuspended.ACTIVE, 1)),
                new Case("string keys read as enum keys", true, Map.of("ACTIVE", 1), Map.of(Status.ACTIVE, 1)),
                new Case("map is not a list", false, Map.of("a", 1), List.of(1)),
                new Case("generic member read by another generic type", true, new Box<>("a"), new Parcel<>("a")),
                new Case("generic member mismatch", false, new Box<>("a"), new Parcel<>("b")),
                new Case("both optionals empty", true, Optional.empty(), Optional.empty()),
                new Case("empty optional read as null", true, Optional.empty(), null),
                new Case("null read as empty optional", true, null, Optional.empty()),
                new Case("present optional read as null", false, Optional.of("a"), null),
                new Case("value read into optional", true, "a", Optional.of("a")),
                new Case("optional value mismatch", false, Optional.of("a"), Optional.of("b")),
                new Case("widened optional value", true, Optional.of(1), Optional.of(1L)),
                new Case(
                        "optional of evolved record",
                        true,
                        Optional.of(new Address("Main St 1", "01234")),
                        Optional.of(new AddressWithCountry("Main St 1", "01234", null))),
                new Case(
                        "target adds optional-typed member",
                        true,
                        new Person("Alice", 30),
                        new PersonWithOptionalNickname("Alice", 30, Optional.empty())),
                new Case(
                        "nested optional member mismatch",
                        false,
                        new PersonWithOptionalNickname("Alice", 30, Optional.of("Al")),
                        new PersonWithOptionalNickname("Alice", 30, Optional.of("Ally"))),
                new Case("same reference cycle", true, sharedCycle, sharedCycle),
                new Case("distinct equivalent cycles", true, cycle("a"), cycle("a")),
                new Case("distinct different cycles", false, cycle("a"), cycle("b")),
                new Case("evolved cycle", true, cycle("a"), evolvedCycle("a")),
                new Case("cycle unrolled in target", true, cycle("a"), twoNodeLoop("a")),
                new Case("shared siblings", true, siblingsOf(new Node("a")), siblingsOf(new Node("a"))),
                new Case("collection cycles", true, selfContainingList(), selfContainingList()),
                new Case("deep chain", true, chain(500), chain(500)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void comparesWhatTheSourceWroteWithWhatTheTargetRead(Case testCase) {
        var difference = StructuralComparison.difference(testCase.source(), testCase.target());

        assertEquals(testCase.compatible(), difference == null, () -> testCase.name() + ": " + difference);
    }

    private enum Status {
        ACTIVE,
        CLOSED
    }

    private enum StatusWithSuspended {
        ACTIVE,
        CLOSED,
        SUSPENDED
    }

    private record Box<T>(T value) {}

    private record Parcel<T>(T value) {}

    private record Person(String name, int age) {}

    private record PersonWithNickname(
            String name, int age, @Nullable String nickname) {}

    private record PersonWithOptionalNickname(String name, int age, Optional<String> nickname) {}

    private record PersonWithAlias(
            String name, int age, @Nullable String alias) {}

    private record PersonWithEmail(String name, int age, String email) {}

    private record PersonWithoutAge(String name) {}

    private record PersonRenamed(String fullName, int age) {}

    private record PersonWithLongAge(String name, long age) {}

    private record PersonWithId(UUID id, String name) {}

    private record Named(String name) {}

    private record CustomerId(UUID value) {}

    private record PersonWithNullableName(@Nullable String name, int age) {}

    private record Marker() {}

    private record Address(String street, String zipCode) {}

    private record AddressWithCountry(
            String street, String zipCode, @Nullable String country) {}

    private record AddressWithNumericZip(String street, int zipCode) {}

    private record Customer(String name, Address address) {}

    private record CustomerWithCountry(String name, AddressWithCountry address) {}

    private record CustomerWithNumericZip(String name, AddressWithNumericZip address) {}

    private record Siblings(Node first, Node second) {}

    private static final class PersonClass {
        final String name;
        final int age;

        PersonClass(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static class Entity {
        final UUID id;

        Entity(UUID id) {
            this.id = id;
        }
    }

    private static final class PersonEntity extends Entity {
        final String name;

        PersonEntity(UUID id, String name) {
            super(id);
            this.name = name;
        }
    }

    private static final class NamedWithConstant {
        static final String TYPE = "Named";
        final String name;

        NamedWithConstant(String name) {
            this.name = name;
        }
    }

    private static final class Money {
        final BigDecimal amount;
        final String currency;
        final String note;

        Money(BigDecimal amount, String currency, String note) {
            this.amount = amount;
            this.currency = currency;
            this.note = note;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Money money && money.amount.equals(amount) && money.currency.equals(currency);
        }

        @Override
        public int hashCode() {
            return Objects.hash(amount, currency);
        }

        @Override
        public String toString() {
            return amount + " " + currency + " (" + note + ")";
        }
    }

    private static final class Node {
        final String name;

        @Nullable Node next;

        Node(String name) {
            this.name = name;
        }
    }

    private static final class NodeWithLabel {
        final String name;
        final @Nullable String label;

        @Nullable NodeWithLabel next;

        NodeWithLabel(String name) {
            this.name = name;
            this.label = null;
        }
    }

    private static Node cycle(String name) {
        var node = new Node(name);
        node.next = node;
        return node;
    }

    private static NodeWithLabel evolvedCycle(String name) {
        var node = new NodeWithLabel(name);
        node.next = node;
        return node;
    }

    private static Node twoNodeLoop(String name) {
        var first = new Node(name);
        var second = new Node(name);
        first.next = second;
        second.next = first;
        return first;
    }

    private static Siblings siblingsOf(Node node) {
        return new Siblings(node, node);
    }

    private static List<Object> selfContainingList() {
        var list = new ArrayList<Object>();
        list.add(list);
        return list;
    }

    private static Node chain(int length) {
        var root = new Node("0");
        var current = root;
        for (var i = 1; i < length; i++) {
            var next = new Node(String.valueOf(i));
            current.next = next;
            current = next;
        }
        return root;
    }

    private static List<@Nullable String> listOf(@Nullable String... items) {
        var list = new ArrayList<@Nullable String>();
        for (var item : items) {
            list.add(item);
        }
        return list;
    }

    private static Set<@Nullable String> setWithNull(String item) {
        var set = new LinkedHashSet<@Nullable String>();
        set.add(null);
        set.add(item);
        return set;
    }

    @SafeVarargs
    private static <T> Set<T> linkedSetOf(T... items) {
        var set = new LinkedHashSet<T>();
        for (var item : items) {
            set.add(item);
        }
        return set;
    }

    private static Map<String, Integer> mapOf(String firstKey, int firstValue, String secondKey, int secondValue) {
        var map = new LinkedHashMap<String, Integer>();
        map.put(firstKey, firstValue);
        map.put(secondKey, secondValue);
        return map;
    }
}
