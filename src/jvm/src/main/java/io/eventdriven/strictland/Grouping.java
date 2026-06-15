package io.eventdriven.strictland;

/**
 * How {@link SnapshotLayout} groups approved files into folders. Pick the one that matches how you
 * read your snapshots: by the test that owns them, or by the message contract they lock down.
 *
 * {@snippet :
 * SnapshotLayout byContract = SnapshotLayout.nextToTest().grouping(Grouping.PER_CONTRACT);
 * }
 */
public enum Grouping {
    /**
     * A folder per test class, named after the calling test. All of one test's snapshots sit
     * together, which reads well when a single test locks down several messages.
     */
    PER_TEST_CLASS,

    /**
     * A folder per message contract, named after the message type. A contract's snapshots sit
     * together across tests, which reads well when many tests touch the same message.
     */
    PER_CONTRACT
}
