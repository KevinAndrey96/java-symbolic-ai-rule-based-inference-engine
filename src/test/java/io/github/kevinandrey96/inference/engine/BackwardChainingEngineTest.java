package io.github.kevinandrey96.inference.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kevinandrey96.inference.model.KnowledgeBase;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BackwardChainingEngineTest {

    @Test
    void provesTheGoalBySubgoalDecomposition() throws Exception {
        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "H");

        assertTrue(result.isGoalReached());
        assertEquals(List.of("X", "D", "E", "F", "A", "H"), result.getDerivedFacts());
        assertEquals("R3", result.getJustification("A").orElseThrow().getId());
        assertEquals("R7", result.getJustification("D").orElseThrow().getId());
    }

    @Test
    void mostSpecificStrategyTriesRulesWithMorePremisesFirst() throws Exception {
        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.MOST_SPECIFIC)
                .prove(Fixtures.classic(), "H");

        assertTrue(result.isGoalReached());
        assertEquals("R9", result.getJustification("D").orElseThrow().getId());
    }

    @Test
    void onlyDerivesFactsRelevantToTheGoal() throws Exception {
        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "X");

        assertEquals(List.of("X"), result.getDerivedFacts());
    }

    @Test
    void failsWhenAPremiseHasNoSupportingRule() throws Exception {
        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "G");

        assertFalse(result.isGoalReached());
        assertTrue(hasStep(result, TraceStep.Kind.FAILED, "no rule concludes G"));
    }

    @Test
    void prunesCyclesAndKeepsSearchingAlternatives() throws Exception {
        KnowledgeBase kb = Fixtures.inline("Z", "B=A\nA=B\nZ=B");

        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).prove(kb, "A");

        assertTrue(result.isGoalReached());
        assertTrue(hasStep(result, TraceStep.Kind.CYCLE_DETECTED, "cycle A -> B -> A, branch pruned"));
        assertEquals("R3", result.getJustification("B").orElseThrow().getId());
    }

    @Test
    void terminatesOnPurelyCyclicRuleBases() throws Exception {
        KnowledgeBase kb = Fixtures.inline("", "A=B\nB=C\nC=A");

        InferenceResult result = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED).prove(kb, "A"));

        assertFalse(result.isGoalReached());
    }

    @Test
    void traceDepthMirrorsTheProofTree() throws Exception {
        InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.FIRST_DECLARED)
                .prove(Fixtures.classic(), "X");

        assertEquals(List.of(
                "[PHASE] backward chaining, rule order: first declared rule",
                "[SUBGOAL] to prove X try R4: B => X",
                "  [FACT_KNOWN] B is known",
                "[PROVEN] X proven by R4",
                "[GOAL_REACHED] X proven, facts {B, C, X}"),
                result.getTrace().stream().map(TraceStep::toString).collect(Collectors.toList()));
    }

    private static boolean hasStep(InferenceResult result, TraceStep.Kind kind, String message) {
        return result.getTrace().stream().anyMatch(s -> s.getKind() == kind && s.getMessage().equals(message));
    }
}
