[![](https://dcbadge.vercel.app/api/server/fTpqUTMmVa?style=flat)](https://discord.gg/fTpqUTMmVa)[![Github Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&link=https://github.com/sponsors/event-driven-io)](https://github.com/sponsors/event-driven-io) [![blog](https://img.shields.io/badge/blog-event--driven.io-brightgreen)](https://event-driven.io/?utm_source=event_sourcing_nodejs) [![blog](https://img.shields.io/badge/%F0%9F%9A%80-Architecture%20Weekly-important)](https://www.architecture-weekly.com/?utm_source=event_sourcing_nodejs) [<img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20px" />](https://www.linkedin.com/in/oskardudycz/) 

# Strictland - contract testing for message compatibility

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
testImplementation("io.event-driven:strictland:0.4.0")
```

Maven:

```xml
<dependency>
  <groupId>io.event-driven</groupId>
  <artifactId>strictland</artifactId>
  <version>0.4.0</version>
  <scope>test</scope>
</dependency>
```

Then add a new test:

```java
MessageContract.specification(Json.Jackson.defaults())
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

The first run serializes the message and writes the result to a file in your repository:

```json
{"orderId":"00000000-0000-0000-0000-000000000001","customer":"Alice","placedAt":"2024-01-01T12:00:00Z"}
```

Review that file and commit it with the test. From then on, Strictland treats the committed file as the approved snapshot: the expected message format for later runs. By default, Strictland keeps those snapshots under `src/test/resources/contract-registry`, grouped by message contract. If the serialized format changes, the test fails and the diff appears in the same pull request as the code that caused it.

## Why Strictland

**When you change how a message serializes, the change is easy to miss.** The code compiles and the tests pass, because they write and read the message with the same code. The mismatch surfaces later, when something that still holds the old format reads it: a stored event, a message waiting on a queue, or another service.

If you've used consumer-driven contract testing, the usual shape is to run both the provider and the consumer, record the consumer's expectations against a mock, verify the provider against them, and share those contracts through a broker.

**Strictland takes a smaller, simpler approach. It serializes one message in a normal unit test and saves the output as a snapshot file you commit.** The test fails when the serialized shape changes, and a separate check confirms an older and a newer version of the message can still read each other's data.

**Because it's only serialization and a file, the setup stays small:**

- **The checks are ordinary unit tests in your existing suite**, so there's no broker, schema registry, or mock service to run, and nothing to start in Docker.
- **The contract is the serialized JSON committed to the same repository as your tests**, so a format change appears in a normal diff and is reviewed like any other code.
- **You write the check beside the message it covers** and get the answer in the same **fast feedback loop** as the rest of your tests.
- **The check uses your application's own serializer**, so the snapshot is the exact bytes you ship.

Strictland checks the serialized shape of a message and whether its versions stay compatible. It doesn't exercise a live exchange between running services, so it complements that kind of tooling rather than replacing it.

## How it works

A message under contract goes through one of two checks.

A **snapshot check** confirms the message still serializes exactly as it did when you last approved it, so nothing reading it downstream breaks. A failure means the format changed: a field renamed, a date format switched, a value newly dropped or added.

A **compatibility check** is for intentional message evolution. It protects messages that already exist: stored events, queued messages, sent requests, or responses another service may still read. Use `thenBackwardCompatible()` to confirm the newer version still reads a message the older one wrote. Use `thenForwardCompatible()` to confirm an older reader still reads a message the newer version writes. Both compare the fields the two versions share and fail if a required one is missing or a shared value changed.

Strictland includes a sensible Jackson setup: ISO-8601 dates, nulls kept, unknown properties ignored on read. You can use it with `Json.Jackson.defaults()`:

```java
MessageContract.specification(Json.Jackson.defaults())
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

In production code, prefer your application's object mapper. Pass the same `ObjectMapper` it uses, so the test checks the exact bytes you ship: field naming, date format, null handling, and other serialization rules. Against any other serializer, you would be pinning a shape your consumers never see:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new ShipmentScheduled(shipmentId, "Alice Smith", scheduledAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

For instance, if your mapper writes snake_case fields, the snapshot records that exact shape:

```json
{"shipment_id":"00000000-0000-0000-0000-000000000001","recipient_name":"Alice Smith","scheduled_at":"2024-01-01T12:00:00Z"}
```

A snapshot is what your message looks like once serialized: the JSON you reviewed and approved. If you do not choose a snapshot name, Strictland names it after the message class. Use `MessageSnapshot` when you need to point at another snapshot explicitly: by message-type name when the snapshot is named after a logical type rather than a Java class, by class, or by path.

## The contract registry

Strictland keeps approved snapshots as files in the same Git repository as your tests. There is no external broker or cloud registry to run. By default, those files live under `src/test/resources/contract-registry`.

The registry groups snapshots by message type. Several contracts produce a tree like this:

```text
src/test/resources/
  contract-registry/
    com/acme/orders/OrderPlaced/
      OrderPlaced.1.default.snap.approved.json
      OrderPlaced.2.default.snap.approved.json
    com/acme/orders/OrderInitiated/
      OrderInitiated.1.WithPromotion.snap.approved.json
      OrderInitiated.1.NoPromotion.snap.approved.json
    InvoiceIssuedEvent/
      InvoiceIssuedEvent.1.default.snap.approved.json
```

That structure keeps contract files out of source packages without scattering them across the project. Each message contract has one place to look, and compatibility checks have a stable location for older shapes. The approved files are the baselines your tests compare against. When current code writes a different shape, Strictland puts the new payload next to it as `.snap.received` so you can inspect the change before accepting it.

By default, Strictland names a contract from the message class's fully-qualified name. A class such as `com.acme.orders.OrderPlaced` is stored under `com/acme/orders/OrderPlaced`. If you name a contract explicitly, for example with `MessageSnapshot.ofTypeNamed("InvoiceIssuedEvent")`, that logical name decides where the snapshot is stored.

The file name carries the message name, contract version, example label, snapshot state, and serializer extension:

```text
OrderPlaced.1.default.snap.approved.json
OrderPlaced.1.default.snap.received.json
```

## Versions and variants

Version and variant answer different questions. A version identifies which revision of the message format you are protecting. A variant identifies which example of that version you are protecting.

Use a version when the same Java class represents more than one version of the message format. For example, `OrderPlaced` may have version `1` in production, then the class evolves and starts writing version `2`. Version `1` messages can still exist in storage, queues, or traffic, so keep their approved snapshot separate from version `2`:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderPlaced(orderId, "Alice", placedAt), "2")
    .whenSerialized()
    .thenContractIsUnchanged();
```

Then read that version explicitly in a compatibility check:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(MessageSnapshot.of(OrderPlaced.class).version("2"))
    .whenDeserializedAs(OrderPlaced.class)
    .thenBackwardCompatible();
```

One version of a message can still have more than one important shape. For example, keep one snapshot with optional data present and another with only the required data, so compatibility checks cover both cases. In the API, those examples are called variants:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderInitiated(orderId, "Alice", "WELCOME"))
    .whenSerializedAs(SnapshotVariant.named("WithPromotion"))
    .thenContractIsUnchanged();

MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderInitiated(orderId, "Alice", null))
    .whenSerializedAs(SnapshotVariant.named("NoPromotion"))
    .thenContractIsUnchanged();
```

When a compatibility check reads `MessageSnapshot.of(OrderInitiated.class)` without a variant, Strictland replays all approved variants for that message type and version. Add `.variant("WithPromotion")` when the check should read only one example.

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(MessageSnapshot.of(OrderInitiated.class).variant("NoPromotion"))
    .whenDeserializedAs(OrderInitiated.class)
    .thenBackwardCompatible();
```

Versions and variants can be combined. That lets one message type keep several examples for version `1` and several examples for version `2`, then read the exact case a compatibility check is meant to protect:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(MessageSnapshot.of(OrderInitiated.class)
        .version("2")
        .variant("NoPromotion"))
    .whenDeserializedAs(OrderInitiated.class)
    .thenBackwardCompatible();
```

## Common checks

### Catch accidental format changes

Use a snapshot check when the serialized message should stay exactly the same unless you approve the change. This is useful for fields that are easy to change in code but visible to other systems, such as a type discriminator:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new InvoiceIssued(invoiceId, new BigDecimal("99.99")))
    .whenSerializedAs(MessageSnapshot.ofTypeNamed("InvoiceIssuedEvent"))
    .thenContractIsUnchanged();
```

```json
{"type":"InvoiceIssued","invoiceId":"00000000-0000-0000-0000-000000000001","amount":99.99}
```

### Check backward compatibility

Use a backward compatibility check before moving readers to a newer message type. It confirms the newer type can still read messages written by the older one:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderPlaced(orderId, "Alice"))
    .whenDeserializedAs(OrderPlacedWithCoupon.class)
    .thenBackwardCompatible(order -> assertNull(order.couponCode()));
```

### Check forward compatibility

Use a forward compatibility check when newer code may write a message before every reader has upgraded. It confirms an older type can still read the newer message:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
    .whenDeserializedAs(OrderPlaced.class)
    .thenForwardCompatible();
```

### Read an approved older snapshot

Use `MessageSnapshot` when the old shape is already saved in the contract registry. This checks current code against the same bytes that were previously approved, instead of rebuilding the old message in the test:

```java
MessageContract.specification(Json.Jackson.of(yourObjectMapper))
    .given(MessageSnapshot.of(CustomerRegisteredV1.class))
    .whenDeserializedAs(CustomerRegisteredV2.class)
    .thenBackwardCompatible(event -> assertNull(event.referralCode()));
```

You'll find these and more in the test suite, [`SerializationContractTests`](./src/jvm/src/test/java/io/eventdriven/strictland/SerializationContractTests.java), [`BackwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/BackwardCompatibilityTests.java), and [`ForwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/ForwardCompatibilityTests.java), each written as a worked example of the cases above.

## Reviewing a drift

When a snapshot check finds the message no longer matches its approved baseline, the failure is meant to show what changed and how to review it.

**The failure message shows what changed.** Every drift writes the new payload to a `.snap.received` file next to the approved one and includes the approved and received paths in the failure. For text payloads, such as JSON or CSV, the failure also includes a line diff, so a CI log explains itself with no extra tooling:

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

For payloads that cannot be shown as text, the failure uses the byte length and a short hex preview instead of a line diff.

**On your machine, a diff tool opens.** Locally, the same drift also opens the received payload next to the approved one in a diff tool, where you review it as you would any other change and save over the approved file to accept it. On CI, a headless JVM, or Linux without `DISPLAY`/`WAYLAND_DISPLAY`, nothing launches; the failure message is the review output.

Auto mode follows the diff tool you already told git about. When `git config diff.tool` is set, it opens `git difftool --no-index`, so the drift shows up in the same tool you use for every other diff, with no Strictland-specific setup. When git has no configured diff tool, it falls back to the first installed tool from the built-in roster (VS Code, IntelliJ IDEA, Meld, Beyond Compare, KDiff3, P4Merge, WinMerge).

**Using a specific diff tool.** You usually do not need to configure this. Auto mode uses your git diff tool when one is configured, then looks for a known local tool. Set a tool only when a project or a test suite should always ask for the same one. In code, name a tool for a spec or globally; in configuration, set `strictland.review.tool` to a registered name (`vscode`, `idea`, `meld`, `bcompare`, `kdiff3`, `p4merge`, `winmerge`). If that tool is unavailable, Strictland keeps the failure message and does not silently launch a different GUI tool:

```java
MessageContract.specification(Json.Jackson.defaults().snapshotReview(SnapshotReview.tool(DiffTool.MELD)))
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

Use a custom command only for a tool Strictland does not list, or when the command needs project-specific arguments. Include `{received}` and `{approved}` placeholders; Strictland replaces them with the two snapshot files. For example, to open the approved file on the left and the received file on the right in Neovim diff mode:

```java
MessageContract.specification(Json.Jackson.defaults()
        .snapshotReview(SnapshotReview.customTool("nvim -d {approved} {received}")))
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

If there is a diff tool you use often and Strictland should support it directly, open an [issue](https://github.com/event-driven-io/strictland/issues/new).

**Accepting a change you made on purpose.** Re-run the tests with the review mode set to `approve`, and each drift updates its approved snapshot instead of failing - the Jest `-u` / `cargo insta accept` model:

```shell
cd src/jvm
./gradlew test -Dstrictland.review.mode=approve
```

The `-D` property updates the approved snapshots touched by that test run. You then commit the updated `.snap.approved` files alongside the code. You can also set the mode in code for one spec (`SnapshotReview.approve()`), globally for a suite (`Strictland.defaults().snapshotReview(SnapshotReview.approve())`), or in `strictland.properties`.

**Replacing approved snapshots with the latest output.** If the contract has not been released yet, or you have reviewed the drift and want the current output to become the new baseline, [`SnapshotApprove`](./src/jvm/src/main/java/io/eventdriven/strictland/SnapshotApprove.java) replaces each `.snap.approved` file with its matching `.snap.received` file. It does this as a filesystem sweep, so you can accept the latest state without rerunning the test suite. Wire it into your build once. This repository ships the Gradle task, so `./gradlew approveSnapshots` works here:

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

By default, the sweep walks `src/test/resources/contract-registry`. To sweep another directory, pass it as the first argument or set `-Dstrictland.review.root=...` for that run.

## Configuration

Strictland is meant to work without project-specific configuration. The defaults keep snapshots under `src/test/resources/contract-registry`, show a useful failure message when a snapshot drifts, open a local diff tool when one is available, and stay with the failure message on CI.

Configure it only when the project needs a different registry location, a fixed review policy, or a specific diff tool.

### Project configuration file

For project-wide settings, add `strictland.properties` to your test resources:

```text
src/test/resources/strictland.properties
```

Only include the settings you want to change. For example:

```properties
strictland.layout.wrapperFolder=message-contracts
strictland.review.mode=off
strictland.review.tool=nvim -d {approved} {received}
```

| Setting | Default | Meaning |
| --- | --- | --- |
| `strictland.layout.rootPath` | `src/test/resources` | The directory the snapshot tree is rooted at. |
| `strictland.layout.wrapperFolder` | `contract-registry` | The folder under the root that holds the per-message snapshot folders. |
| `strictland.review.mode` | `auto` | `auto`: show the failure message and open a diff tool locally. `off`: show the failure message only. `approve`: replace the approved snapshot when a drift is found. |
| `strictland.review.tool` | auto-selected | A registered tool name (`vscode`, `idea`, `meld`, `bcompare`, `kdiff3`, `p4merge`, `winmerge`) or a custom command with `{approved}` and `{received}` placeholders. |

Review settings can also be passed for one run:

```shell
./gradlew test -Dstrictland.review.mode=approve
```

### Per-spec configuration

Use code configuration when the setting belongs to a test, not to the whole project. A per-spec option affects only that `MessageContract`:

```java
var options = Json.Jackson.of(yourObjectMapper)
    .snapshotLayout(SnapshotLayout.registry().wrapperFolder("message-contracts"))
    .snapshotReview(SnapshotReview.off());

MessageContract.specification(options)
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

### Global code defaults

Use `Strictland.defaults()` when the suite should share the same Strictland settings but you prefer to keep them in test setup code instead of `strictland.properties`. This is useful when the setting is assembled in code, or when a test fixture needs to control the defaults for the scope of that suite.

Each setting is independent. If you set only `snapshotReview(...)`, the layout still falls through to `strictland.properties` or the built-in registry layout:

```java
@BeforeAll
static void configureStrictland() {
    Strictland.defaults()
        .snapshotReview(SnapshotReview.off());
}

@AfterAll
static void resetStrictland() {
    Strictland.resetDefaults();
}
```

You can set layout the same way when the whole suite should use a different registry folder:

```java
@BeforeAll
static void configureStrictland() {
    Strictland.defaults()
        .snapshotLayout(SnapshotLayout.registry().wrapperFolder("message-contracts"));
}
```

`Strictland.defaults()` is process-wide. Reset it in teardown when a test suite changes it, so another suite does not inherit that setting by accident.

The lookup order is:

| Setting type | Lookup order |
| --- | --- |
| Layout | per-spec `snapshotLayout(...)`, then `Strictland.defaults().snapshotLayout(...)`, then `strictland.properties`, then the built-in registry layout. |
| Review | runtime `-Dstrictland.review.*` properties, then per-spec `snapshotReview(...)`, then `Strictland.defaults().snapshotReview(...)`, then `strictland.properties`, then `auto`. |

### Custom serializers

Strictland provides Jackson support for JSON. If your messages use another format, implement `MessageSerializer` with the same serialization rules your application uses. Return the snapshot file extension without the leading dot:

```java
final class CsvMessageSerializer implements MessageSerializer {
    @Override
    public String fileExtension() {
        return "csv";
    }

    // implement serialize(...) and deserialize(...) with the format your application uses
}
```

See complete examples in:
- [CsvMessageSerializer](./src/jvm/src/test/java/io/eventdriven/strictland/CsvMessageSerializer.java) and its [tests](src/jvm/src/test/java/io/eventdriven/strictland/CsvMessageSerializerTests.java) or,
- [SimpleBinaryMessageSerializer](./src/jvm/src/test/java/io/eventdriven/strictland/SimpleBinaryMessageSerializer.java) and its [tests](./src/jvm/src/test/java/io/eventdriven/strictland/SimpleBinaryMessageSerializerTests.java).

### Message type names

Use `messageTypeMapper(...)` when the contract name should come from your system rather than from the Java class. This is common when an event store or message bus records a logical message type, and you want the snapshot registry to use the same name reviewers already see in production.

Implement `MessageTypeMapper`. `name(...)` maps a Java class to the contract name used in the registry. If your mapper cannot resolve a stored name back to a Java class, `type(...)` can return `Optional.empty()`:

```java
final class RegisteredMessageTypes implements MessageTypeMapper {
    private final Map<Class<?>, String> namesByClass = Map.of(
        OrderPlaced.class, "orders.OrderPlaced",
        OrderCancelled.class, "orders.OrderCancelled");

    private final Map<String, Class<?>> classesByName = Map.of(
        "orders.OrderPlaced", OrderPlaced.class,
        "orders.OrderCancelled", OrderCancelled.class);

    @Override
    public MessageTypeName name(Class<?> type) {
        return Optional.ofNullable(namesByClass.get(type))
            .map(MessageTypeName::of)
            .orElseGet(() -> MessageTypeName.of(type));
    }

    @Override
    public Optional<Class<?>> type(String name) {
        return Optional.ofNullable(classesByName.get(name));
    }
}

var options = Json.Jackson.of(yourObjectMapper)
    .messageTypeMapper(new RegisteredMessageTypes());
```

With that mapper, `OrderPlaced` snapshots are stored under `contract-registry/orders/OrderPlaced` even if the Java class lives in another package.

### Custom snapshot storage

Use `snapshotStorage(...)` only when snapshots should be read from and written to something other than Strictland's file registry. If you only want a different folder, use `snapshotLayout(...)` or `strictland.layout.*` settings instead.

Implement `SnapshotStorage`. `store(...)` writes or compares a snapshot, `approve(...)` accepts a received payload, `read(...)` loads one approved snapshot, and `readAll(...)` loads a family of approved snapshots, for example all variants of one message version.

```java
final class MySnapshotStorage implements SnapshotStorage {
    @Override
    public SnapshotResult store(SnapshotLocation location, SnapshotData received) {
        // write the first approved snapshot, return Unchanged when it matches,
        // or write a received snapshot and return Drifted when it differs
    }

    @Override
    public void approve(SnapshotLocation location, SnapshotData received) {
        // replace the approved snapshot with the received payload
    }

    @Override
    public SnapshotData read(SnapshotLocation location) {
        // read one approved snapshot
    }

    @Override
    public List<SnapshotData> readAll(SnapshotFilter filter) {
        // read all approved snapshots matching this message type and version
    }
}
```

```java
var options = SpecificationOptions.serializer(new CsvMessageSerializer())
    .snapshotStorage(new MySnapshotStorage());

MessageContract.specification(options)
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

See [Base64SnapshotStorage](./src/jvm/src/test/java/io/eventdriven/strictland/Base64SnapshotStorage.java) and its [tests](./src/jvm/src/test/java/io/eventdriven/strictland/SimpleBinaryMessageSerializerTests.java) for a complete custom storage example.

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
