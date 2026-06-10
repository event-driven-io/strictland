[![](https://dcbadge.vercel.app/api/server/fTpqUTMmVa?style=flat)](https://discord.gg/fTpqUTMmVa) [<img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20px" />](https://www.linkedin.com/in/oskardudycz/) [![Github Sponsors](https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&link=https://github.com/sponsors/event-driven-io)](https://github.com/sponsors/event-driven-io) [![blog](https://img.shields.io/badge/blog-event--driven.io-brightgreen)](https://event-driven.io/?utm_source=event_sourcing_nodejs) [![blog](https://img.shields.io/badge/%F0%9F%9A%80-Architecture%20Weekly-important)](https://www.architecture-weekly.com/?utm_source=event_sourcing_nodejs)

# Strictland - contract testing for the messages your code sends and stores

Strictland is a contract-testing library for the messages your code sends and stores: events, commands, queue messages, HTTP requests and responses, and anything else you serialize for someone else to read.

You write a small unit test that locks down a message's format. Later you rename a field, change a type, or adjust how a value serializes; the code still compiles and your other tests pass, but that one fails and points at what changed. You fix it in your build, before a consumer or a stored event has hit the old format in production.

When a message changes by accident, a snapshot check shows you exactly what moved. When you evolve a message on purpose, a compatibility check confirms an old and a new version can still read each other's data.

Every check starts from `MessageContract` and reads as a sentence:

```java
MessageContract.specification()
    .given(new OrderPlaced(orderId, "Alice", placedAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

## Getting started

Strictland is on Maven Central as `io.event-driven:strictland`. It runs on JDK 21 or newer.

Gradle (Kotlin DSL):

```kotlin
testImplementation("io.event-driven:strictland:0.2.0")
```

Maven:

```xml
<dependency>
  <groupId>io.event-driven</groupId>
  <artifactId>strictland</artifactId>
  <version>0.2.0</version>
  <scope>test</scope>
</dependency>
```

The first run serializes the message and writes the result to an approved file named after the class, [`OrderPlaced.approved.txt`](./src/jvm/src/test/java/io/eventdriven/strictland/OrderPlaced.approved.txt), saved next to the test:

```json
{"orderId":"00000000-0000-0000-0000-000000000001","customer":"Alice","placedAt":"2024-01-01T12:00:00Z"}
```

You review that file and commit it. From then on the check compares against it and fails if the format drifts, so a later change to the format shows up in the same pull request as the code that caused it.

## How it works

A message under contract goes through one of two checks.

A **snapshot check** confirms the message still serializes exactly as it did when you last approved it, so nothing reading it downstream breaks. A failure means the format changed: a field renamed, a date format switched, a value newly dropped or added.

A **compatibility check** is for the version you evolve on purpose, so changing a message doesn't strand the ones already in your store or on the wire. Use `thenBackwardCompatible()` to confirm the newer version still reads a message the older one wrote, the events you stored last year or a request already sent. Use `thenForwardCompatible()` to confirm a reader that hasn't upgraded yet still reads a message the newer version writes, so you can ship the new shape before everyone reading it has caught up. Both compare the fields the two versions share and fail if a required one is missing or a shared value changed.

By default Strictland serializes with a sensible Jackson setup: ISO-8601 dates, nulls kept, unknown properties ignored on read. When your application serializes its own way, pass the same `ObjectMapper` it uses, so the test checks the exact bytes you ship, snake_case naming, a custom date format, `NON_NULL` inclusion, and so on. Against any other serializer you'd be pinning a shape your consumers never see:

```java
var snakeCase = JsonMapper.builder()
    .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    .build();

MessageContract.specification(snakeCase)
    .given(new ShipmentScheduled(shipmentId, "Alice Smith", scheduledAt))
    .whenSerialized()
    .thenContractIsUnchanged();
```

The snapshot then records the shape your mapper actually produces, snake_case keys and all:

```json
{"shipment_id":"00000000-0000-0000-0000-000000000001","recipient_name":"Alice Smith","scheduled_at":"2024-01-01T12:00:00Z"}
```

A snapshot is what your message looks like once serialized: the JSON you reviewed and approved. Every check already uses a default one, named after the message and kept next to your test. You reach for `Snapshot` only to point at a different file, by message-type name when the snapshot is named after a logical type rather than a Java class, by class, or by path:

```java
MessageContract.specification()
    .given(new OrderInitiated(orderId, null, initiatedAt))
    .whenSerialized(Snapshot.forMessageType("OrderInitiated_NullPromotion"))
    .thenContractIsUnchanged();
```

## Examples

Pin a polymorphic event so its type discriminator can't change by accident, the kind of field that's invisible in the Java type but breaks deserialization the moment it's renamed:

```java
MessageContract.specification()
    .given(new InvoiceIssued(invoiceId, new BigDecimal("99.99")))
    .whenSerialized(Snapshot.forMessageType("InvoiceIssuedEvent"))
    .thenContractIsUnchanged();
```

```json
{"type":"InvoiceIssued","invoiceId":"00000000-0000-0000-0000-000000000001","amount":99.99}
```

Confirm a newer type still reads what an older one wrote (backward compatible):

```java
MessageContract.specification()
    .given(new OrderPlaced(orderId, "Alice"))
    .whenDeserializedAs(OrderPlacedWithCoupon.class)
    .thenBackwardCompatible(order -> assertNull(order.couponCode()));
```

Confirm a reader that hasn't upgraded yet still reads what a newer type writes (forward compatible):

```java
MessageContract.specification()
    .given(new OrderPlacedWithCoupon(orderId, "Alice", "SAVE10"))
    .whenDeserializedAs(OrderPlaced.class)
    .thenForwardCompatible();
```

Read a snapshot you saved from an old version with today's type, to prove the current code still loads what production stored last year:

```java
MessageContract.specification()
    .given(Snapshot.of(CustomerRegisteredV1.class))
    .whenDeserializedAs(CustomerRegisteredV2.class)
    .thenBackwardCompatible(event -> assertNull(event.referralCode()));
```

Pin the public surface of a package the same way you pin a message: snapshot the text, and any accidental change to the surface, a method gone, a parameter added, a return type changed, fails the build:

```java
String api = PublicApiScanner.forPackage("com.myapp.events.v1").generate();
Approvals.verify(api);
```

You'll find these and more in the test suite, [`SerializationContractTests`](./src/jvm/src/test/java/io/eventdriven/strictland/SerializationContractTests.java), [`BackwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/BackwardCompatibilityTests.java), and [`ForwardCompatibilityTests`](./src/jvm/src/test/java/io/eventdriven/strictland/ForwardCompatibilityTests.java), each written as a worked example of the cases above.

## Why Strictland

When you change how a message serializes, the change is easy to miss. The code compiles and the tests pass, because they write and read the message with the same code. The mismatch surfaces later, when something that still holds the old format reads it: a stored event, a message waiting on a queue, or another service.

If you've used consumer-driven contract testing, the usual shape is to run both the provider and the consumer, record the consumer's expectations against a mock, verify the provider against them, and share those contracts through a broker. Strictland takes a smaller, simpler approach. It serializes one message in a normal unit test and saves the output as a snapshot file you commit. The test fails when the serialized shape changes, and a separate check confirms an older and a newer version of the message can still read each other's data.

Because it's only serialization and a file, the setup stays small:

- **The checks are ordinary unit tests in your existing suite**, so there's no broker, schema registry, or mock service to run, and nothing to start in Docker.
- **The contract is the serialized JSON committed next to the test**, so a format change appears in a normal diff and is reviewed like any other code.
- **You write the check beside the message it covers** and get the answer in the same **fast feedback loop** as the rest of your tests.
- **The check uses your application's own serializer**, so the snapshot is the exact bytes you ship.

Strictland checks the serialized shape of a message and whether its versions stay compatible. It doesn't exercise a live exchange between running services, so it complements that kind of tooling rather than replacing it.

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
