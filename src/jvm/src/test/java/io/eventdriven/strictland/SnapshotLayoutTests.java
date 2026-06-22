package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotLayoutTests {

    @Test
    void registry_usesTheDocumentedDefaults() {
        var layout = SnapshotLayout.registry();

        assertEquals("src/test/resources", layout.rootPath());
        assertEquals("contract-registry", layout.wrapperFolder());
    }

    @Test
    void withers_returnCopiesWithoutMutatingTheOriginal() {
        var original = SnapshotLayout.registry();

        var rerooted = original.rootPath("build/snaps");
        var wrapped = original.wrapperFolder("approved");

        assertEquals("build/snaps", rerooted.rootPath());
        assertEquals("approved", wrapped.wrapperFolder());
        assertEquals("src/test/resources", original.rootPath());
        assertEquals("contract-registry", original.wrapperFolder());
    }
}
