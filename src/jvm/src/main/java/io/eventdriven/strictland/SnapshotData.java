package io.eventdriven.strictland;

/**
 * A snapshot's raw payload bytes, the one value storage stores, reads, and replays. It is a thin typed
 * wrapper so the storage seam moves a named value rather than a bare {@code byte[]}, which keeps method
 * signatures self-describing and stops a payload getting confused with any other byte array.
 *
 * <p>Equality is the record default, by array identity, not by content. Two {@code SnapshotData} that
 * hold equal-but-distinct arrays are not equal yet. Content equality is a later concern.</p>
 *
 * @param bytes the snapshot's raw payload bytes
 */
// The payload is intentionally a raw byte array, the value the storage seam moves; identity equality is
// the point for now, so the array record component is deliberate.
@SuppressWarnings("ArrayRecordComponent")
record SnapshotData(byte[] bytes) {}
