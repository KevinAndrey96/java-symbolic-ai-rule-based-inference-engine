package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.Rule;
import java.util.Comparator;
import java.util.List;

/**
 * Selects which applicable rule fires next when the conflict set holds more than one candidate.
 * The conflict set is always ordered by rule declaration, so ties fall back to declaration order.
 */
public enum ConflictResolutionStrategy {

    FIRST_DECLARED("first declared rule") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            return conflictSet.get(0);
        }
    },

    MOST_SPECIFIC("most specific rule (largest number of premises)") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            Rule best = conflictSet.get(0);
            for (Rule candidate : conflictSet) {
                if (candidate.getPremiseCount() > best.getPremiseCount()) {
                    best = candidate;
                }
            }

            return best;
        }

        @Override
        public Comparator<Rule> backwardOrder() {
            return Comparator.comparingInt(Rule::getPremiseCount).reversed();
        }
    };

    private final String description;

    ConflictResolutionStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Picks the rule to fire. {@code conflictSet} must not be empty.
     */
    public abstract Rule select(List<Rule> conflictSet);

    /**
     * Order in which backward chaining tries the rules that conclude the same goal. The sort is
     * stable, so rules with the same key keep their declaration order.
     */
    public Comparator<Rule> backwardOrder() {
        return (a, b) -> 0;
    }
}
