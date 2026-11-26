package io.github.kevinandrey96.inference.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleTest {

    @Test
    void isSatisfiedOnlyWhenAllPremisesAreKnown() {
        Rule rule = new Rule("R1", List.of("B", "D"), "E");

        assertTrue(rule.isSatisfiedBy(Set.of("B", "D", "X")));
        assertFalse(rule.isSatisfiedBy(Set.of("B")));
    }

    @Test
    void isImmutable() {
        List<String> premises = new ArrayList<>(List.of("A"));
        Rule rule = new Rule("R1", premises, "B");
        premises.add("C");

        assertEquals(List.of("A"), rule.getPremises());
        assertThrows(UnsupportedOperationException.class, () -> rule.getPremises().add("D"));
    }

    @Test
    void rejectsRulesWithoutPremises() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Rule("R1", List.of(), "B"));

        assertEquals("rule R1 must have at least one premise", ex.getMessage());
    }

    @Test
    void rendersAsReadableClause() {
        assertEquals("R3: C AND F AND B => A", new Rule("R3", List.of("C", "F", "B"), "A").toString());
    }
}
