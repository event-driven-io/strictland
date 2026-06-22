/**
 * Strictland is a contract-testing library for the messages your code sends and stores: events,
 * commands, queue messages, HTTP requests and responses, and anything else you serialize for someone
 * else to read.
 *
 * <p>You write a small unit test that locks down a message's format. Later you rename a field, change
 * a type, or adjust how a value serializes; the code still compiles and your other tests pass, but
 * that one fails and points at what changed. You fix it in your build, before a consumer or a stored
 * event has hit the old format in production.</p>
 *
 * <p>When a message changes by accident, a snapshot check ({@link
 * io.eventdriven.strictland.ThenContractStep}) shows you exactly what moved. When you evolve a message
 * on purpose, a compatibility check ({@link io.eventdriven.strictland.ThenCompatibilityStep}) confirms
 * an old and a new version can still read each other's data.</p>
 *
 * <p>Both start from {@link io.eventdriven.strictland.MessageContract} and read as a sentence:</p>
 *
 * <pre>
 * // Lock a message's format, so any accidental change to it fails the build:
 * MessageContract.specification(Json.Jackson.defaults())
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 *
 * // Check that a newer version can still read data written by an older one:
 * MessageContract.specification(Json.Jackson.defaults())
 *     .given(new OrderPlaced(orderId, "Alice"))
 *     .whenDeserializedAs(OrderPlacedWithCoupon.class)
 *     .thenBackwardCompatible();
 * </pre>
 *
 * <p>The approved file each check compares against lives in a committed contract registry, under
 * {@code src/test/resources/contract-snapshots} by default, so a contract change shows up in the same
 * pull request as the code that caused it.
 * {@link io.eventdriven.strictland.MessageSnapshot} picks which file backs a check, and {@link
 * io.eventdriven.strictland.PublicApiScanner} renders a package's public API as text so you can
 * approval-test the surface itself.</p>
 */
@NullMarked
package io.eventdriven.strictland;

import org.jspecify.annotations.NullMarked;
