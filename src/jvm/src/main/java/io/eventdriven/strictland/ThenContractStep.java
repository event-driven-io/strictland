package io.eventdriven.strictland;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.reporters.AutoApproveWhenEmptyReporter;
import org.jspecify.annotations.Nullable;

/**
 * The "then" step of the contract DSL that verifies the serialized shape of an instance is unchanged.
 */
public class ThenContractStep<S> {
    private final S instance;
    private final @Nullable Snapshot destination;
    private final ObjectMapper mapper;

    ThenContractStep(S instance, @Nullable Snapshot destination, ObjectMapper mapper) {
        this.instance = instance;
        this.destination = destination;
        this.mapper = mapper;
    }

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
        try {
            Approvals.verify(
                    mapper.writeValueAsString(instance), options.withReporter(new AutoApproveWhenEmptyReporter()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ApprovalNamer namedAt(Path path) {
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
