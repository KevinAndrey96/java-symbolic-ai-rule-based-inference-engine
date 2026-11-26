package io.github.kevinandrey96.inference.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kevinandrey96.inference.model.KnowledgeBase;
import java.util.List;
import org.junit.jupiter.api.Test;

class HybridChainingEngineTest {

    private static final String BRANCHING_RULES = "A=B\nA=C\nC=D\nB=G";

    @Test
    void derivesOnlyFactsThatCanSupportTheGoal() throws Exception {
        KnowledgeBase kb = Fixtures.inline("A", BRANCHING_RULES);

        InferenceResult forward = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).prove(kb, "G");
        InferenceResult hybrid = new HybridChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).prove(kb, "G");

        assertEquals(List.of("B", "C", "D", "G"), forward.getDerivedFacts());
        assertEquals(List.of("B", "G"), hybrid.getDerivedFacts());
        assertTrue(hybrid.isGoalReached());
    }

    @Test
    void tracesTheRelevanceAnalysisBeforeTheForwardPass() throws Exception {
        KnowledgeBase kb = Fixtures.inline("A", BRANCHING_RULES);

        InferenceResult result = new HybridChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).prove(kb, "G");

        assertEquals("relevant rules [R1, R4], pruned [R2, R3]", result.getTrace().get(1).getMessage());
        assertEquals(TraceStep.Kind.PHASE, result.getTrace().get(2).getKind());
    }

    @Test
    void agreesWithForwardChainingOnReachability() throws Exception {
        KnowledgeBase kb = Fixtures.classic();
        for (String goal : kb.getVocabulary()) {
            boolean forward = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                    .prove(kb, goal).isGoalReached();
            boolean backward = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                    .prove(kb, goal).isGoalReached();
            boolean hybrid = new HybridChainingEngine(ConflictResolutionStrategy.MOST_SPECIFIC)
                    .prove(kb, goal).isGoalReached();

            assertEquals(forward, backward, "backward disagrees on " + goal);
            assertEquals(forward, hybrid, "hybrid disagrees on " + goal);
        }
    }

    @Test
    void failsCleanlyWhenNoRuleConcludesTheGoal() throws Exception {
        InferenceResult result = new HybridChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "G");

        assertFalse(result.isGoalReached());
        assertEquals(0, result.getFiredRules());
    }
}
