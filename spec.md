# Spec: Configurable snapshot location strategies and multiple snapshots per contract

## Summary

Strictland writes one approved snapshot per message, flat, in the test's own
package directory, named after the message class (see
`FileSnapshotStorage.resolve()`). Two consequences hurt:

1. **Bloat.** A package accumulates dozens of `*.approved.txt` files
   interleaved with `*.java` sources, so the tests are hard to read even though
   each snapshot is easy to find.
2. **Collisions.** Two tests that pin the same message class write the same
   filename and silently overwrite each other, unless one passes an explicit
   `Snapshot.forMessageType("...")`.

This work introduces **configurable snapshot location strategies** and makes
**multiple snapshots per contract** a first-class, expected thing, with an
optional **value generator** seam for auto-synthesising canonical variants.

## Goals

- Let a project choose where snapshot files live and how they are grouped.
- Remove accidental collisions between tests pinning the same message type.
- Make snapshots carry their real format extension and a `.snap` marker.
- Make multiple snapshots per contract first-class: labelled variants, no silent
  overwrite, the label recorded as documentation.
- Provide a deserialization check that replays every recorded variant.
- Lay in a pluggable value-generator seam (dumb default now, smarter later).
- Keep the existing flat layout selectable for users who want it.

## Non-goals

- A smart/faker-style value generator. The default stays intentionally dumb; the
  seam is what we build now.
- An automated file migrator. Pre-1.0, the default may change and users opt into
  the old layout or move files by hand.
- Changing the compatibility-check semantics themselves (backward/forward).

## Background: two kinds of snapshot

Investigation of the current suite showed two distinct roles, with opposite
needs. The design serves both.

- **Owned snapshots.** Produced and asserted by
  `whenSerialized().thenContractIsUnchanged()`. Auto-generated, regenerated on
  change, conceptually belong to one test. They want **locality and
  collision-safety**.
- **Reference snapshots.** Hand-authored historical bytes (for example
  `CustomerRegisteredV1.approved.txt` and `AccountOpenedV1.approved.txt`, which
  no test ever writes), only **read** by compatibility tests, sometimes by a
  different class than pins the type. They want a **stable, discoverable,
  shareable name**: a registry.

## Two independent axes

The layout decomposes into two orthogonal axes plus a filename convention.

1. **Root location**: `NextToTest` or `GlobalRoot`.
2. **Grouping**: `PerTestClass` or `PerContract` (message type).

Both are configurable. They combine freely. The `Flat` layout remains a
selectable third option on the root axis: the original shape, kept for projects
that want today's behaviour, not a deprecated path.

## Configuration model

Every setting (layout strategy, grouping, wrapperFolder, rootPath, value
generator, requiredness policy) is configurable at three levels, plus the
built-in fallback. They resolve with precedence:

**per-spec `SpecificationOptions` > global fluent (code) > `strictland.properties`
(file) > built-in defaults.**

The rule is most-explicit-and-most-local wins: a per-spec override beats a
process-wide programmatic default, which beats the ambient file, which beats the
built-in.

- **Per-spec fluent API**: methods on `SpecificationOptions` override for a single
  specification. Highest precedence.

- **Global fluent API**: a process-wide programmatic default set once in code, so
  a project picks its layout in one place instead of repeating it. A small static
  config holder, for example:

  ```java
  // set once (a static init, or a JUnit @BeforeAll on a shared base)
  Strictland.defaults()
      .snapshotLayout(SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT))
      .valueGenerator(myGenerator)
      .requiredFieldPolicy(myPolicy);

  Strictland.resetDefaults(); // restore built-ins (for test isolation)
  ```

- **Config file**: `strictland.properties` on the test classpath (test resources
  root), read with `java.util.Properties`. Zero new dependencies: Strictland core
  must not depend on Jackson, since custom serializers exist. A JSON variant can
  be added later behind the same loader. Recognised keys (all optional):

  ```properties
  # strictland.properties
  strictland.layout.strategy=nextToTest      # nextToTest | globalRoot | flat
  strictland.layout.grouping=perTestClass    # perTestClass | perContract
  strictland.layout.wrapperFolder=snapshots  # folder name wrapping the groups
  strictland.layout.rootPath=src/test/resources/snapshots  # globalRoot only
  ```

A setting left unset at one level falls through to the next; the levels merge per
setting, they do not replace each other wholesale (for example a global value
generator still applies when a spec only overrides the layout).

Per-spec fluent equivalent (immutable copy-on-write style, matching the existing
options API):

```java
SpecificationOptions.serializer(serializer)
    .snapshotLayout(SnapshotLayout.nextToTest()
        .grouping(Grouping.PER_TEST_CLASS)
        .wrapperFolder("snapshots"))
```

> Note: the global config holder is process-wide mutable state. Prefer setting it
> once at process start. A test that mutates it should restore it with
> `resetDefaults()` so test order cannot change where snapshots land.

### Built-in defaults

When no per-spec override, no global config, and no config file are present:

- strategy = `NextToTest`
- grouping = `PerTestClass`
- wrapperFolder = `snapshots`

This is the new out-of-the-box behaviour and replaces today's flat default.

## Filename convention

Settled and orthogonal to folder layout.

- Committed baseline: `<base>.snap.approved.<ext>`
- Transient on mismatch: `<base>.snap.received.<ext>`

`<ext>` is the **format extension declared by the serializer** (see below). The
`approved`/`received` verbs and the diff/promote workflow stay as ApprovalTests
provides them. ApprovalTests 24.0.0 supports this via
`Options.forFile().withExtension(".json").withBaseName("<base>.snap")`; the
`.txt` default is not mandatory.

`<base>` is the leaf name from the grouping/variant rules below.

### Serializer declares its format extension

`MessageSerializer` gains a way to declare its file extension, used to name
snapshot files.

```java
public interface MessageSerializer {
    byte[] serialize(Object value);
    <T> T deserialize(byte[] bytes, Class<T> type);

    /** File extension for snapshots this serializer produces, e.g. ".json". */
    default String fileExtension() {
        return ".txt";
    }
}
```

- `Json.Jackson` returns `.json`.
- The CSV sample returns `.csv`, the binary sample `.bin`.
- The default keeps existing custom serializers compiling.

## Folder layout, worked examples

Message type `OrderInitiated`, pinned by `OrderInitiatedTests`, JSON.

**NextToTest + PerTestClass (default):**

```
src/test/java/io/eventdriven/strictland/
  OrderInitiatedTests.java
  snapshots/
    OrderInitiatedTests/
      OrderInitiated.snap.approved.json
```

**NextToTest + PerContract:**

```
src/test/java/io/eventdriven/strictland/
  OrderInitiatedTests.java
  snapshots/
    OrderInitiated/
      IsoDate.snap.approved.json
      EpochDate.snap.approved.json
```

**GlobalRoot + PerContract (the registry; natural home for reference
fixtures):**

```
src/test/resources/snapshots/
  io/eventdriven/strictland/
    OrderInitiated/
      IsoDate.snap.approved.json
```

**Flat (selectable):** the original behaviour, `<leaf>.approved.txt` straight in
the test's source directory, with no wrapper folder, no grouping, and no `.snap`
marker. Kept byte-identical so a project can opt back into it.

### Where the tree is anchored

The two test-relative strategies, `NextToTest` and `Flat`, anchor on the test's
own source directory, the real file on disk, not a reconstructed
`src/test/java/<package>`. The examples above show a `java` source set only
because that is where the sample tests live; the same resolution puts snapshots
beside a `src/test/kotlin` or `src/test/scala` test without configuration, since
it never hard-codes the source root.

That directory is found through a small internal locator, behind a one-method
seam, whose default delegates to ApprovalTests' own source-file resolution. The
seam reuses a maintained guesser today and leaves room to replace it later
without changing how layouts resolve.

`GlobalRoot` does not anchor on the test file. It roots the tree at the
`rootPath` you name and lays the test's package path beneath it, taken from the
class at runtime, so it resolves the same regardless of source set.

### Leaf name rules

- `PerTestClass` grouping: the leaf defaults to the message type name. A variant
  label, when supplied, becomes the leaf.
- `PerContract` grouping: the folder is the message type. The leaf is the
  variant label, defaulting to the message type name when none is given.

## Multiple snapshots per contract

A contract may have many snapshots, distinguished by a **variant label**. The
label is the leaf filename, recorded by `thenContractIsUnchanged` so it reads as
documentation (for example `OrderInitiated / NullPromotion`). No silent
overwrite.

### Manual variants

```java
MessageContract.specification(options)
    .given(new OrderInitiated(orderId, null, initiatedAt))
    .whenSerialized(Snapshot.variant("NullPromotion"))
    .thenContractIsUnchanged();
```

### Auto-generated variants (value generator)

`Snapshot.ByClass<S>` gains a fluent `.variant(...)`. The variant is an enum for
the predefined canonical shapes, with a string overload for custom labels.

```java
public enum Variant { REQUIRED_ONLY, FULL }
```

```java
// target a specific generated variant
Snapshot.of(OrderPlaced.class).variant(Variant.REQUIRED_ONLY);
Snapshot.of(OrderPlaced.class).variant("IsoDate"); // custom label
```

Syntactic sugar `given(Class)` is shorthand for `given(Snapshot.of(Class))` and
auto-generates the predefined variant set:

```java
MessageContract.specification(options)
    .given(OrderPlaced.class)        // generates REQUIRED_ONLY and FULL
    .whenSerialized()
    .thenContractIsUnchanged();
// -> OrderPlaced/requiredOnly.snap.approved.json
// -> OrderPlaced/allFilled.snap.approved.json
```

### Reading variants back (replay-all)

A deserialization check from a class replays **every** recorded variant of the
contract and asserts each still reads as the new type. Targeting a single
variant by label stays available.

```java
// replays all variants of OrderPlaced
MessageContract.specification(options)
    .given(Snapshot.of(OrderPlaced.class))
    .whenDeserializedAs(OrderPlacedWithCoupon.class)
    .thenBackwardCompatible();

// one variant
MessageContract.specification(options)
    .given(Snapshot.of(OrderPlaced.class).variant("IsoDate"))
    .whenDeserializedAs(OrderPlacedWithCoupon.class)
    .thenBackwardCompatible();
```

## Value generator

A small seam, built now, dumb by default.

```java
public interface ValueGenerator {
    <T> T generate(Class<T> type, Variant variant);
}
```

The `Variant` enum (`REQUIRED_ONLY`, `FULL`) is reused here as the fill mode, so
there is no separate "requiredness" enum.

**Hard constraint: determinism.** Generated snapshots must be byte-for-byte
stable across runs and machines, or `thenContractIsUnchanged` flaps. The default
generator emits fixed values per type:

- `String` -> `"string"`
- `UUID` -> `00000000-0000-0000-0000-000000000001`
- numeric -> a fixed constant
- `Instant`/date types -> a fixed epoch
- enums -> the first constant
- records (preferred): construct via the canonical constructor with generated
  components
- POJOs (supported): instantiate via the no-arg constructor and set generated
  values into the declared fields by reflection; if there is no usable no-arg
  constructor, throw a clear error naming the type and suggesting a manual variant

Any future faker/Bogus-style generator must be seeded to preserve determinism.
Initial scope targets records and POJOs plus the common scalar types above; the
seam allows growth.

### Field-requirement policy

`REQUIRED_ONLY` fills required fields and leaves optional ones null/empty;
`FULL` fills everything. The policy answers, per field, which it is. It keys off
`Field` so it works for both record backing fields and POJO fields.

```java
public interface RequiredFieldPolicy {
    boolean isRequired(Field field);
}
```

The default is `@Nullable`-driven: a field is optional when it carries a
`@Nullable` annotation, otherwise required. Detection is by annotation simple
name (`Nullable`) across the field's declared annotations and its type-use
annotations, so it works whatever nullability library a project uses (JSpecify,
JetBrains, jakarta, javax) without a hard dependency on any. A JSpecify
`@Nullable` is a `TYPE_USE` annotation retained at runtime and propagates to a
record's backing field, so the same check covers records and POJOs. No
`Optional<>` handling: `Optional` as a field is an anti-pattern and never appears
in a message contract.

- A fully non-null type has no optional fields, so its `REQUIRED_ONLY` equals its
  `FULL`. When the two generated instances serialize identically, `given(Class)`
  writes a single snapshot named after the type (no variant suffix) rather than
  two identical files.
- The policy is a seam (Open-Closed). Other policies (Bean Validation
  `@NotNull`, custom) can be added later without changing core.

Both `ValueGenerator` and `RequiredFieldPolicy` are configurable through
`SpecificationOptions` (and the global config) and default to the
dumb/nullability-driven pair.

## API changes summary

- `MessageSerializer`: add `default String fileExtension()`.
- `Json.Jackson`, CSV and binary samples: override `fileExtension()`.
- `SpecificationOptions`: add layout config (`snapshotLayout(...)` or
  equivalent), `valueGenerator(...)`, and the field-requirement policy; keep
  immutable copy-on-write style. Refactor it to carry configurable settings as
  "unset" (nullable) rather than eagerly defaulting, so per-key fallthrough works
  (the standard layered-config resolution, as in Spring Boot). Existing default
  behaviour is preserved for callers who never set these.
- New types: `SnapshotLayout` (strategy + grouping + wrapperFolder + rootPath),
  `Grouping` enum, `Variant` enum (also the generator fill mode), `ValueGenerator`,
  `RequiredFieldPolicy`.
- `Snapshot`: add `variant(String)` factory; add `.variant(Variant)` and
  `.variant(String)` fluent methods on `ByClass<S>`.
- `MessageContract`: add `given(Class)` sugar.
- `GivenStep`: support replay-all when given a `ByClass` without a variant.
- Layout and variant resolution happen in the DSL step
  (`ThenContractStep`/`ThenCompatibilityStep`), which holds the layout, the type
  mapper, and the `Snapshot`. The step resolves a snapshot identity and hands it
  to storage already resolved. Storage does not walk the stack, know the layout,
  or understand variants.
- `SnapshotStorage` keeps its two-method shape, `store(name, bytes)` and
  `read(name)`. The variant is folded into the resolved identity before storage
  is called, so the public extension point gains no `variantLabel` overloads and
  a custom storage needs no concept of variants.
- `FileSnapshotStorage`: write and read at the identity the step resolved, with
  no hard-coded `src/test/java/<package>` and no ApprovalTests duplicate-tracker
  workarounds.
- New `Strictland` global config holder: a process-wide fluent default for every
  setting (layout, value generator, requiredness policy), with `defaults()` and
  `resetDefaults()`.
- Config loader: read `strictland.properties` from the test classpath.
- A single resolution point that merges the four levels per setting (per-spec >
  global > file > built-in) when a specification builds its storage.

## Backward compatibility and migration

- Pre-1.0, the default layout changes to `NextToTest + PerTestClass`. Existing
  users keep the old behaviour by selecting `Flat`.
- Existing `Snapshot.of`, `Snapshot.forMessageType`, `Snapshot.at` keep working,
  reinterpreted within the active layout.
- The repository's own ~30 snapshots are relocated to the new structure as part
  of this work, with their tests updated.
- No automated migrator ships.

## Testing requirements

Per the project's standard (100% line and branch coverage, documented public
API, outside-in tests driving the public DSL):

- Path resolution for every (strategy x grouping) combination, plus `Flat`.
- Config-file loading and precedence over defaults; fluent override over file.
- Filename composition, including the serializer-declared extension.
- Manual variants: distinct files, no overwrite, label recorded.
- Auto-generation: deterministic bytes, `REQUIRED_ONLY` vs `FULL`,
  nullability-driven requiredness, records and scalar types.
- Replay-all deserialization across multiple variants; single-variant targeting.
- Javadoc with examples for every new public/protected member.

## Open questions / future work

- Smarter value generators (faker/Bogus-style, seeded) behind the existing seam.
- A JSON config-file format behind the same loader.
- An optional automated migrator, if demand appears.
