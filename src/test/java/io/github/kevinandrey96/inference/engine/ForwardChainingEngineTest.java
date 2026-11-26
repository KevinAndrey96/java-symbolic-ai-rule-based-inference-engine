package io.github.kevinandrey96.inference.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kevinandrey96.inference.model.KnowledgeBase;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ForwardChainingEngineTest {

    @Test
    void firstDeclaredStrategyFollowsRuleOrder() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "H");

        assertTrue(result.isGoalReached());
        assertEquals(List.of("X", "D", "E", "F", "A", "H"), result.getDerivedFacts());
        assertEquals(6, result.getFiredRules());
    }

    @Test
    void mostSpecificStrategyReachesTheGoalInFewerCycles() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.MOST_SPECIFIC)
                .prove(Fixtures.classic(), "H");

        assertTrue(result.isGoalReached());
        assertEquals(List.of("X", "A", "H"), result.getDerivedFacts());
        assertEquals("R8", result.getJustification("A").orElseThrow().getId());
    }

    @Test
    void saturationDerivesEveryReachableFact() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .saturate(Fixtures.classic());

        assertFalse(result.getGoal().isPresent());
        assertEquals(Set.of("B", "C", "X", "D", "E", "F", "A", "H"), result.getFacts());
        assertFalse(result.getFacts().contains("G"));
        assertEquals(TraceStep.Kind.FIXPOINT, last(result).getKind());
    }

    @Test
    void reportsUnreachableGoals() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "G");

        assertFalse(result.isGoalReached());
        assertEquals(TraceStep.Kind.GOAL_NOT_REACHED, last(result).getKind());
    }

    @Test
    void goalAlreadyInInitialFactsNeedsNoRules() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "B");

        assertTrue(result.isGoalReached());
        assertEquals(0, result.getFiredRules());
    }

    @Test
    void terminatesOnCyclicRuleBases() throws Exception {
        KnowledgeBase kb = Fixtures.inline("A", "A=B\nB=A\nB=C");

        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).saturate(kb);

        assertEquals(Set.of("A", "B", "C"), result.getFacts());
    }

    @Test
    void explainsHowAFactWasDerived() throws Exception {
        InferenceResult result = new ForwardChainingEngine(ConflictResolutionStrategy.MOST_SPECIFIC)
                .prove(Fixtures.classic(), "H");

        assertEquals(List.of(
                "H was derived by R6: X AND A => H",
                "  X was derived by R4: B => X",
                "    B is an initial fact",
                "  A was derived by R8: X AND C => A",
                "    X was derived by R4: B => X",
                "      B is an initial fact",
                "    C is an initial fact"), result.explain("H"));
    }

    @Test
    void doesNotMutateTheKnowledgeBase() throws Exception {
        KnowledgeBase kb = Fixtures.classic();

        new ForwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).saturate(kb);

        assertEquals(Set.of("B", "C"), kb.getFacts());
        assertEquals(9, kb.getRules().size());
    }

    private static TraceStep last(InferenceResult result) {
        return result.getTrace().get(result.getTrace().size() - 1);
    }
}
