package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.Rule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable outcome of an inference run: final working memory, derivation order, the rule that
 * justifies every derived fact and the full step-by-step trace.
 */
public final class InferenceResult {

    private final String goal;
    private final Set<String> facts;
    private final List<String> derivedFacts;
    private final Map<String, Rule> justifications;
    private final List<TraceStep> trace;
    private final int firedRules;

    InferenceResult(String goal, Set<String> facts, List<String> derivedFacts,
                    Map<String, Rule> justifications, List<TraceStep> trace, int firedRules) {
        this.goal = goal;
        this.facts = Collections.unmodifiableSet(new LinkedHashSet<>(facts));
        this.derivedFacts = List.copyOf(derivedFacts);
        this.justifications = Collections.unmodifiableMap(new LinkedHashMap<>(justifications));
        this.trace = List.copyOf(trace);
        this.firedRules = firedRules;
    }

    public Optional<String> getGoal() {
        return Optional.ofNullable(goal);
    }

    public boolean isGoalReached() {
        return goal != null && facts.contains(goal);
    }

    public Set<String> getFacts() {
        return facts;
    }

    public List<String> getDerivedFacts() {
        return derivedFacts;
    }

    public Optional<Rule> getJustification(String fact) {
        return Optional.ofNullable(justifications.get(fact));
    }

    public List<TraceStep> getTrace() {
        return trace;
    }

    public int getFiredRules() {
        return firedRules;
    }

    InferenceResult withLeadingTrace(List<TraceStep> leading) {
        List<TraceStep> merged = new ArrayList<>(leading);
        merged.addAll(trace);

        return new InferenceResult(goal, facts, derivedFacts, justifications, merged, firedRules);
    }

    /**
     * Builds a "how" explanation: the derivation tree of {@code fact} down to the initial facts.
     */
    public List<String> explain(String fact) {
        List<String> lines = new ArrayList<>();
        if (!facts.contains(fact)) {
            lines.add(fact + " is not known");

            return lines;
        }
        explain(fact, 0, lines);

        return lines;
    }

    private void explain(String fact, int depth, List<String> lines) {
        String indent = "  ".repeat(depth);
        Rule rule = justifications.get(fact);
        if (rule == null) {
            lines.add(indent + fact + " is an initial fact");

            return;
        }
        lines.add(indent + fact + " was derived by " + rule);
        for (String premise : rule.getPremises()) {
            explain(premise, depth + 1, lines);
        }
    }
}
