package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.engine.TraceStep.Kind;
import io.github.kevinandrey96.inference.model.KnowledgeBase;
import io.github.kevinandrey96.inference.model.Rule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mixed chaining. A backward pass walks from the goal through rule conclusions to find the rules
 * that can contribute to it; a forward pass then fires only those rules. The result is the same
 * as plain forward chaining, but facts that cannot support the goal are never derived.
 */
public final class HybridChainingEngine implements InferenceEngine {

    private final ConflictResolutionStrategy strategy;

    public HybridChainingEngine(ConflictResolutionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public InferenceResult prove(KnowledgeBase knowledgeBase, String goal) {
        Objects.requireNonNull(goal, "goal");
        List<TraceStep> trace = new ArrayList<>();
        trace.add(new TraceStep(Kind.PHASE, 0, "hybrid chaining, backward relevance analysis for " + goal));

        List<Rule> relevant = relevantRules(knowledgeBase, goal);
        List<Rule> pruned = knowledgeBase.getRules().stream()
                .filter(r -> !relevant.contains(r))
                .collect(Collectors.toList());
        trace.add(new TraceStep(Kind.SUBGOAL, 0, "relevant rules " + ForwardChainingEngine.ids(relevant)
                + ", pruned " + ForwardChainingEngine.ids(pruned)));

        KnowledgeBase focused = new KnowledgeBase(knowledgeBase.getFacts(), relevant);
        InferenceResult forward = new ForwardChainingEngine(strategy).prove(focused, goal);

        return forward.withLeadingTrace(trace);
    }

    static List<Rule> relevantRules(KnowledgeBase knowledgeBase, String goal) {
        Set<String> visited = new HashSet<>();
        Set<Rule> relevant = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(goal);
        while (!pending.isEmpty()) {
            String symbol = pending.pop();
            if (!visited.add(symbol) || knowledgeBase.getFacts().contains(symbol)) {
                continue;
            }
            for (Rule rule : knowledgeBase.rulesConcluding(symbol)) {
                relevant.add(rule);
                rule.getPremises().forEach(pending::push);
            }
        }

        return knowledgeBase.getRules().stream().filter(relevant::contains).collect(Collectors.toList());
    }
}
