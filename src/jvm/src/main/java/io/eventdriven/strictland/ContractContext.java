package io.eventdriven.strictland;

/**
 * The wiring one contract check carries from {@code given(...)} through to a {@code then} step: the
 * serializer that turns a message into bytes, the storage that reads and writes snapshots, and the type
 * mapper that names a message type. Folding the three into one value keeps the step constructors short
 * and stops a layout or a file extension leaking through every step, since both derive at the use site.
 *
 * @param serializer turns a message into the bytes the contract compares, and back
 * @param storage reads and writes approved snapshots
 * @param typeMapper names a message type for locating its snapshot
 */
record ContractContext(MessageSerializer serializer, SnapshotStorage storage, MessageTypeMapper typeMapper) {}
