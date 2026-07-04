package io.eventdriven.strictland;

import java.nio.file.Path;

/**
 * What {@link SnapshotStorage#store storing} a snapshot did: a first-run approval, an unchanged match,
 * or a drift from the approved snapshot. Storage only reports the outcome; deciding what a drift means
 * - open a diff tool, fail the check, or re-baseline - is the caller's job, so a custom store never has
 * to know about diffs, assertions, or review tools.
 */
public sealed interface SnapshotResult {

    /**
     * The location this outcome is for.
     *
     * @return the snapshot location that was stored
     */
    SnapshotLocation location();

    /**
     * The first run for a location: no approved snapshot existed, so the payload was written as the
     * approved snapshot for you to review and commit.
     *
     * @param location the location the approved snapshot was written at
     */
    record Approved(SnapshotLocation location) implements SnapshotResult {}

    /**
     * A later run whose payload matched the approved snapshot: the contract still holds.
     *
     * @param location the location that matched
     */
    record Unchanged(SnapshotLocation location) implements SnapshotResult {}

    /**
     * A later run whose payload differs from the approved snapshot. The received payload has been
     * written beside the approved one so the two can be compared and, if the change is intended,
     * accepted with {@link SnapshotStorage#approve}.
     *
     * @param location the location that drifted
     * @param approved the approved payload the check compared against
     * @param received the drifted payload this run produced
     * @param approvedFile the approved snapshot file
     * @param receivedFile the received snapshot file written for review
     */
    record Drifted(
            SnapshotLocation location,
            SnapshotData approved,
            SnapshotData received,
            Path approvedFile,
            Path receivedFile)
            implements SnapshotResult {}
}
