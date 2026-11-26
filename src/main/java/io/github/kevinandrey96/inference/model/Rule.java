package io.github.kevinandrey96.inference.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Immutable propositional Horn clause of the form {@code p1 AND p2 AND ... => conclusion}.
 */
public final class Rule {

    private final String id;
    private final List<String> premises;
    private final String conclusion;

    public Rule(String id, List<String> premises, String conclusion) {
        this.id = requireText(id, "rule id");
        this.conclusion = requireText(conclusion, "rule conclusion");
        if (premises == null || premises.isEmpty()) {
            throw new IllegalArgumentException("rule " + id + " must have at least one premise");
        }
        premises.forEach(p -> requireText(p, "rule premise"));
        this.premises = List.copyOf(premises);
    }

    public String getId() {
        return id;
    }

    public List<String> getPremises() {
        return premises;
    }

    public String getConclusion() {
        return conclusion;
    }

    public int getPremiseCount() {
        return premises.size();
    }

    public boolean isSatisfiedBy(Collection<String> facts) {
        return facts.containsAll(premises);
    }

    public boolean concludes(String goal) {
        return conclusion.equals(goal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rule)) {
            return false;
        }
        Rule other = (Rule) o;

        return id.equals(other.id) && premises.equals(other.premises) && conclusion.equals(other.conclusion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, premises, conclusion);
    }

    @Override
    public String toString() {
        return id + ": " + String.join(" AND ", premises) + " => " + conclusion;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value;
    }
}
