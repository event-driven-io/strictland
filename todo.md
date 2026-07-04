# Todo: configurable snapshot layouts and multiple snapshots per contract

Tracks [plan.md](plan.md). Each step ends on a green `./gradlew build` from
`src/jvm` (100% coverage, Javadoc -Xwerror, NullAway, Spotless). Check a step
only when the build is green.

## Phase 0: foundations (additive, default unchanged)

- [x] **Step 1: serializer file extension** (no deps, do first)
  - [x] `MessageSerializer.fileExtension()` default `.txt` + Javadoc
  - [x] override: Jackson `.json`, CSV `.csv`, binary `.bin`
  - [x] tests: each override + the bare-default case
- [x] **Step 2: `SnapshotLayout` + pure resolver**
  - [x] `Grouping` enum, `SnapshotLayout` value type, factories + withers
  - [x] pure `resolve(...)` for every strategy x grouping, `(messageType, snapshotName)`, ext
  - [x] FLAT output byte-identical to today's path
  - [x] tests: full combination matrix, branch-complete
- [x] **Step 3: config-file parsing**
  - [x] `fromProperties(Properties)` + `fromClasspath()` seam
  - [x] keys: strategy / grouping / wrapperFolder / rootPath; `flat` -> FLAT
  - [x] invalid-value error path
  - [x] tests via non-default fixture (NO repo-wide strictland.properties)

## Phase 1: wire the layout, opt-in only

- [x] **Step 4: resolution out of storage + ApprovalTests removed**
  - [x] resolution moved to `SnapshotLocation` (caller discovery + layout); `FileSnapshotStorage` is a plain path sink
  - [x] plain write/compare storage: first-run approve, compare, `.snap.received` on drift (no ApprovalTests, no duplicate-tracker)
  - [x] clean-room `TestSourceDirectoryLocator`: known source roots + JVM source-file name, no tree scan; `Strictland.defaults().testSourceRoots(...)` override
  - [x] identity is `(messageType, snapshotName)`; variant folded in the DSL, layout + storage variant-unaware (no `SnapshotKeys`)
  - [x] FLAT parity preserved byte-for-byte
  - [x] ApprovalTests dropped from the library AND the test suite (PublicApiContractTests dogfoods `FileSnapshotStorage`)
  - [x] tests: NEXT_TO_TEST + GLOBAL_ROOT, both groupings, round-trip; locator java/kotlin/scala roots + override + miss
- [x] **Step 5: configuration system (global fluent + per-spec + file + precedence)**
  - [x] refactor `SpecificationOptions`: stop eager defaulting, settings carry "unset" (nullable), resolve on lookup
  - [x] `SpecificationOptions.snapshotLayout(...)` immutable wither, "unset" falls through
  - [x] `Strictland.defaults()` global fluent holder (layout) + `resetDefaults()`
  - [x] one resolution point: per-spec > global > file > built-in, merged per setting
  - [x] precedence + reset/no-leak unit tests
  - [x] end-to-end opt-in tests (NEXT_TO_TEST, GLOBAL_ROOT) with committed snaps; tests reset globals

## Phase 2: multiple snapshots per contract

- [x] **Step 6: manual variant labels**
  - [x] `SnapshotVariant.named(String)` (and `Snapshot.ByVariant`)
  - [x] label folds into snapshotName on serialize + deserialize read; storage stays variant-unaware
  - [x] tests: two variants, no overwrite, read one by label, label recorded (`ManualVariantTests`)

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
  - [ ] keep a FLAT opt-in test so that path stays covered
  - [ ] move/rename ~30 existing snapshots into the new structure
  - [ ] relocate hand-authored reference fixtures (CustomerRegisteredV1, etc.)
  - [ ] update tests naming explicit paths (BackwardCompatibility, Misuse, namers)
  - [ ] no stray *.approved.txt except the FLAT opt-in
  - [ ] green `./gradlew build` AND `./gradlew check` (kotlin/scala compat)
- [ ] **Step 11: docs**
  - [ ] README: new layout, config file, variants, given(Class)
  - [ ] CONTRIBUTING: new snapshots/ layout note
  - [ ] confirm Javadoc snippets still resolve

## Snapshot review devex (diff + approve)

Restores the diff-tool and approval experience dropped with ApprovalTests, behind one
`SnapshotReview` setting that flows through the same config chain as `SnapshotLayout`.

- [x] **Inline diff** on every drift: `SnapshotDiff` embeds a unified diff (hex/length fallback for
  binary) in the `AssertionError`, so CI logs are self-explanatory.
- [x] **Local diff-tool launch**: private diff-tool registry + resolved launcher, gated off on CI/headless/Linux-without-display by `CiDetector`; manual pick via
  `SnapshotReview.tool(...)` or `strictland.review.tool`, and preferred auto order via
  `SnapshotReview.toolOrder(...)`, `strictland.review.toolOrder`, or `STRICTLAND_REVIEW_TOOL_ORDER`.
- [x] **Approve mode**: `SnapshotReview.approve()` re-baselines on drift; drive per-run with
  `-Dstrictland.review.mode=approve`, or sweep the registry with `SnapshotApprove` /
  `./gradlew approveSnapshots`.
- [x] **Config chain**: runtime properties/env override per-spec, global, file, and built-in defaults;
  `SpecificationOptions.snapshotReview(...)` and `Strictland.defaults().snapshotReview(...)` stay as the public entry points.
- [x] **Docs**: README "Reviewing a drift" section (workflow, tool table, settings, bulk approve).

## Release boundaries

- After **Step 6**: layouts + manual variants. Solves the original bloat and
  collision pain. Shippable on its own.
- After **Step 9**: full value-generation layer (the optional, riskier part).
- **Step 10/11**: flip + docs land the new default and the documentation.
