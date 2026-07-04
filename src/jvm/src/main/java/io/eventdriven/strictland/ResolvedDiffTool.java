package io.eventdriven.strictland;

import java.util.ArrayList;
import java.util.List;

record ResolvedDiffTool(String name, String executable, List<String> arguments) {

    ResolvedDiffTool {
        arguments = List.copyOf(arguments);
    }

    List<String> command(String received, String approved) {
        var argv = new ArrayList<String>(arguments.size() + 1);
        argv.add(executable);
        for (var token : arguments) {
            argv.add(token.replace("{received}", received).replace("{approved}", approved));
        }
        return List.copyOf(argv);
    }
}
