# Strictland

Contract-testing library for message compatibility. See `README.md` for what it does. Project structure, setup, code style, and the tests, coverage, and documentation sections are copied from `CONTRIBUTING.md` without changes. Keep them in sync with that file.

## Project structure

The repository groups source by language:

```
src/
└── jvm/   the JVM package (this guide); 
```

### JVM package structure

Within the JVM package, the Gradle root project **is** the published library
(`io.event-driven:strictland`):

```
src/jvm/
├── build.gradle.kts            the library: toolchain, tests, static analysis, publishing
├── gradle/libs.versions.toml   centralized versions
├── src/                        io.eventdriven.strictland sources & tests
├── compat-kotlin/              Kotlin smoke test (consumes the library, not published)
└── compat-scala/               Scala smoke test (consumes the library, not published)
```

The `compat-kotlin` and `compat-scala` subprojects only prove the jar is consumable and null-correct
from those languages - they are not published.

## Setup

### JVM

**JVM** package is located in [`src/jvm`](./src/jvm).

The quickest start is the **dev container** - it ships with the JDK, the recommended IDE extensions,
and the Git hooks already configured:

1. Install [Docker](https://docs.docker.com/engine/install/) and
   [VS Code](https://code.visualstudio.com/) with the **Dev Containers** extension.
2. Open the repo and run **Dev Containers: Reopen in Container**.

To work on your own machine instead, install **JDK 27** (we use Temurin) - that's the version the
build compiles with. Gradle fetches any other JDK it needs on its own (for example JDK 21, which the
tests also run against), so you don't have to manage them by hand.

Then build and test from the package:

```shell
cd src/jvm
./gradlew build
```

#### Everyday commands

All from [`src/jvm`](./src/jvm):

```shell
./gradlew build         # compile, test, and check everything
./gradlew testClasses   # compile only - the fast inner loop
./gradlew test          # run the tests
./gradlew spotlessApply # reformat sources by hand, if you ever need to
```

These are also available as VS Code tasks (_Ctrl+Shift+P → Run Task_); **compile** is the default
build (Ctrl+Shift+B).

### Editor setup

**VS Code** - open the workspace file rather than the folder: `code strictland.code-workspace`. Accept
the recommended extensions when prompted (or run the **Install All Recommended Extensions** task).
Format-on-save then matches the build exactly, so your changes stay clean as you type.

**IntelliJ IDEA** - import `src/jvm` as a Gradle project and accept the prompt to install the
**palantir-java-format** and **Scala** plugins. Enabling palantir-java-format keeps editor formatting
in step with the build.

### Additional commands

Run from `src/jvm`:

```shell
./gradlew test --tests 'io.eventdriven.strictland.SerializationContractTests' # run one test class
./gradlew approveSnapshots # promote every drifted *.snap.received over its approved sibling
```

Run `approveSnapshots` only when the snapshot change is intended and approved.

## Code style

### JVM code style
Formatting is handled for you by [Spotless](https://github.com/diffplug/spotless), with the Gradle
build as the single source of truth:

- **Java** - [palantir-java-format](https://github.com/palantir/palantir-java-format)
- **Kotlin** - `ktfmt` (and `ktlint` for Gradle scripts)
- **Scala** - `scalafmt`, configured by [`.scalafmt.conf`](./src/jvm/.scalafmt.conf)

The editor integrations and the pre-commit hook both defer to this same config, so once your
environment is set up you rarely touch it directly - `./gradlew spotlessApply` reformats everything,
and `./gradlew spotlessCheck` (part of `build`) flags any drift.

To get the pre-commit hook on your own machine, point Git at the shared hooks once (the dev container
already does this for you):

```shell
git config core.hooksPath .githooks
```

It runs the formatter on staged sources before each commit, so style stays consistent without you
thinking about it.

The build also runs [Error Prone](https://errorprone.info/) with
[NullAway](https://github.com/uber/NullAway), and the public API is `@NullMarked`
([JSpecify](https://jspecify.dev/)) - so annotate nullable references explicitly.

## Tests, coverage, and documentation

Alongside formatting and static analysis, `./gradlew build` checks two more things: full test coverage and a documented public API. Both run under `check`, so a pull request that drops either won't go green. The existing tests and comments show what's expected, so neither needs to slow you down.

### 100% line and branch coverage

The build requires 100% line and branch coverage ([`jacocoTestCoverageVerification`](./src/jvm/build.gradle.kts)). This does more than confirm the tests run the code they're meant to. It also surfaces code no test can reach. A branch you can't cover is often one that can't execute: a null check on a value that's never null, or a `default` case the type already rules out. When that's the case, the better fix is usually to remove the branch, not to write a test that only satisfies the check. So treat a coverage gap as a question about the code first, and the test second.

We reach the coverage from the outside in. Tests drive the public DSL the same way calling code does: `given(...).whenSerialized().thenContractIsUnchanged()`. The internals are covered because real usage runs through them, not because a test reaches into a private method. Tests written this way survive refactors and read as examples of the API. [`SerializationContractTests`](./src/jvm/src/test/java/io/eventdriven/strictland/SerializationContractTests.java) is a good place to start. The compatibility suites [`BackwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/BackwardCompatibilityTests.java) and [`ForwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/ForwardCompatibilityTests.java) follow the same shape.

### Documented public API

The public API is what other code is written against. Its documentation carries the intent a signature can't: what a type is for, when you'd reach for it, how the pieces fit. That's why we document all of it.

`check` runs Javadoc with warnings treated as errors ([`-Xwerror`](./src/jvm/build.gradle.kts)), so every public and protected member needs a comment or the build fails. Each one is written for the reader calling it. It opens with what the thing is and why you'd reach for it, then shows a short example pulled from a real test with `{@snippet}`. It doesn't restate the signature. The contract DSL steps are a good illustration: their comments say what each stage checks and show the call in context. The members already documented are the easiest guide to the tone.

Writing the comment also tells you something about the API. A member you can't describe in a plain sentence is often one whose design is worth a second look while it's still new.

## Agent skills

Shared skills live in `.agents/skills/<name>/SKILL.md`. That file is the canonical body, so edit the skill there.

Codex and OpenCode read `.agents/skills/` directly. Claude Code does not, so `.claude/commands/<name>.md` holds a thin pointer that reads the skill file. OpenCode also gets a pointer in `.opencode/commands/<name>.md`, which exposes the skill as a `/name` slash command.

When adding a skill, write the body in `.agents/skills/` with `name` and `description` in the front matter, then add both pointer files.

See `.agents/README.md` for how skills and the change-check hooks work. Run the hook tests from the repository root with `node --test .agents/hooks/*.spec.ts .opencode/plugins/*.spec.ts`.

## Verification

The completion hook runs `./gradlew spotlessApply` and `./gradlew testClasses` automatically in each Gradle build (a folder with `gradlew`, today `src/jvm`) edited in the session.

For documentation or agent-configuration changes, validate only the changed files and configuration. The Gradle build is not required.

While changing library code, identify and run the smallest relevant set of tests covering the affected behavior. Before reporting a development phase as complete, run `./gradlew build` from `src/jvm`. If a build is interrupted, ask before running it again.

Report which verification commands were run, whether they passed, and why any relevant checks were skipped.

## Working rules

- Use simple, direct, straightforward language (STE English). Keep explanations concise, example-based, and focused on the problem. Simplify without oversimplifying.
- If a requirement or intended behavior is unclear, ask instead of assuming.
- Do not monkey-patch globals, prototypes, or third-party modules. If a change appears to require monkey-patching, stop and ask first.
- Prefer simple, easily removable code over premature abstractions. Do not introduce a new abstraction unless explicitly requested.
- Do not use comments inside the code unless asked. If you want to do it, review code and make it self-explanatory instead. Javadoc on the public API is required, see "Documented public API".
- Use a test-first approach for behavior changes. Name tests after observable behavior from the usage perspective, not implementation details.
- Treat questions, design discussions, and requests for opinions as read-only. Wait for explicit approval before editing files or running mutating commands.
- Never reverse a decision already agreed to (a requirement, a behaviour, a test that guards it) unless explicitly approved by that specific reversal. Ask user and explain tradeoffs.
- Plan to use subagents, when available, for independent, non-sequential work that can usefully run in parallel, such as research, repository exploration, focused implementation, verification, and review. Keep user interaction, cross-cutting decisions, and final synthesis in the main context. Ask for that usage before sending them. Don't start subagents with inherit context. Give subagents sufficient context and require concrete, evidence-backed findings so important details are not lost.
- Do not commit, create branches or tags, rebase, reset, or otherwise modify Git history unless explicitly requested.
- Do not add manual line breaks inside Markdown paragraphs. Keep each paragraph on a single line and let the renderer or editor wrap it.
