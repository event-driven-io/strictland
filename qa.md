# Brainstorm Q&A: Multiple snapshot location strategies

## Context

Today `FileSnapshotStorage.resolve()` walks the stack to find the test class,
turns its package into a path, and writes
`src/test/java/<package>/<snapshotName>.approved.txt`. The snapshot name
defaults to the message's simple class name. Result: ~30 `.approved.txt` files
sit flat alongside the `.java` files in one package directory, and two tests
pinning the same message class collide unless one passes
`Snapshot.ofTypeNamed(...)`.

Two distinct problems are tangled together:
1. **Where** snapshot files physically live (the location strategy).
2. **How many** snapshots one contract can legitimately have, and how they are
   named and selected on read-back (e.g. required-only vs all-fields-filled).

---

## Q1. The current flat "next to the test" layout: keep as default, keep selectable but change default, or remove?

**Answer:** It should stay selectable, but it's undecided whether it remains the
default (probably not). Possibly keep the flat layout but with adjusted file
names to make snapshots easier to locate.

---

## Key finding: two kinds of snapshot

Investigation showed `CustomerRegisteredV1.approved.txt` and
`AccountOpenedV1.approved.txt` are never written by any test (no
`new CustomerRegisteredV1(...)` exists). They are hand-authored reference
fixtures read by `BackwardCompatibilityTests` to prove today's code still parses
older bytes. So the suite holds two kinds with opposite needs:

- **Owned snapshots** — produced and asserted by `whenSerialized()
  .thenContractIsUnchanged()`. Auto-generated, regenerated on change, belong to
  one test. Want **locality + collision-safety**.
- **Reference snapshots** — hand-authored historical bytes, only read by
  compatibility tests, sometimes by a different class than pins the type. Want a
  **stable, discoverable, shareable name** (a registry).

Two independent axes emerged:
1. **Grouping namespace** — flat / folder-per-test-class /
   folder-per-message-type / global-registry-mirroring-package.
2. **Leaf name** — message type / test method / message type + variant label.
   The multi-snapshot case (required-only vs all-filled) is solved purely on
   axis 2 (a variant label), independently of where folders sit.

---

## Q2. Within a test class, what identifies each snapshot (the leaf naming key)?

**Answer (discussion):** Unsure which is best, wants to see tradeoffs.
Message-centric naming is better as documentation and lets multiple test files
reuse/share a snapshot. File/test-centric is better to avoid collisions. A
subfolder would make it cleaner instead of an explosion of tiny files.

## Q3. Which grouping namespace (axis 1): folder-per-test-class, folder-per-message-type, or both-by-role?

**Answer:** Offer **both options** — folder per class OR folder per contract
(message type), configurable. Also: snapshot files should not end as `.txt` but
carry the real format extension (`.json`, `.avro`, etc.), probably with a
`.snap` marker segment in the name.

## Q4. Filename convention (resolved, orthogonal to folder structure)

**Answer:** ApprovalTests is NOT locked to `.txt` (confirmed: ApprovalTests
24.0.0 exposes `Options.forFile().withExtension(".json")` and `withBaseName`).
Keep the `approved`/`received` verbs, add a `.snap` part, and use the proper
format extension. Result: committed `OrderInitiated.snap.approved.json`, and a
transient `OrderInitiated.snap.received.json` on mismatch. The format extension
is declared by the serializer (new `MessageSerializer` capability: e.g. `.json`
for Jackson, `.csv` for CSV, `.bin`/`.avro` for others). Filename is orthogonal
to the folder-structure discussion.

## Q5. Root location: next-to-test, global registry root, or both configurable?

**Answer:** Both, configurable. The resulting matrix:
- `strategy = NextToTest | GlobalRoot`
- `grouping = PerTestClass | PerContract`
- `wrapperFolderName` configurable (e.g. `snapshots` / `messagecontracts`)
- `rootPath` configurable (for GlobalRoot, e.g. `src/test/resources/snapshots`)
- legacy Flat layout stays selectable (default still TBD)
- filename `<base>.snap.approved.<ext>` from Q4

## Q6. Multi-snapshot scope: manual variants, also auto-generate, or defer?

**Answer:** A mixture of manual variants AND auto-generation, configurable. Be
prepared for a value generator NOW: design the seam, ship the dummiest possible
default generator, then make it smarter later (faker.js / Bogus-style, or reuse
an external package). So:
- variant labels make manual multi-snapshots first-class (no overwrite,
  recorded as documentation), with replay-all on deserialization;
- `given(Class)` auto-generates canonical variants (required-only, all-filled)
  via a pluggable `ValueGenerator` whose default is intentionally dumb;
- the generator is an extension seam meant to grow.

**Hard constraint:** generated snapshots must be byte-for-byte deterministic
across runs and machines (else `thenContractIsUnchanged` flaps). The dummy
default emits fixed values per type (`String -> "string"`, `UUID ->
0000…0001`, `Instant ->` fixed epoch). Any future faker/Bogus-style generator
must be seeded.

## Q7. How does "required-only" decide which fields to leave empty?

**Answer:** Nullability-driven (fill all components, leave `@Nullable`/`Optional`
empty) is the DEFAULT and the only policy implemented for now. Expose a
pluggable `RequirednessPolicy` seam so other policies (Bean Validation `@NotNull`,
custom) can be added later without modifying core. Open-Closed Principle.

## Q8. Configuration model: fluent API, config file, or both?

**Answer:** Both. A config file in test resources sets project-wide defaults,
loaded automatically; the fluent `SpecificationOptions` API overrides per-spec.
Precedence: fluent API > config file > built-in defaults.

**Decision (format):** zero new dependencies, so `strictland.properties` on the
test classpath via `java.util.Properties` (Strictland core must not depend on
Jackson, since custom serializers exist). A future JSON variant can be added
behind the same loader.

## Q9. Default layout and migration?

**Answer:** Default = `NextToTest + PerTestClass` (wrapped in a `snapshots`
folder). No auto-migrator requested. Strictland is pre-1.0, so the default
change is acceptable; existing users opt into `Flat` or move files. The repo's
own ~30 snapshots get relocated as part of this work.

## Q10. DSL surface: variant labels and auto-generation entrypoint?

**Answer:**
- Manual labelled variant via the Snapshot route: `SnapshotVariant.named("IsoDate")`,
  used in `whenSerialized(SnapshotVariant.named("IsoDate"))`.
- Auto-generation reuses `Snapshot.ByClass<S>` with a fluent `.variant(...)`:
  `Snapshot.of(OrderPlaced.class).variant(variant)`. The variant is an **enum**
  for the predefined canonical ones (e.g. `Variant.REQUIRED_ONLY`,
  `Variant.FULL`) and/or **string constants** for custom labels.
- Add syntactic sugar `given(Class)` as shorthand for
  `given(Snapshot.of(Class))` (auto-generates the predefined variant set).
