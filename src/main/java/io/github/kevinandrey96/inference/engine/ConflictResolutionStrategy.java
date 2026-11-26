package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.Rule;
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
}
