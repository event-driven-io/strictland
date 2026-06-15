# Implementation plan: configurable snapshot layouts and multiple snapshots per contract

Source spec: [spec.md](spec.md). Decisions log: [qa.md](qa.md).

## How this plan is built

The hard constraint shaping every step is the build gate: `./gradlew build`
enforces **100% line and branch coverage**, **Javadoc with `-Xwerror`**,
**NullAway**, and **Spotless**. That means you cannot land a new public type or
method unless a test exercises it and it carries Javadoc in the same step. So
every step is vertically integrated: tests plus implementation plus wiring, all
green. There is no "scaffolding now, use it later".

The second constraint, from the request, is to keep the not-working state as
small as possible. The on-disk snapshot convention is the one genuinely
disruptive change: renaming `*.approved.txt` to `*.snap.approved.<ext>` and
moving files into folders breaks the whole existing suite at once. So the plan is
**additive first, flip last**:

- Steps 1 to 9 add new capability behind explicit opt-in. The existing default
  behaviour (today's flat `src/test/java/<pkg>/<name>.approved.txt`) is preserved
  as a `Legacy` layout and stays the default, so the existing suite never breaks
  while we build.
- Step 10 is the single disruptive step: flip the default and migrate the repo's
  own ~30 snapshots in one well-defined, mechanical move.
- Step 11 is docs.

Every new-feature test from Step 5 onward **opts into an explicit layout**, so it
is independent of whatever the default is at the time. That is what lets the flip
sit safely at the end.

## Running through subagents

Each step below is written as a self-contained prompt a single subagent can
execute end to end: write failing tests, implement, run the build until green,
report. The dependency graph is mostly linear (each step builds on the last), with
two genuine parallel opportunities called out:

- **Step 1** (serializer file extension) has no dependencies. Run it first
  because later steps read `fileExtension()`.
- **Step 7** (value generator seam) depends only on base types and is independent
  of the layout chain (Steps 2 to 6). It can run in parallel with Steps 2 to 6.
- Everything else is sequential.

Each subagent must finish with a **green `./gradlew build` from `src/jvm`** (the
fast inner loop is `./gradlew testClasses`, then `./gradlew test`, then the full
`build` for coverage, Javadoc, and Spotless). It must never weaken the coverage
rule or skip Javadoc to go green.

---

## The blueprint, in chunks

**Phase 0: foundations (additive, default behaviour unchanged)**
1. Serializer declares its file extension.
2. `SnapshotLayout` value type and pure path resolver.
3. Config-file parsing into a `SnapshotLayout`.

**Phase 1: wire the new layout, opt-in only**
4. Teach `FileSnapshotStorage` the new layout path (selected internally, default
   still `Legacy`).
5. The configuration system: `Strictland` global fluent defaults, per-spec
   `SpecificationOptions`, the config file, and the precedence that merges them;
   first end-to-end opt-in tests.

**Phase 2: multiple snapshots per contract (manual)**
6. Manual variant labels through the serialize and deserialize paths.

**Phase 3: value generation and replay (the ambitious layer)**
7. `Variant` enum, `ValueGenerator` seam, dumb deterministic default,
   `RequiredFieldPolicy`.
8. `Variant` enum, `Snapshot.ByClass.variant(...)`, `given(Class)` auto-gen.
9. Replay-all on deserialization across a contract's variants.

**Phase 4: flip and finish**
10. Flip the default to `NextToTest + PerTestClass` and migrate the repo suite.
11. Documentation.

A natural release boundary sits after Step 6 (layouts plus manual variants solve
the original pain). Steps 7 to 9 are the optional, riskier value-generation layer
and could ship separately.

---

## Right-sized steps and the prompts

Each prompt assumes the working directory `src/jvm` and the conventions in
[CONTRIBUTING.md](CONTRIBUTING.md). TDD throughout: write the test first, watch
it fail, implement the minimum, refactor green.

### Step 1: Serializer declares its file extension

Adds `fileExtension()` to `MessageSerializer` with a safe default, and overrides
it in the three serializers. Nothing consumes it yet except its own tests, so the
default keeps every existing custom serializer compiling and the existing suite
untouched.

```text
You are working in the Strictland JVM package at src/jvm. Read CONTRIBUTING.md
first; the build enforces 100% line and branch coverage, Javadoc with -Xwerror,
NullAway, and Spotless (palantir-java-format). Work test-first.

Task: let a MessageSerializer declare the file extension its snapshots should use.

1. Add to src/main/java/io/eventdriven/strictland/MessageSerializer.java a new
   method:
     default String fileExtension() { return ".txt"; }
   Write Javadoc in the house style: open with what it is and why you would care
   (it names the snapshot file, e.g. ".json"), include a short {@snippet} pulled
   from a real test. Do not restate the signature. Keep the leading dot in the
   contract and say so.

2. Override it:
   - JacksonMessageSerializer -> ".json"
   - src/test/.../CsvMessageSerializer -> ".csv"
   - src/test/.../SimpleBinaryMessageSerializer -> ".bin"

3. Tests (write first): in a new or existing serializer test, assert each
   serializer returns its expected extension, and that a bare custom serializer
   (anonymous implementing only serialize/deserialize) returns the default
   ".txt". This covers the default method and every override.

Do not change any path-resolution or storage code in this step. Do not touch any
*.approved.txt files.

Finish with a green `./gradlew build` from src/jvm. Report the files changed and
the test names added.
```

### Step 2: `SnapshotLayout` value type and pure path resolver

A pure, fully unit-testable unit: given the layout plus caller facts plus message
type, variant, and extension, it returns the snapshot `Path`. No I/O, no stack
walking, no storage wiring. This is where every (strategy x grouping) combination
is proven, which is exactly what the coverage rule wants, and it is trivial for a
subagent to drive.

```text
You are working in the Strictland JVM package at src/jvm. Read CONTRIBUTING.md;
the build enforces 100% line and branch coverage, Javadoc with -Xwerror,
NullAway, and Spotless. Work test-first. The package is @NullMarked (JSpecify),
so annotate nullable references with @Nullable.

Task: add a pure snapshot-layout model and path resolver. No storage wiring yet.

1. New public types in io.eventdriven.strictland:
   - enum Grouping { PER_TEST_CLASS, PER_CONTRACT }
   - SnapshotLayout: an immutable value carrying
       strategy (NEXT_TO_TEST | GLOBAL_ROOT | LEGACY),
       Grouping grouping,
       String wrapperFolder (e.g. "snapshots"),
       String rootPath (used only by GLOBAL_ROOT, e.g.
         "src/test/resources/snapshots").
     Provide static factories nextToTest(), globalRoot(String rootPath),
     legacy(), and copy-on-write withers grouping(...), wrapperFolder(...).
     Defaults: grouping = PER_TEST_CLASS, wrapperFolder = "snapshots".

2. A pure resolver method (on SnapshotLayout or a package-private helper) with
   signature roughly:
     Path resolve(String callerPackage, String callerSimpleName,
                  String messageTypeName, @Nullable String variantLabel,
                  String fileExtension)
   Rules (return the committed approved-file path, including the
   ".snap.approved" + extension suffix):
   - Leaf name: variantLabel if present, else messageTypeName.
   - PER_TEST_CLASS group folder = callerSimpleName.
   - PER_CONTRACT group folder = messageTypeName; here the leaf defaults to
     messageTypeName when no variant is given.
   - NEXT_TO_TEST root = src/test/java/<callerPackageAsPath>/<wrapperFolder>.
   - GLOBAL_ROOT root = <rootPath>/<callerPackageAsPath>.
   - LEGACY = src/test/java/<callerPackageAsPath>/<leaf>.approved.txt
     (today's exact behaviour: no wrapper, no group folder, no .snap, .txt
     extension regardless of fileExtension). Keep LEGACY byte-identical to the
     current FileSnapshotStorage.resolve output.
   - Non-LEGACY filename = <leaf>.snap.approved<fileExtension>.

3. Tests first: a SnapshotLayoutTests covering every strategy x grouping
   combination, with and without a variant label, plus wrapperFolder and
   rootPath overrides, plus the LEGACY equivalence to today's path shape. Aim for
   full branch coverage of the resolver here, since later steps reuse it.

Javadoc every public member with a {@snippet} from these tests. Do not modify
FileSnapshotStorage, SpecificationOptions, or any *.approved.txt yet.

Finish with a green `./gradlew build`. Report types added and the combination
matrix the tests cover.
```

### Step 3: Config-file parsing into a `SnapshotLayout`

Parses `strictland.properties` keys into a `SnapshotLayout`, with an injectable
seam so tests pass `Properties`/`InputStream` directly. Critically, do NOT commit
a repo-wide `strictland.properties`: an auto-loaded classpath file would override
the layout for the entire suite and break everything mid-plan. The classpath
auto-load is covered once via a fixture loaded by stream, not by a default
resource.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first.

Task: parse a strictland.properties into a SnapshotLayout (built in Step 2).

1. Add a loader with an injectable seam, e.g.:
     SnapshotLayout fromProperties(Properties props)   // pure, testable
     Optional<SnapshotLayout> fromClasspath()           // reads
       "strictland.properties" if present, via fromProperties
   Recognised keys (all optional, fall back to SnapshotLayout defaults):
     strictland.layout.strategy   = nextToTest | globalRoot | flat
     strictland.layout.grouping   = perTestClass | perContract
     strictland.layout.wrapperFolder
     strictland.layout.rootPath
   Map "flat" -> LEGACY. Unknown values throw a clear IllegalArgumentException
   naming the key and the bad value.

2. CRITICAL: do not add a src/test/resources/strictland.properties to the repo.
   It would be auto-loaded and flip the whole suite. Test fromClasspath() by
   loading a fixture from a non-default resource name (e.g.
   src/test/resources/fixtures/layout-sample.properties) through the stream/
   Properties seam, so nothing global changes.

3. Tests first: each key parsed, defaults when a key is absent, every enum value,
   the flat->LEGACY mapping, and the invalid-value error path (branch coverage).

Javadoc public members with {@snippet}s. Do not wire this into SpecificationOptions
yet. Finish green with `./gradlew build`. Report keys handled and error cases.
```

### Step 4: Teach `FileSnapshotStorage` the new layout path

Plumbs a `SnapshotLayout` plus the serializer's extension into the storage and
adds the new-layout code path using the Step 2 resolver, selected by an internal
field that **defaults to `Legacy`**. Construct it directly in unit tests with a
layout; do not change `SpecificationOptions` yet, so the default and the existing
suite stay exactly as they are.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first.

Task: give FileSnapshotStorage a new-layout path while keeping LEGACY the default.

1. Add to FileSnapshotStorage a SnapshotLayout field and the serializer's file
   extension (pass them through a package-private constructor; keep the existing
   no-arg/default construction producing LEGACY behaviour so current callers are
   unchanged).

2. In store(...) and read(...), when the layout is non-LEGACY:
   - determine callerPackage and callerSimpleName via the existing StackWalker
     logic (reuse requireCaller / DSL_CLASSES);
   - compute the target Path with the Step 2 resolver, using the message type
     name passed in as `name` and the serializer extension;
   - drive ApprovalTests with Options.forFile().withExtension(<ext>)
     .withBaseName("<leaf>.snap") and a namer pointing at the resolved directory,
     creating parent folders as needed.
   When the layout is LEGACY, keep the current behaviour byte-for-byte.

3. Tests first: construct FileSnapshotStorage directly with a NEXT_TO_TEST and a
   GLOBAL_ROOT layout and assert the resolved/created file path and round-trip
   read. Cover both grouping modes. Keep these test snapshots under a clearly
   named throwaway test package or temp dir so they do not pollute the suite;
   committed new-layout snapshots are fine if placed under the new structure.

Do not modify SpecificationOptions defaults. Do not move existing *.approved.txt.
Finish green with `./gradlew build`. Report the new branches and how LEGACY parity
was preserved.
```

### Step 5: The configuration system (global fluent, per-spec, file, precedence)

Builds the full four-level configuration model and the single point that resolves
it, then adds the first true end-to-end opt-in tests. Every setting is
configurable globally in code, overridable per spec, with the file as the ambient
fallback. The default stays `Legacy` for now (the flip is Step 10).

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first.

Task: build the configuration system with precedence
  per-spec SpecificationOptions > global fluent (code) > strictland.properties
  (file) > built-in defaults.
Each setting resolves independently: the levels MERGE per setting, they do not
replace each other wholesale (a global value generator still applies when a spec
overrides only the layout).

1. Per-spec fluent: add SpecificationOptions.snapshotLayout(SnapshotLayout) as an
   immutable wither matching the existing copy-on-write style (alongside
   snapshotStorage(...) and messageTypeMapper(...)).

   Refactor needed first: today SpecificationOptions.serializer(...) EAGERLY sets
   storage and typeMapper to concrete defaults, so there is no way to tell "use
   the default" from "not set". Per-key fallthrough needs "unset" to be
   representable. Change the configurable settings to nullable/unset fields that
   default to null, stop eagerly defaulting in the factory, and resolve each
   setting once at the point the spec builds its storage (step 4 below). This is
   exactly how layered config works in Spring Boot and similar tooling: a source
   contributes only the keys it sets, and resolution happens on lookup. Keep the
   existing public behaviour identical for callers who never touch these settings
   (resolution still yields today's defaults). Update SpecificationOptionsTests
   accordingly.

2. Global fluent: add a process-wide config holder, e.g.
     Strictland.defaults()        // returns a mutable fluent config
       .snapshotLayout(...)
     Strictland.resetDefaults()    // restore built-ins
   Scope it to the snapshot layout for now; Step 8 adds valueGenerator(...) and
   requiredFieldPolicy(...) once those types exist (keeps Step 7 parallelisable).
   Keep it thread-safe enough for test use and resettable. Each setting is
   independently settable and independently unset by default.

3. File: reuse Step 3's fromClasspath()/fromProperties() as the file level. Do NOT
   commit a repo-wide strictland.properties (it would flip the whole suite); test
   the file level through the injectable seam / a non-default fixture.

4. Resolution: add ONE place (invoked when a spec builds its FileSnapshotStorage)
   that, per setting, takes the per-spec value if set, else the global value if
   set, else the file value if present, else the built-in default. Built-in
   default layout stays LEGACY in this step. Thread the resolved layout and the
   serializer's fileExtension into the storage.

5. Tests first:
   - precedence unit tests per setting: per-spec beats global beats file beats
     default; partial overrides merge (global generator + per-spec layout);
     resetDefaults() restores built-ins and leaves no cross-test leakage;
   - end-to-end opt-in tests in new test classes selecting NEXT_TO_TEST +
     PER_TEST_CLASS and GLOBAL_ROOT + PER_CONTRACT, pinning a sample message with
     .whenSerialized().thenContractIsUnchanged() and reading it back via a
     compatibility check. These commit a few snapshots under the new structure
     (snapshots/<TestClass>/... and src/test/resources/snapshots/...). Use
     Json.Jackson so the extension is .json. Any test that sets global defaults
     MUST reset them afterwards (e.g. @AfterEach) so order cannot affect others.

Javadoc every new public member with a {@snippet}. Do not change existing tests or
their snapshots. Finish green with `./gradlew build`. Report the global config
surface, the resolution order, and the snapshots created.
```

### Step 6: Manual variant labels

Makes multiple snapshots per contract first-class through the existing
serialize/deserialize paths, with no silent overwrite.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first.

Task: support manually labelled snapshot variants.

1. Snapshot API: add a factory for a labelled variant, e.g.
   Snapshot.variant(String label) returning a Snapshot the serialize path
   understands. The label becomes the leaf name (Step 2 resolver's variantLabel).

2. Thread the label:
   - GivenStep.whenSerialized(Snapshot) already exists; ensure a variant Snapshot
     flows into ThenContractStep and sets the leaf via the resolver.
   - ThenContractStep.snapshotName / the new-layout naming must use the label.
   - The deserialize read path (ThenCompatibilityStep.resolveSourceBytes) must be
     able to read a labelled variant back.
   Under LEGACY the label behaves like today's forMessageType leaf name.

3. Tests first (opt into NEXT_TO_TEST + PER_CONTRACT so variants sit together):
   write two variants of one message type, assert two distinct files exist and
   neither overwrote the other, and read one back by label. Add a case asserting
   the label is what gets recorded (documentation).

Javadoc new public members with {@snippet}s. Finish green with `./gradlew build`.
Report the variant files written.
```

### Step 7: `Variant` enum, `ValueGenerator` seam, dumb default, `RequiredFieldPolicy`

Independent of the layout chain. Pure and deterministic, tested in isolation.
**Can run in parallel with Steps 2 to 6.**

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first. The package is @NullMarked.

Task: add a deterministic value-generator seam with a deliberately dumb default.

1. Public types:
   - enum Variant { REQUIRED_ONLY, FULL }  (one enum, reused as both the
     predefined snapshot variant and the generator fill mode; there is NO
     separate Requiredness enum)
   - interface ValueGenerator { <T> T generate(Class<T> type, Variant variant); }
   - interface RequiredFieldPolicy { boolean isRequired(Field field); }  // keys
     off java.lang.reflect.Field so it covers record backing fields AND POJO
     fields
   - a default @Nullable-driven RequiredFieldPolicy: a field is optional when it
     carries a @Nullable annotation, otherwise required. Detect by annotation
     SIMPLE NAME ("Nullable") across field.getDeclaredAnnotations() and
     field.getAnnotatedType().getDeclaredAnnotations(), so it works for any
     nullability library without a hard dependency. A JSpecify @Nullable is a
     TYPE_USE annotation retained at runtime and propagates to a record's backing
     field. Do NOT special-case Optional<>: Optional as a field is an anti-pattern
     and never appears in a message contract.
   - a default dumb ValueGenerator.

2. The default generator MUST be byte-for-byte deterministic. Fixed values:
   String -> "string"; UUID -> 00000000-0000-0000-0000-000000000001; numeric ->
   a fixed constant; boolean -> false; Instant/date types -> a fixed epoch; enum
   -> first constant. Construction:
   - record (preferred): canonical constructor, filling components per Variant
     (optional components left null under REQUIRED_ONLY, filled under FULL);
   - POJO (supported): no-arg constructor, then set generated values into declared
     fields by reflection per Variant; if there is no usable no-arg constructor,
     throw a clear error naming the type and pointing at a manual variant.
   Handle nested records/POJOs recursively. Throw a clear exception for scalar
   types it cannot build, naming the type.

3. Tests first: generate a sample record AND a sample POJO both ways and assert
   exact field values; assert determinism (two calls equal); assert REQUIRED_ONLY
   leaves @Nullable fields empty while FULL fills them; cover the no-usable-
   constructor error and the unsupported-scalar error branches.

Do not wire this into the DSL yet (Step 8 does). Javadoc all public members with
{@snippet}s. Finish green with `./gradlew build`. Report supported types, how
@Nullable is detected, and the error paths.
```

### Step 8: `ByClass.variant(...)` and `given(Class)` auto-gen

Wires the Step 7 generator into the DSL: `given(Class)` writes the canonical
variants (using the Step 7 `Variant` enum), and a single variant can be targeted.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first. Depends on Steps 6 and 7.

Task: auto-generate canonical variants from a class.

1. Add fluent methods on Snapshot.ByClass<S> using the Variant enum from Step 7:
     ByClass<S> variant(Variant v)
     ByClass<S> variant(String label)
   Predefined leaf names: REQUIRED_ONLY -> "requiredOnly", FULL -> "allFilled".

2. Add MessageContract.given(Class<S>) as sugar for given(Snapshot.of(class)).
   When such a class-based serialize check runs without a specific variant, it
   auto-generates BOTH canonical variants (via the configured ValueGenerator +
   RequiredFieldPolicy), serializes each, and stores them under their variant leaf
   names. A specific .variant(Variant) targets just one.
   Collapse rule: when the two variants serialize to identical bytes (a type with
   no @Nullable fields), write ONE snapshot named after the type (no variant
   suffix) instead of two identical files.

3. Extend configuration with valueGenerator(...) and requiredFieldPolicy(...) at
   both levels: per-spec SpecificationOptions withers AND the Strictland global
   holder from Step 5. They resolve through the same precedence chain (per-spec >
   global > file-not-applicable-here > built-in Step 7 defaults). Thread the
   resolved pair to the serialize path. Any test setting global defaults resets
   them afterwards.

4. Tests first (opt into a layout): a type WITH a @Nullable field shows
   requiredOnly and allFilled differ (two files); a fully non-null type collapses
   to one file named after the type; a POJO auto-gens too; given(...).variant(
   Variant.REQUIRED_ONLY) writes only one. Assert deterministic content.

Javadoc all new public members with {@snippet}s. Finish green with
`./gradlew build`. Report files generated and the nullable-field difference.
```

### Step 9: Replay-all on deserialization

A class-based compatibility check replays every recorded variant of the contract.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. Work test-first. Depends on Steps 6 and 8.

Task: replay all variants of a contract on deserialization.

1. Add a way for storage to enumerate the recorded variants of a contract under
   the active layout. Add it to SnapshotStorage as a DEFAULT method so existing
   implementors keep compiling, e.g.:
     default Collection<byte[]> readAll(String name) {
       return read(name).map(List::of).orElse(List.of());
     }
   Implement the real listing in FileSnapshotStorage for the new layouts (list
   the contract/class folder for *.snap.approved.<ext>).

2. ThenCompatibilityStep: when the source is a Snapshot.ByClass WITHOUT a
   specific variant, read all variants and assert each deserializes as the target
   type (run the existing shared-field verification per variant). A ByClass WITH
   .variant(...) targets exactly one, as today.

3. Tests first: with two variants on disk, a backward-compatible check from the
   class replays both (prove both are read, e.g. via the extra consumer count or
   distinct field values); a single-variant check reads only one; the empty case
   (no variants found) fails clearly.

Javadoc the new default method and any new public member with {@snippet}s. Finish
green with `./gradlew build`. Report how many variants the replay covered.
```

### Step 10: Flip the default and migrate the repo suite

The single disruptive step, isolated and last. Mechanical: change the default,
move and rename the existing snapshots, update the few tests that name explicit
paths, regenerate, commit-ready.

```text
You are working in src/jvm. Read CONTRIBUTING.md; full coverage, Javadoc
-Xwerror, NullAway, Spotless. This step intentionally touches many files; do it
carefully and keep the build green at the end.

Task: make NEXT_TO_TEST + PER_TEST_CLASS the default and migrate existing
snapshots.

1. Change SpecificationOptions' built-in default layout from LEGACY to
   NEXT_TO_TEST + PER_TEST_CLASS (wrapperFolder "snapshots"). Keep LEGACY
   selectable; ensure at least one opt-in test still exercises LEGACY so its code
   path stays covered.

2. Migrate the repo's own snapshots in src/test/java/io/eventdriven/strictland:
   for each existing *.approved.txt that a test pins by default, move it under
   snapshots/<TestClassThatPinsIt>/ and rename to <leaf>.snap.approved.json (or
   the right extension for that test's serializer). The cleanest path is to
   delete the old files and re-run the tests so the AutoApproveWhenEmptyReporter
   regenerates them in the new location, then eyeball the diffs.

3. Update tests that reference explicit names or paths so they still resolve:
   - BackwardCompatibilityTests Snapshot.at(path, ...) and forMessageType(...)
     cases (hand-authored reference fixtures like CustomerRegisteredV1,
     AccountOpenedV1): move these reference files to a sensible home under the new
     structure (a GLOBAL_ROOT registry folder is a good fit) and update the
     paths/names accordingly;
   - MisuseTests path cases;
   - any test asserting a literal .approved.txt path.
   Update SnapshotPathResolverTests / ThenContractStepNamerTests if they assert
   the old shape.

4. Verify no stray *.approved.txt remain except those deliberately kept for the
   LEGACY opt-in test.

Finish with a fully green `./gradlew build` AND a clean `./gradlew check` across
compat-kotlin and compat-scala. Report every file moved, every test updated, and
confirm the LEGACY path is still covered.
```

### Step 11: Documentation

```text
You are working in the repo root and src/jvm. No production code changes.

Task: update docs to the new behaviour.

1. README.md: update the snapshot examples to the new file names and folder
   layout (snapshots/<TestClass>/<Type>.snap.approved.json), document the
   strictland.properties config file and its keys, document manual variants
   (Snapshot.variant), and document given(Class) auto-generation with the
   required/full variants. Keep the existing tone and the "use your own
   serializer" guidance.
2. CONTRIBUTING.md: note the new snapshots/ layout and that new tests should rely
   on the default layout.
3. Ensure every public Javadoc {@snippet} still points at a real test (the build
   already enforces this; just confirm).

Follow the writing conventions: precise plain English, no em dashes, lead with
what a thing is and why you would reach for it. Finish with a green
`./gradlew build` (Javadoc included). Report sections changed.
```

---

## Verification at each phase

Per the repo rule that every phase ends on a working state and a clean build:

- After each step: `./gradlew testClasses` (fast), then `./gradlew test`, then
  `./gradlew build` for coverage, Javadoc, and Spotless.
- After Step 10 only: also `./gradlew check` so the compat-kotlin and
  compat-scala consumers still build against the new API.
- Never lower the coverage threshold or skip Javadoc to pass. A coverage gap is a
  question about the code first (see CONTRIBUTING.md).

## Risks and notes

- The auto-loaded `strictland.properties` is a foot-gun during development: do not
  commit a repo-wide one before Step 10, or it will flip the whole suite. Step 3
  tests the classpath path via a non-default fixture for this reason.
- `SnapshotStorage` is public; Step 9's `readAll` must be a `default` method so
  custom implementors keep compiling.
- The value generator (Steps 7 to 9) is the riskiest area. Keep the default dumb
  and deterministic; resist scope creep into faker-style data here.
- The `Strictland` global config (Step 5) is process-wide mutable state, a
  foot-gun of its own: a test that sets it and forgets to `resetDefaults()` can
  change where another test's snapshots land, making failures order-dependent.
  Every test that mutates it must reset it (e.g. `@AfterEach`). Prefer set-once
  for real projects.
- Predefined variant leaf names are `requiredOnly` and `allFilled` (Step 8), set
  in one place so they are easy to change.
