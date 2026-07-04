package io.eventdriven.strictland;

/**
 * The shared context between {@code given(...)} through to a {@code then} step: <br/>
 * - the serializer that turns a message into bytes, <br/>
 * - the storage that reads and writes snapshots, <br/>
 * - the type mapper that names a message type, <br/>
 * - the reviewer that decides what a stored snapshot's outcome means.
 *
 * @param serializer turns a message into the bytes the contract compares, and back
 * @param storage reads and writes approved snapshots
 * @param typeMapper names a message type for locating its snapshot
 * @param reviewer reacts to a store outcome: pass, re-baseline, or open a diff and fail
 */
record ContractContext(
        MessageSerializer serializer,
        SnapshotStorage storage,
        MessageTypeMapper typeMapper,
        SnapshotReviewer reviewer) {}
