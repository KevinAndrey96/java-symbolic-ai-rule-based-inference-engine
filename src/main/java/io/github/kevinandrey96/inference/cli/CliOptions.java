package io.github.kevinandrey96.inference.cli;

import io.github.kevinandrey96.inference.engine.ConflictResolutionStrategy;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Parsed command line arguments.
 */
final class CliOptions {

    enum Mode { FORWARD, BACKWARD, HYBRID, SATURATE, COMPARE }

    Path factsFile;
    Path rulesFile;
    String goal;
    Mode mode;
    ConflictResolutionStrategy strategy = ConflictResolutionStrategy.FIRST_DECLARED;
    boolean showTrace = true;
    boolean help;

    static CliOptions parse(String[] args) {
        CliOptions options = new CliOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--facts":
                    options.factsFile = Path.of(value(args, ++i, arg));
                    break;
                case "--rules":
                    options.rulesFile = Path.of(value(args, ++i, arg));
                    break;
                case "--example":
                    Path dir = Path.of("examples", value(args, ++i, arg));
                    options.factsFile = dir.resolve("facts.txt");
                    options.rulesFile = dir.resolve("rules.txt");
                    break;
                case "--goal":
                    options.goal = value(args, ++i, arg);
                    break;
                case "--mode":
                    options.mode = parseMode(value(args, ++i, arg));
                    break;
                case "--strategy":
                    options.strategy = parseStrategy(value(args, ++i, arg));
                    break;
                case "--quiet":
                    options.showTrace = false;
                    break;
                case "-h":
                case "--help":
                    options.help = true;
                    break;
                default:
                    throw new IllegalArgumentException("unknown option " + arg);
            }
        }
        options.validate();

        return options;
    }

    private void validate() {
        if (help) {
            return;
        }
        if (factsFile == null || rulesFile == null) {
            throw new IllegalArgumentException("a knowledge base is required: use --example or --facts and --rules");
        }
        if (mode == null) {
            mode = goal == null ? Mode.SATURATE : Mode.HYBRID;
        }
        if (goal == null && mode != Mode.SATURATE) {
            throw new IllegalArgumentException("--goal is required for mode " + mode.name().toLowerCase(Locale.ROOT));
        }
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("missing value for " + option);
        }

        return args[index];
    }

    private static Mode parseMode(String value) {
        try {
            return Mode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown mode " + value, e);
        }
    }

    private static ConflictResolutionStrategy parseStrategy(String value) {
        switch (value.toLowerCase(Locale.ROOT)) {
            case "first":
                return ConflictResolutionStrategy.FIRST_DECLARED;
            case "specific":
                return ConflictResolutionStrategy.MOST_SPECIFIC;
            default:
                throw new IllegalArgumentException("unknown strategy " + value);
        }
    }

    static String usage() {
        return String.join(System.lineSeparator(),
                "usage: inference-engine (--example NAME | --facts FILE --rules FILE) [options]",
                "",
                "  --example NAME      bundled knowledge base: classic, animals, incident-triage",
                "  --facts FILE        comma separated initial facts",
                "  --rules FILE        one rule per line: p1, p2 => conclusion",
                "  --goal SYMBOL       fact to prove",
                "  --mode MODE         forward | backward | hybrid | saturate | compare",
                "                      (default: hybrid with --goal, saturate without)",
                "  --strategy NAME     first | specific (default: first)",
                "  --quiet             print only the result and the explanation",
                "  -h, --help          show this help",
                "",
                "exit codes: 0 goal proven, 1 goal not proven, 2 invalid input");
    }
}
