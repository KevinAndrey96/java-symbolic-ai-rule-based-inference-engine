package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.Rule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable fact store used during a single inference run. Every derived fact records the rule
 * that produced it, which is what makes the result explainable.
 */
final class WorkingMemory {

    private final Set<String> facts;
    private final Map<String, Rule> justifications = new LinkedHashMap<>();
    private final List<String> derivedFacts = new ArrayList<>();

    WorkingMemory(Collection<String> initialFacts) {
        this.facts = new LinkedHashSet<>(initialFacts);
    }

    boolean contains(String fact) {
        return facts.contains(fact);
    }

    Set<String> facts() {
        return facts;
    }

    void derive(String fact, Rule rule) {
        if (facts.add(fact)) {
            justifications.put(fact, rule);
            derivedFacts.add(fact);
        }
    }

    String render() {
        return "{" + String.join(", ", facts) + "}";
    }

    InferenceResult toResult(String goal, List<TraceStep> trace, int firedRules) {
        return new InferenceResult(goal, facts, derivedFacts, justifications, trace, firedRules);
    }
}
