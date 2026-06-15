# Todo: configurable snapshot layouts and multiple snapshots per contract

Tracks [plan.md](plan.md). Each step ends on a green `./gradlew build` from
`src/jvm` (100% coverage, Javadoc -Xwerror, NullAway, Spotless). Check a step
only when the build is green.

## Phase 0: foundations (additive, default unchanged)

- [ ] **Step 1: serializer file extension** (no deps, do first)
  - [ ] `MessageSerializer.fileExtension()` default `.txt` + Javadoc
  - [ ] override: Jackson `.json`, CSV `.csv`, binary `.bin`
  - [ ] tests: each override + the bare-default case
- [ ] **Step 2: `SnapshotLayout` + pure resolver**
  - [ ] `Grouping` enum, `SnapshotLayout` value type, factories + withers
  - [ ] pure `resolve(...)` for every strategy x grouping, variant, ext
  - [ ] LEGACY output byte-identical to today's path
  - [ ] tests: full combination matrix, branch-complete
- [ ] **Step 3: config-file parsing**
  - [ ] `fromProperties(Properties)` + `fromClasspath()` seam
  - [ ] keys: strategy / grouping / wrapperFolder / rootPath; `flat` -> LEGACY
  - [ ] invalid-value error path
  - [ ] tests via non-default fixture (NO repo-wide strictland.properties)

## Phase 1: wire the layout, opt-in only

- [ ] **Step 4: layout-aware `FileSnapshotStorage`**
  - [ ] thread layout + serializer extension; internal default LEGACY
  - [ ] new-layout store/read via Step 2 resolver + ApprovalTests withExtension
  - [ ] LEGACY parity preserved byte-for-byte
  - [ ] tests: NEXT_TO_TEST + GLOBAL_ROOT, both groupings, round-trip
- [ ] **Step 5: configuration system (global fluent + per-spec + file + precedence)**
  - [ ] refactor `SpecificationOptions`: stop eager defaulting, settings carry "unset" (nullable), resolve on lookup
  - [ ] `SpecificationOptions.snapshotLayout(...)` immutable wither, "unset" falls through
  - [ ] `Strictland.defaults()` global fluent holder (layout) + `resetDefaults()`
  - [ ] one resolution point: per-spec > global > file > built-in, merged per setting
  - [ ] precedence + reset/no-leak unit tests
  - [ ] end-to-end opt-in tests (NEXT_TO_TEST, GLOBAL_ROOT) with committed snaps; tests reset globals

## Phase 2: multiple snapshots per contract

- [ ] **Step 6: manual variant labels**
  - [ ] `Snapshot.variant(String)`
  - [ ] label threads into serialize naming + deserialize read
  - [ ] tests: two variants, no overwrite, read one by label, label recorded

## Phase 3: value generation and replay (can start Step 7 in parallel with 2-6)

- [ ] **Step 7: `Variant` enum + `ValueGenerator` seam + dumb default + `RequiredFieldPolicy`**
  - [ ] `Variant` enum (REQUIRED_ONLY/FULL, reused as fill mode; no separate Requiredness)
  - [ ] `ValueGenerator`, `RequiredFieldPolicy` (keys off `Field`: records + POJOs)
  - [ ] default policy: `@Nullable` by simple name across field + type-use; NO Optional handling
  - [ ] deterministic dumb generator: records (canonical ctor, preferred) + POJOs (no-arg + reflective set), nested
  - [ ] tests: record + POJO, both modes, determinism, nullable handling, no-usable-ctor + unsupported-scalar errors
- [ ] **Step 8: `ByClass.variant(...)` + `given(Class)` auto-gen**
  - [ ] leaf names `requiredOnly` / `allFilled`
  - [ ] `ByClass.variant(Variant)` and `.variant(String)`
  - [ ] `MessageContract.given(Class)` sugar -> generate both canonical variants
  - [ ] collapse rule: identical variants (no @Nullable fields) -> one file named after the type
  - [ ] `valueGenerator(...)` + `requiredFieldPolicy(...)` on BOTH per-spec options and `Strictland.defaults()`
  - [ ] tests: nullable diff (two files), collapse (one file), POJO, single-variant targeting
- [ ] **Step 9: replay-all on deserialization**
  - [ ] `SnapshotStorage.readAll(...)` as a `default` method
  - [ ] `FileSnapshotStorage` lists a contract's variants
  - [ ] ByClass-without-variant replays all; with-variant targets one
  - [ ] tests: replay both, single target, empty-found error

## Phase 4: flip and finish

- [ ] **Step 10: flip default + migrate repo suite** (single disruptive step)
  - [ ] default -> NEXT_TO_TEST + PER_TEST_CLASS
  - [ ] keep a LEGACY opt-in test so that path stays covered
  - [ ] move/rename ~30 existing snapshots into the new structure
  - [ ] relocate hand-authored reference fixtures (CustomerRegisteredV1, etc.)
  - [ ] update tests naming explicit paths (BackwardCompatibility, Misuse, namers)
  - [ ] no stray *.approved.txt except the LEGACY opt-in
  - [ ] green `./gradlew build` AND `./gradlew check` (kotlin/scala compat)
- [ ] **Step 11: docs**
  - [ ] README: new layout, config file, variants, given(Class)
  - [ ] CONTRIBUTING: new snapshots/ layout note
  - [ ] confirm Javadoc snippets still resolve

## Release boundaries

- After **Step 6**: layouts + manual variants. Solves the original bloat and
  collision pain. Shippable on its own.
- After **Step 9**: full value-generation layer (the optional, riskier part).
- **Step 10/11**: flip + docs land the new default and the documentation.
