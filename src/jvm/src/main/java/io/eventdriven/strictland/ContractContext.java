package io.eventdriven.strictland;

/**
 * The shared context between {@code given(...)} through to a {@code then} step: <br/>
 * - the serializer that turns a message into bytes, <br/>
 * - the storage that reads and writes snapshots, <br/>
 * - the type mapper that names a message type, <br/>
 * - the reviewer that decides what a stored snapshot's outcome means, <br/>
 * - the diff review that opens a drifted pair for a human to compare.
 *
 * @param serializer turns a message into the bytes the contract compares, and back
 * @param storage reads and writes approved snapshots
 * @param typeMapper names a message type for locating its snapshot
 * @param reviewer turns a store outcome into a verdict: pass, re-baseline, or fail
 * @param diffReview opens the drifted received/approved pair in the developer's diff tool
 */
record ContractContext(
        MessageSerializer serializer,
        SnapshotStorage storage,
        MessageTypeMapper typeMapper,
        SnapshotReviewer reviewer,
        DiffReview diffReview) {}
