# Contributing to Strictland

We take Pull Requests!

## Before you send Pull Request

1. Contact the maintainers via the [Discord channel](https://discord.gg/fTpqUTMmVa) or the [Github Issue](https://github.com/event-driven-io/strictland/issues/new) to make sure that this is issue or bug should be handled with proposed way. Send details of your case and explain the details of the proposed solution.
2. Once you get approval from one of the maintainers, you can start to work on your code change.
3. After your changes are ready, make sure that you covered your case with automated tests and verify that you have limited the number of breaking changes to a bare minimum.
4. We also highly appreciate any relevant updates to the documentation.
5. Make sure that your code is compiling and all automated tests are passing.

## After you have sent Pull Request

1. Make sure that you applied or answered all the feedback from the maintainers.
2. We're trying to be as much responsive as we can, but if we didn't respond to you, feel free to ping us on the [Discord channel](https://discord.gg/fTpqUTMmVa).
3. Pull request will be merged when you get approvals from at least one of the maintainers (and no rejection from others). Pull request will be tagged with the target Strictland version in which it will be released. We also label the Pull Requests with information about the type of change.

## Setup your work environment

Strictland is a monorepo with one package per language under [`src/`](./src/). 

### JVM

**JVM** package is located in [`src/jvm`](./src/jvm).

The quickest start is the **dev container** - it ships with the JDK, the recommended IDE extensions,
and the Git hooks already configured:

1. Install [Docker](https://docs.docker.com/engine/install/) and
   [VS Code](https://code.visualstudio.com/) with the **Dev Containers** extension.
2. Open the repo and run **Dev Containers: Reopen in Container**.

To work on your own machine instead, install **JDK 26** (we use Temurin) - that's the version the
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


Stuck on any of this? Ask us on the [Discord channel](https://discord.gg/fTpqUTMmVa) - we're happy to help.

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


## Working with the Git

1. Fork the repository.
2. Create a feature branch from the `main` branch.
3. We're not squashing the changes and using rebase strategy for our branches (see more in [Git documentation](https://git-scm.com/book/en/v2/Git-Branching-Rebasing)). Having that, we highly recommend using clear commit messages. Commits should also represent the unit of change.
4. Before sending PR to make sure that you rebased the latest `main` branch from the main Strictland repository.
5. When you're ready to create the [Pull Request on GitHub](https://github.com/event-driven-io/strictland/compare).

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

## Licensing and legal rights

By contributing to Strictland:

1. You assert that contribution is your original work.
2. You assert that you have the right to assign the copyright for the work.

## Code of Conduct

This project has adopted the code of conduct defined by the [Contributor Covenant](http://contributor-covenant.org/) to clarify expected behavior in our community.
