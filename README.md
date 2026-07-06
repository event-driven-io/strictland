[![](https://dcbadge.vercel.app/api/server/fTpqUTMmVa?style=flat)](https://discord.gg/fTpqUTMmVa)[![Github Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&link=https://github.com/sponsors/event-driven-io)](https://github.com/sponsors/event-driven-io) [![blog](https://img.shields.io/badge/blog-event--driven.io-brightgreen)](https://event-driven.io/?utm_source=event_sourcing_nodejs) [![blog](https://img.shields.io/badge/%F0%9F%9A%80-Architecture%20Weekly-important)](https://www.architecture-weekly.com/?utm_source=event_sourcing_nodejs) [<img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20px" />](https://www.linkedin.com/in/oskardudycz/) 

# Strictland - contract testing for your messages compatibility

![](./assets/logo.png)

**Strictland is a contract-testing library** for the messages your code sends and stores: events, commands, queue messages, HTTP requests and responses, and anything else you serialize for someone else to read.

**You write a small unit test that locks down a message's format.** Later you rename a field, change a type, or adjust how a value serializes; the code still compiles and your other tests pass, but that one fails and points at what changed. You fix it in your build, before a consumer or a stored event has hit the old format in production.

When a message changes by accident, a snapshot check shows you exactly what moved. When you evolve a message on purpose, a compatibility check confirms an old and a new version can still read each other's data.

Every check starts from `MessageContract` and reads as a sentence:

```java
@Test
void ensureOrderPlacedCompatibilityWithNewerVersion() {
    // Strictland specification
    MessageContract.specification(Json.Jackson.of(yourObjectMapper))
        .given(new OrderPlaced(orderId, "Alice"))
        .whenDeserializedAs(OrderPlacedWithCoupon.class)
        .thenBackwardCompatible();
}
```

## Getting started

Strictland is on Maven Central as `io.event-driven:strictland`. It runs on JDK 21 or newer.

Gradle (Kotlin DSL):

```kotlin
testImplementation("io.event-driven:strictland:0.3.0")
```

Maven:

```xml
<dependency>
  <groupId>io.event-driven</groupId>
  <artifactId>strictland</artifactId>
  <version>0.3.0</version>
  <scope>test</scope>
</dependency>
```

Then add new test with:

```java
MessageContract.specification(Json.Jackson.defaults())
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

The first run serializes the message and writes the result to an approved file named after the class, [`OrderPlaced.approved.txt`](./src/jvm/src/test/java/io/eventdriven/strictland/OrderPlaced.approved.txt), saved next to the test:

```json
{"orderId":"00000000-0000-0000-0000-000000000001","customer":"Alice","placedAt":"2024-01-01T12:00:00Z"}
```

You review that file and commit it. From then on the check compares against it and fails if the format drifts, so a later change to the format shows up in the same pull request as the code that caused it.

## Why Strictland

**When you change how a message serializes, the change is easy to miss.** The code compiles and the tests pass, because they write and read the message with the same code. The mismatch surfaces later, when something that still holds the old format reads it: a stored event, a message waiting on a queue, or another service.

If you've used consumer-driven contract testing, the usual shape is to run both the provider and the consumer, record the consumer's expectations against a mock, verify the provider against them, and share those contracts through a broker.

**Strictland takes a smaller, simpler approach. It serializes one message in a normal unit test and saves the output as a snapshot file you commit.** The test fails when the serialized shape changes, and a separate check confirms an older and a newer version of the message can still read each other's data.

**Because it's only serialization and a file, the setup stays small:**

- **The checks are ordinary unit tests in your existing suite**, so there's no broker, schema registry, or mock service to run, and nothing to start in Docker.
- **The contract is the serialized JSON committed next to the test**, so a format change appears in a normal diff and is reviewed like any other code.
- **You write the check beside the message it covers** and get the answer in the same **fast feedback loop** as the rest of your tests.
- **The check uses your application's own serializer**, so the snapshot is the exact bytes you ship.

Strictland checks the serialized shape of a message and whether its versions stay compatible. It doesn't exercise a live exchange between running services, so it complements that kind of tooling rather than replacing it.

## How it works

A message under contract goes through one of two checks.

A **snapshot check** confirms the message still serializes exactly as it did when you last approved it, so nothing reading it downstream breaks. A failure means the format changed: a field renamed, a date format switched, a value newly dropped or added.

A **compatibility check** is for the version you evolve on purpose, so changing a message doesn't strand the ones already in your store or on the wire. Use `thenBackwardCompatible()` to confirm the newer version still reads a message the older one wrote, the events you stored last year or a request already sent. Use `thenForwardCompatible()` to confirm a reader that hasn't upgraded yet still reads a message the newer version writes, so you can ship the new shape before everyone reading it has caught up. Both compare the fields the two versions share and fail if a required one is missing or a shared value changed.


Strictland provides an implementation of a sensible Jackson setup: ISO-8601 dates, nulls kept, unknown properties ignored on read. You can use it with `Json.Jackson.defaults()`:

```java
MessageContract.specification(Json.Jackson.defaults())
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```


Yet, we encourage to use your application's object mapper. Pass the same `ObjectMapper` it uses, so the test checks the exact bytes you ship, snake_case naming, a custom date format, `NON_NULL` inclusion, and so on. Against any other serializer you'd be pinning a shape your consumers never see:

```java
var snakeCase = JsonMapper.builder()
    .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .build();

MessageContract.specification(Json.Jackson.of(snakeCase))
    .given(new ShipmentScheduled(shipmentId, "Alice Smith", scheduledAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

The snapshot then records the shape your mapper actually produces, snake_case keys and all:

```json
{"shipment_id":"00000000-0000-0000-0000-000000000001","recipient_name":"Alice Smith","scheduled_at":"2024-01-01T12:00:00Z"}
```

A snapshot is what your message looks like once serialized: the JSON you reviewed and approved. Every check already uses a default one, named after the message and kept next to your test. You reach for `MessageSnapshot` only to point at a different file, by message-type name when the snapshot is named after a logical type rather than a Java class, by class, or by path:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderInitiated(orderId, null, initiatedAt))
    .whenSerializedAs(MessageSnapshot.ofTypeNamed("OrderInitiated_NullPromotion"))
    .thenContractIsUnchanged();
```

You can also define your own serializer, if you're using unsupported (yet?) format or serializer type. See basic examples in:
- [CsvMessageSerializer](./src/jvm/src/test/java/io/eventdriven/strictland/CsvMessageSerializer.java) and its [tests](src/jvm/src/test/java/io/eventdriven/strictland/CsvMessageSerializerTests.java) or,
- [SimpleBinaryMessageSerializer](./src/jvm/src/test/java/io/eventdriven/strictland/SimpleBinaryMessageSerializer.java) and its [tests](./src/jvm/src/test/java/io/eventdriven/strictland/SimpleBinaryMessageSerializerTests.java).

## Reviewing a drift

When a snapshot check finds the message no longer matches its approved baseline, the failure is meant to be actionable, not a chore.

**The failure message carries the diff.** Every drift writes the new payload to a `.snap.received` file next to the approved one and fails with a unified diff of what moved, so a CI log explains itself with no extra tooling:

```
MessageSnapshot drift: .../OrderPlaced.1.default.snap.approved.json differs from the approved snapshot.

Text content differs (- approved, + received):
  1 | {"id":1,"customer":"Alice"}
- 2 | {"total":10}
+ 2 | {"total":12}

received: .../OrderPlaced.1.default.snap.received.json
approved: .../OrderPlaced.1.default.snap.approved.json

To accept this change, re-run with -Dstrictland.review.mode=approve, or save the received payload over the approved file in the diff tool.
```

A binary serializer falls back to a byte-length and hex summary instead of a line diff.

**On your machine, a diff tool opens.** Locally, the same drift also opens the received payload next to the approved one in a resolved diff tool, where you review it as you would any other change and save over the approved file to accept it. On CI, a headless JVM, or Linux without `DISPLAY`/`WAYLAND_DISPLAY`, nothing launches - the inline diff stands alone.

Auto mode follows the diff tool you already told git about. When `git config diff.tool` is set, it opens `git difftool --no-index`, so the drift shows up in the same tool you use for every other diff, with no Strictland-specific setup. When git has no configured diff tool, it falls back to the first installed tool from the built-in roster (VS Code, IntelliJ IDEA, Meld, Beyond Compare, KDiff3, P4Merge, WinMerge).

**Choosing the diff tool.** You're never stuck with auto selection. In code, name a tool for a spec or globally; in configuration, set `strictland.review.tool` to a registered name (`vscode`, `idea`, `meld`, `bcompare`, `kdiff3`, `p4merge`, `winmerge`) or a full `path {received} {approved}` template for a tool Strictland doesn't know. An explicitly chosen tool wins over auto selection. If that single tool is unavailable, Strictland keeps the inline diff and does not silently launch a different GUI tool:

```java
MessageContract.specification(Json.Jackson.defaults().snapshotReview(SnapshotReview.tool(DiffTool.MELD)))
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

**Accepting a change you made on purpose.** Re-run the tests with the review mode set to `approve`, and each drift re-baselines its snapshot instead of failing - the Jest `-u` / `cargo insta accept` model:

```shell
cd src/jvm
./gradlew test -Dstrictland.review.mode=approve
```

The `-D` property re-baselines whatever those tests touch and then you commit the updated `.snap.approved` files alongside the code. You can also set the mode in code for one spec (`SnapshotReview.approve()`), globally for a suite (`Strictland.defaults().snapshotReview(SnapshotReview.approve())`), or in `strictland.properties`. To re-baseline everything already written without re-running the suite, run the sweep task (below).

### Settings

Review settings work in a `strictland.properties` file on the classpath or as `-D` system properties on a single run.

| Setting | Values | Meaning |
| --- | --- | --- |
| `strictland.review.mode` | `auto` (default), `off`, `approve` | `auto`: inline diff + launch a tool locally. `off`: inline diff only, never launch. `approve`: re-baseline on drift instead of failing. |
| `strictland.review.tool` | a registered name, or a `path {received} {approved}` template | The single diff tool to launch, overriding auto selection without fallback to another GUI tool. |
| `strictland.review.root` | a path (default `src/test/resources/contract-registry`) | The registry root the `SnapshotApprove` sweep walks. |

Resolution, highest priority first: runtime `-Dstrictland.review.*` properties, then a per-spec `snapshotReview(...)`, then the global `Strictland.defaults().snapshotReview(...)`, then `strictland.properties`, then the built-in `auto`.

### Re-baselining in bulk

[`SnapshotApprove`](./src/jvm/src/main/java/io/eventdriven/strictland/SnapshotApprove.java) promotes every `.snap.received` file over its approved sibling as a filesystem sweep, with no test run. Wire it into your build once. This repository ships the Gradle task, so `./gradlew approveSnapshots` works here:

```kotlin
// build.gradle.kts
tasks.register<JavaExec>("approveSnapshots") {
    mainClass = "io.eventdriven.strictland.SnapshotApprove"
    classpath = sourceSets.test.get().runtimeClasspath
}
```

```xml
<!-- Maven: mvn exec:java -Dexec.mainClass=io.eventdriven.strictland.SnapshotApprove -Dexec.classpathScope=test -->
```

## Examples

You can pin a message so its type can't change by accident, the kind of field that's invisible in the Java type but breaks deserialization the moment it's renamed:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new InvoiceIssued(invoiceId, new BigDecimal("99.99")))
    .whenSerializedAs(MessageSnapshot.ofTypeNamed("InvoiceIssuedEvent"))
    .thenContractIsUnchanged();
```

```json
{"type":"InvoiceIssued","invoiceId":"00000000-0000-0000-0000-000000000001","amount":99.99}
```

Confirm a newer type still reads what an older one wrote (backward compatible):

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderPlaced(orderId, "Alice"))
    .whenDeserializedAs(OrderPlacedWithCoupon.class)
    .thenBackwardCompatible(order -> assertNull(order.couponCode()));
```

Confirm a consumer that hasn't upgraded yet still can deserialize what a newer type writes (forward compatible):

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
    .whenDeserializedAs(OrderPlaced.class)
    .thenForwardCompatible();
```

Read a snapshot you saved from an old version with today's type, to prove the current code still deserializes what production stored last year:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(MessageSnapshot.of(CustomerRegisteredV1.class))
    .whenDeserializedAs(CustomerRegisteredV2.class)
    .thenBackwardCompatible(event -> assertNull(event.referralCode()));
```

You'll find these and more in the test suite, [`SerializationContractTests`](./src/jvm/src/test/java/io/eventdriven/strictland/SerializationContractTests.java), [`BackwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/BackwardCompatibilityTests.java), and [`ForwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/ForwardCompatibilityTests.java), each written as a worked example of the cases above.

## Is it production ready?

Strictland is young and pre-1.0, so the API can still move between versions. The checks themselves are small and well covered, and the snapshots they produce are just files in your repository, so trying it out costs little and commits you to nothing.

We'd genuinely like your feedback. If something is missing or awkward, tell us on [Discord](https://discord.gg/fTpqUTMmVa) or open an [issue](https://github.com/event-driven-io/strictland/issues/new).

## Where the name comes from

It's a word game. Contract testing rewards a strict approach to your message shapes, and [Mr. Strickland](https://backtothefuture.fandom.com/wiki/Stanford_S._Strickland) was strict enforcer in *Back to the Future*. That puts it in good company next to its sibling [Emmett](https://github.com/event-driven-io/emmett), named after Doc Emmett Brown.

## Support

Join the [Discord channel](https://discord.gg/fTpqUTMmVa) to ask questions and share what you're building. If Strictland helps you, consider sponsoring the work through [GitHub Sponsors](https://github.com/sponsors/event-driven-io).

## Contribution

Pull requests are welcome. See [CONTRIBUTING.md](./CONTRIBUTING.md) for how to set up your environment and what the build expects.

## Code of Conduct

This project has adopted the code of conduct defined by the [Contributor Covenant](http://contributor-covenant.org/) to clarify expected behavior in our community.
