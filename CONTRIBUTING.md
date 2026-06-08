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

The quickest start is the **dev container** — it ships with the JDK, the recommended IDE extensions,
and the Git hooks already configured:

1. Install [Docker](https://docs.docker.com/engine/install/) and
   [VS Code](https://code.visualstudio.com/) with the **Dev Containers** extension.
2. Open the repo and run **Dev Containers: Reopen in Container**.

To work on your own machine instead, install **JDK 26** (we use Temurin) — that's the version the
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
./gradlew testClasses   # compile only — the fast inner loop
./gradlew test          # run the tests
./gradlew spotlessApply # reformat sources by hand, if you ever need to
```

These are also available as VS Code tasks (_Ctrl+Shift+P → Run Task_); **compile** is the default
build (Ctrl+Shift+B).

### Editor setup

**VS Code** — open the workspace file rather than the folder: `code strictland.code-workspace`. Accept
the recommended extensions when prompted (or run the **Install All Recommended Extensions** task).
Format-on-save then matches the build exactly, so your changes stay clean as you type.

**IntelliJ IDEA** — import `src/jvm` as a Gradle project and accept the prompt to install the
**palantir-java-format** and **Scala** plugins. Enabling palantir-java-format keeps editor formatting
in step with the build.


Stuck on any of this? Ask us on the [Discord channel](https://discord.gg/fTpqUTMmVa) — we're happy to help.

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
from those languages — they are not published.


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

- **Java** — [palantir-java-format](https://github.com/palantir/palantir-java-format)
- **Kotlin** — `ktfmt` (and `ktlint` for Gradle scripts)
- **Scala** — `scalafmt`, configured by [`.scalafmt.conf`](./src/jvm/.scalafmt.conf)

The editor integrations and the pre-commit hook both defer to this same config, so once your
environment is set up you rarely touch it directly — `./gradlew spotlessApply` reformats everything,
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
([JSpecify](https://jspecify.dev/)) — so annotate nullable references explicitly.

## Licensing and legal rights

By contributing to Strictland:

1. You assert that contribution is your original work.
2. You assert that you have the right to assign the copyright for the work.

## Code of Conduct

This project has adopted the code of conduct defined by the [Contributor Covenant](http://contributor-covenant.org/) to clarify expected behavior in our community.
