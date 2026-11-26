package io.github.kevinandrey96.inference.cli;

import io.github.kevinandrey96.inference.engine.BackwardChainingEngine;
import io.github.kevinandrey96.inference.engine.ForwardChainingEngine;
import io.github.kevinandrey96.inference.engine.HybridChainingEngine;
import io.github.kevinandrey96.inference.engine.InferenceEngine;
import io.github.kevinandrey96.inference.engine.InferenceResult;
import io.github.kevinandrey96.inference.engine.TraceStep;
import io.github.kevinandrey96.inference.model.KnowledgeBase;
import io.github.kevinandrey96.inference.model.KnowledgeBaseParseException;
import io.github.kevinandrey96.inference.model.KnowledgeBaseParser;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Command line entry point. Prints the knowledge base summary, the inference trace and a "how"
 * explanation of the goal.
 */
public final class InferenceCli {

    static final int PROVEN = 0;
    static final int NOT_PROVEN = 1;
    static final int INVALID_INPUT = 2;

    private final PrintStream out;
    private final PrintStream err;

    InferenceCli(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new InferenceCli(System.out, System.err).run(args));
    }

    int run(String[] args) {
        CliOptions options;
        KnowledgeBase knowledgeBase;
        try {
            options = CliOptions.parse(args);
            if (options.help) {
                out.println(CliOptions.usage());

                return PROVEN;
            }
            knowledgeBase = KnowledgeBaseParser.load(options.factsFile, options.rulesFile);
        } catch (IllegalArgumentException | KnowledgeBaseParseException e) {
            err.println("error: " + e.getMessage());
            err.println(CliOptions.usage());

            return INVALID_INPUT;
        } catch (IOException e) {
            err.println("error: knowledge base read: " + e.getMessage());

            return INVALID_INPUT;
        }

        out.printf("knowledge base: %d facts, %d rules%n", knowledgeBase.getFacts().size(),
                knowledgeBase.getRules().size());
        out.printf("mode: %s, strategy: %s%n%n", options.mode.name().toLowerCase(Locale.ROOT),
                options.strategy.getDescription());

        switch (options.mode) {
            case SATURATE:
                return saturate(knowledgeBase, options);
            case COMPARE:
                return compare(knowledgeBase, options);
            default:
                return prove(engine(options), knowledgeBase, options);
        }
    }

    private int saturate(KnowledgeBase knowledgeBase, CliOptions options) {
        InferenceResult result = new ForwardChainingEngine(options.strategy).saturate(knowledgeBase);
        printTrace(result, options);
        out.println("derived facts: " + result.getDerivedFacts());
        out.println("final facts:   " + result.getFacts());

        return PROVEN;
    }

    private int prove(InferenceEngine engine, KnowledgeBase knowledgeBase, CliOptions options) {
        InferenceResult result = engine.prove(knowledgeBase, options.goal);
        printTrace(result, options);
        if (!result.isGoalReached()) {
            out.println("result: " + options.goal + " cannot be proven");

            return NOT_PROVEN;
        }
        out.printf("result: %s proven, %d rules fired%n%n", options.goal, result.getFiredRules());
        out.println("explanation:");
        result.explain(options.goal).forEach(line -> out.println("  " + line));

        return PROVEN;
    }

    private int compare(KnowledgeBase knowledgeBase, CliOptions options) {
        Map<String, InferenceEngine> engines = new LinkedHashMap<>();
        engines.put("forward", new ForwardChainingEngine(options.strategy));
        engines.put("backward", new BackwardChainingEngine(options.strategy));
        engines.put("hybrid", new HybridChainingEngine(options.strategy));

        out.printf("%-10s %-8s %-7s %s%n", "engine", "proven", "fired", "derived facts");
        boolean proven = false;
        for (Map.Entry<String, InferenceEngine> entry : engines.entrySet()) {
            InferenceResult result = entry.getValue().prove(knowledgeBase, options.goal);
            proven = result.isGoalReached();
            out.printf("%-10s %-8s %-7d %s%n", entry.getKey(), proven, result.getFiredRules(),
                    result.getDerivedFacts());
        }

        return proven ? PROVEN : NOT_PROVEN;
    }

    private void printTrace(InferenceResult result, CliOptions options) {
        if (!options.showTrace) {
            return;
        }
        out.println("trace:");
        for (TraceStep step : result.getTrace()) {
            out.println("  " + step);
        }
        out.println();
    }

    private static InferenceEngine engine(CliOptions options) {
        switch (options.mode) {
            case FORWARD:
                return new ForwardChainingEngine(options.strategy);
            case BACKWARD:
                return new BackwardChainingEngine(options.strategy);
            default:
                return new HybridChainingEngine(options.strategy);
        }
    }
}
