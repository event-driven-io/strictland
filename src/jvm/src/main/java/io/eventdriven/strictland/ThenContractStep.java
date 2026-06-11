package io.eventdriven.strictland;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.reporters.AutoApproveWhenEmptyReporter;
import org.jspecify.annotations.Nullable;

/**
 * The check that the message's serialized format still matches what you approved.
 *
 * <p>Reached after {@link GivenStep#whenSerialized()}. Call {@link #thenContractIsUnchanged()} to
 * compare the output against the approved snapshot and fail the test when it has drifted.
 *
 * @param <S> the type of the message under test
 */
public class ThenContractStep<S> {
    private final S instance;
    private final @Nullable Snapshot destination;
    private final MessageSerializer serializer;

    ThenContractStep(S instance, @Nullable Snapshot destination, MessageSerializer serializer) {
        this.instance = instance;
        this.destination = destination;
        this.serializer = serializer;
    }

    /**
     * Confirms the message still serializes exactly as it did when you last approved it, so nothing
     * reading it downstream breaks. A failure means the format changed: a field renamed, a date format
     * switched, a value newly dropped or added.
     *
     * <p>The first run creates the approved file from the current message for you to review and
     * commit; it lives next to your test, so a later change to the format shows up in the same pull
     * request as the code that caused it.
     *
     * {@snippet :
     * MessageContract.specification()
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * }
     */
    public void thenContractIsUnchanged() {
        verify(destination != null ? optionsFor(destination) : defaultOptions());
    }

    private Options defaultOptions() {
        return new Options().forFile().withBaseName(instance.getClass().getSimpleName());
    }

    private Options optionsFor(Snapshot s) {
        return switch (s) {
            case Snapshot.ByClass<?> b ->
                new Options().forFile().withBaseName(b.sourceType().getSimpleName());
            case Snapshot.ByMessageType b -> new Options().forFile().withBaseName(b.messageType());
            case Snapshot.ByPath b -> new Options().forFile().withNamer(namedAt(b.path()));
        };
    }

    private void verify(Options options) {
        Approvals.verify(
                new String(serializer.serialize(instance), StandardCharsets.UTF_8),
                options.withReporter(new AutoApproveWhenEmptyReporter()));
    }

    static ApprovalNamer namedAt(Path path) {
        var parent = path.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Snapshot path must have a parent directory: " + path);
        }
        var directory = parent.toAbsolutePath() + File.separator;
        var name = path.getFileName().toString().replaceAll("\\.approved\\.txt$", "");
        return new ApprovalNamer() {
            @Override
            public String getApprovalName() {
                return name;
            }

            @Override
            public String getSourceFilePath() {
                return directory;
            }

            @Override
            public File getApprovedFile(String ext) {
                return new File(directory + name + ".approved" + ext);
            }

            @Override
            public File getReceivedFile(String ext) {
                return new File(directory + name + ".received" + ext);
            }

            @Override
            public ApprovalNamer addAdditionalInformation(String info) {
                return this;
            }
        };
    }
}
