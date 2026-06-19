package io.eventdriven.strictland;

/**
 * Defines how {@link SnapshotLayout} groups approved files into folders. Pick the one based on how you want to organise your snapshots:
 * by the test that owns them, or by the message contract they lock down.
 *
 * <pre>
 * SnapshotLayout byContract = SnapshotLayout
 *     .nextToTest()
 *     .grouping(SnapshotGrouping.PER_CONTRACT);
 * </pre>
 */
public enum SnapshotGrouping {

    /**
     * Stores snapshots straight under the root, with no group folder. Pair it with an empty wrapper
     * folder and a next-to-test root to keep snapshots flat in the test's source directory.
     */
    NONE,

    /**
     * Stores snapshot in a folder per message contract, named after the message type.
     * All of the message type's snapshots sit together, which makes it easier to navigate and locate different message snapshots.
     */
    PER_CONTRACT,

    /**
     * Stores snapshot in a folder per test class, named after the calling test. All of one test's snapshots sit
     * together, which helps to logically group your snapshots by usage scenarios.
     */
    PER_TEST_CLASS
}
