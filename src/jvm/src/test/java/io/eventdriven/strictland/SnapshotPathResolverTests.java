package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class SnapshotPathResolverTests {

    @Test
    void requireCaller_whenFrameIsEmpty_throwsIllegalState() {
        var ex = assertThrows(IllegalStateException.class, () -> FileSnapshotStorage.requireCaller(Optional.empty()));
        assertTrue(requireNonNull(ex.getMessage()).contains("Cannot determine calling test class from stack"));
    }
}
