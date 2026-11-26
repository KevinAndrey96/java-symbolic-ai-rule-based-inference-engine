package io.github.kevinandrey96.inference.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of the initial facts and the rule base. Engines copy the facts into their own
 * working memory, so the same knowledge base can be reused across inference runs.
 */
public final class KnowledgeBase {

    private final Set<String> facts;
    private final List<Rule> rules;

    public KnowledgeBase(Set<String> facts, List<Rule> rules) {
        this.facts = Collections.unmodifiableSet(new LinkedHashSet<>(facts));
        this.rules = List.copyOf(rules);
    }

    public Set<String> getFacts() {
        return facts;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public List<Rule> rulesConcluding(String goal) {
        return rules.stream().filter(r -> r.concludes(goal)).collect(Collectors.toList());
    }

    public Set<String> getVocabulary() {
        Set<String> symbols = new LinkedHashSet<>(facts);
        for (Rule rule : rules) {
            symbols.addAll(rule.getPremises());
            symbols.add(rule.getConclusion());
        }

        return Collections.unmodifiableSet(symbols);
    }
}
