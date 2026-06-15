package io.eventdriven.strictland;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.reporters.AutoApproveWhenEmptyReporter;

class FileSnapshotStorage implements SnapshotStorage {
    private static final String APPROVED_SUFFIX = ".approved.txt";
    private static final String SNAP_BASE_SUFFIX = ".snap";
    private static final Set<String> DSL_CLASSES = Set.of(
            MessageContract.class.getName(),
            GivenStep.class.getName(),
            ThenContractStep.class.getName(),
            ThenCompatibilityStep.class.getName(),
            FileSnapshotStorage.class.getName());
    private static final String SOURCE_ROOT = "src/test/java";

    private final SnapshotLayout layout;
    private final String fileExtension;

    FileSnapshotStorage() {
        this(SnapshotLayout.legacy(), APPROVED_SUFFIX);
    }

    FileSnapshotStorage(SnapshotLayout layout, String fileExtension) {
        this.layout = layout;
        this.fileExtension = fileExtension;
    }

    @Override
    public void store(String name, byte[] payload) {
        if (layout.strategy() != SnapshotLayout.Strategy.LEGACY) {
            storeAtLayout(name, payload);
            return;
        }
        Approvals.verify(
                new String(payload, StandardCharsets.UTF_8),
                optionsFor(name).withReporter(new AutoApproveWhenEmptyReporter()));
    }

    @Override
    public Optional<byte[]> read(String name) {
        var path = layout.strategy() != SnapshotLayout.Strategy.LEGACY ? layoutPath(name) : resolvePath(name);
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void storeAtLayout(String name, byte[] payload) {
        var path = layoutPath(name);
        var parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        Approvals.verify(
                new String(payload, StandardCharsets.UTF_8),
                new Options()
                        .forFile()
                        .withExtension(fileExtension)
                        .withBaseName(baseNameOf(path))
                        .forFile()
                        .withNamer(namedAt(path, fileExtension))
                        .forFile()
                        .withExtension(fileExtension)
                        .withReporter(new AutoApproveWhenEmptyReporter()));
    }

    private Path layoutPath(String name) {
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .findFirst());
        var callerClass = requireCaller(frame);
        return layout.resolve(callerClass.getPackageName(), callerClass.getSimpleName(), name, null, fileExtension);
    }

    private String baseNameOf(Path path) {
        var fileName = path.getFileName().toString();
        var marker = SNAP_BASE_SUFFIX + ".approved" + fileExtension;
        return fileName.substring(0, fileName.length() - marker.length()) + SNAP_BASE_SUFFIX;
    }

    private Options optionsFor(String name) {
        if (name.endsWith(APPROVED_SUFFIX)) {
            return new Options().forFile().withNamer(namedAt(Path.of(name)));
        }
        return new Options().forFile().withBaseName(name);
    }

    private Path resolvePath(String name) {
        if (name.endsWith(APPROVED_SUFFIX)) {
            return Path.of(name);
        }
        return resolve(name);
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

    static Path resolve(String snapshotName) {
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.filter(f -> !DSL_CLASSES.contains(f.getClassName()))
                        .findFirst());
        var callerClass = requireCaller(frame);
        var packagePath = callerClass.getPackageName().replace('.', '/');
        return Path.of(SOURCE_ROOT, packagePath, snapshotName + ".approved.txt");
    }

    static Class<?> requireCaller(java.util.Optional<StackWalker.StackFrame> frame) {
        return frame.orElseThrow(() -> new IllegalStateException("Cannot determine calling test class from stack"))
                .getDeclaringClass();
    }
}
