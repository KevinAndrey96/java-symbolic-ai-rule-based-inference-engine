package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.engine.TraceStep.Kind;
import io.github.kevinandrey96.inference.model.KnowledgeBase;
import io.github.kevinandrey96.inference.model.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Goal-driven, depth-first inference (SLD resolution over propositional Horn clauses). Goals on
 * the current proof path are tracked to cut cycles such as {@code A => B, B => A}, and goals that
 * failed without touching a cycle are memoized so they are not explored twice.
 */
public final class BackwardChainingEngine implements InferenceEngine {

    private final ConflictResolutionStrategy strategy;

    public BackwardChainingEngine(ConflictResolutionStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public InferenceResult prove(KnowledgeBase knowledgeBase, String goal) {
        Objects.requireNonNull(goal, "goal");
        Run run = new Run(knowledgeBase, new WorkingMemory(knowledgeBase.getFacts()));
        run.trace.add(new TraceStep(Kind.PHASE, 0, "backward chaining, rule order: " + strategy.getDescription()));

        return run.prove(goal);
    }

    private final class Run {

        private final KnowledgeBase knowledgeBase;
        private final WorkingMemory memory;
        private final List<TraceStep> trace = new ArrayList<>();
        private final Set<String> path = new LinkedHashSet<>();
        private final Set<String> failed = new HashSet<>();
        private int cycleHits;
        private int firedRules;

        Run(KnowledgeBase knowledgeBase, WorkingMemory memory) {
            this.knowledgeBase = knowledgeBase;
            this.memory = memory;
        }

        InferenceResult prove(String goal) {
            boolean proven = solve(goal, 0);
            if (proven) {
                trace.add(new TraceStep(Kind.GOAL_REACHED, 0, goal + " proven, facts " + memory.render()));
            } else {
                trace.add(new TraceStep(Kind.GOAL_NOT_REACHED, 0, goal + " cannot be proven"));
            }

            return memory.toResult(goal, trace, firedRules);
        }

        private boolean solve(String goal, int depth) {
            if (memory.contains(goal)) {
                trace.add(new TraceStep(Kind.FACT_KNOWN, depth, goal + " is known"));

                return true;
            }
            if (path.contains(goal)) {
                cycleHits++;
                trace.add(new TraceStep(Kind.CYCLE_DETECTED, depth, "cycle " + renderPath(goal) + ", branch pruned"));

                return false;
            }
            if (failed.contains(goal)) {
                trace.add(new TraceStep(Kind.FAILED, depth, goal + " already failed"));

                return false;
            }

            List<Rule> candidates = knowledgeBase.rulesConcluding(goal);
            candidates.sort(strategy.backwardOrder());
            if (candidates.isEmpty()) {
                trace.add(new TraceStep(Kind.FAILED, depth, "no rule concludes " + goal));
                failed.add(goal);

                return false;
            }

            int cycleHitsBefore = cycleHits;
            path.add(goal);
            for (Rule rule : candidates) {
                trace.add(new TraceStep(Kind.SUBGOAL, depth, "to prove " + goal + " try " + rule));
                if (allPremisesHold(rule, depth + 1)) {
                    path.remove(goal);
                    memory.derive(goal, rule);
                    firedRules++;
                    trace.add(new TraceStep(Kind.PROVEN, depth, goal + " proven by " + rule.getId()));

                    return true;
                }
            }
            path.remove(goal);

            trace.add(new TraceStep(Kind.FAILED, depth, "every rule concluding " + goal + " failed"));
            if (cycleHits == cycleHitsBefore) {
                failed.add(goal);
            }

            return false;
        }

        private boolean allPremisesHold(Rule rule, int depth) {
            for (String premise : rule.getPremises()) {
                if (!solve(premise, depth)) {
                    return false;
                }
            }

            return true;
        }

        private String renderPath(String goal) {
            List<String> nodes = new ArrayList<>(path);
            nodes.add(goal);

            return String.join(" -> ", nodes);
        }
    }
}
