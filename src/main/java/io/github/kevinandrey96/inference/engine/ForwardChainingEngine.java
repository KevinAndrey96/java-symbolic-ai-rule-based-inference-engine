package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.engine.TraceStep.Kind;
import io.github.kevinandrey96.inference.model.KnowledgeBase;
import io.github.kevinandrey96.inference.model.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data-driven inference using the recognize-act cycle: build the conflict set, pick one rule with
 * the configured strategy, fire it and repeat until the goal appears or a fixpoint is reached.
 * Refraction guarantees each rule fires at most once, so termination is bounded by the rule count.
 */
public final class ForwardChainingEngine implements InferenceEngine {

    private final ConflictResolutionStrategy strategy;

    public ForwardChainingEngine(ConflictResolutionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public InferenceResult prove(KnowledgeBase knowledgeBase, String goal) {
        Objects.requireNonNull(goal, "goal");

        return run(knowledgeBase, goal);
    }

    /**
     * Fires every applicable rule until no new fact can be derived.
     */
    public InferenceResult saturate(KnowledgeBase knowledgeBase) {
        return run(knowledgeBase, null);
    }

    private InferenceResult run(KnowledgeBase knowledgeBase, String goal) {
        WorkingMemory memory = new WorkingMemory(knowledgeBase.getFacts());
        List<TraceStep> trace = new ArrayList<>();
        trace.add(new TraceStep(Kind.PHASE, 0, "forward chaining, strategy: " + strategy.getDescription()));
        trace.add(new TraceStep(Kind.FACT_KNOWN, 0, "initial facts " + memory.render()));

        if (goal != null && memory.contains(goal)) {
            trace.add(new TraceStep(Kind.GOAL_REACHED, 0, goal + " is an initial fact"));

            return memory.toResult(goal, trace, 0);
        }

        Set<String> fired = new HashSet<>();
        int cycles = 0;
        while (true) {
            List<Rule> conflictSet = conflictSet(knowledgeBase, memory, fired);
            if (conflictSet.isEmpty()) {
                trace.add(new TraceStep(Kind.FIXPOINT, 0, "no applicable rules left " + memory.render()));
                break;
            }
            trace.add(new TraceStep(Kind.CONFLICT_SET, 0, "conflict set " + ids(conflictSet)));

            Rule rule = strategy.select(conflictSet);
            fired.add(rule.getId());
            memory.derive(rule.getConclusion(), rule);
            cycles++;
            trace.add(new TraceStep(Kind.RULE_FIRED, 0, rule + " fires, facts " + memory.render()));

            if (goal != null && memory.contains(goal)) {
                trace.add(new TraceStep(Kind.GOAL_REACHED, 0, goal + " derived after " + cycles + " cycles"));

                return memory.toResult(goal, trace, cycles);
            }
        }

        if (goal != null) {
            trace.add(new TraceStep(Kind.GOAL_NOT_REACHED, 0, goal + " cannot be derived"));
        }

        return memory.toResult(goal, trace, cycles);
    }

    static List<Rule> conflictSet(KnowledgeBase knowledgeBase, WorkingMemory memory, Set<String> fired) {
        return knowledgeBase.getRules().stream()
                .filter(r -> !fired.contains(r.getId()))
                .filter(r -> !memory.contains(r.getConclusion()))
                .filter(r -> r.isSatisfiedBy(memory.facts()))
                .collect(Collectors.toList());
    }

    static String ids(List<Rule> rules) {
        return rules.stream().map(Rule::getId).collect(Collectors.joining(", ", "[", "]"));
    }
}
