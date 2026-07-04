package io.eventdriven.strictland;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

record DiffTool(String name, List<String> candidates, List<String> arguments) {

    DiffTool {
        candidates = List.copyOf(candidates);
        arguments = List.copyOf(arguments);
    }

    Optional<ResolvedDiffTool> resolve(Predicate<String> isInstalled) {
        for (var candidate : candidates) {
            if (isInstalled.test(candidate) || executableFileExists(candidate)) {
                return Optional.of(new ResolvedDiffTool(name, candidate, arguments));
            }
        }
        return Optional.empty();
    }

    private static boolean executableFileExists(String candidate) {
        return candidate.contains(File.separator) && new File(candidate).canExecute();
    }
}
