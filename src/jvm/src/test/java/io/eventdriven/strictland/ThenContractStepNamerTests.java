package io.eventdriven.strictland;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class ThenContractStepNamerTests {

    @Test
    void namedAt_returnsNamerWithCorrectNameAndPath() {
        var path = Path.of("src/test/java/io/eventdriven/strictland/MyEvent.approved.txt");
        var namer = ThenContractStep.namedAt(path);

        assertEquals("MyEvent", namer.getApprovalName());

        var expectedDir = requireNonNull(path.getParent()).toAbsolutePath() + File.separator;
        assertEquals(expectedDir, namer.getSourceFilePath());

        assertEquals(new File(expectedDir + "MyEvent.approved.txt"), namer.getApprovedFile(".txt"));

        assertEquals(new File(expectedDir + "MyEvent.received.txt"), namer.getReceivedFile(".txt"));

        assertSame(namer, namer.addAdditionalInformation("ignored"));
    }
}
