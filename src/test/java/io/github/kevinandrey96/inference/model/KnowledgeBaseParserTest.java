package io.github.kevinandrey96.inference.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KnowledgeBaseParserTest {

    @Test
    void parsesFactsAcrossLinesIgnoringCommentsAndWhitespace() throws Exception {
        Set<String> facts = KnowledgeBaseParser.parseFacts("# initial facts\n B , C\n\nD");

        assertEquals(List.of("B", "C", "D"), List.copyOf(facts));
    }

    @Test
    void parsesRulesWithBothImplicationOperators() throws Exception {
        List<Rule> rules = KnowledgeBaseParser.parseRules("B,D,E=F\n# comment\nhas_hair => mammal");

        assertEquals(2, rules.size());
        assertEquals(new Rule("R1", List.of("B", "D", "E"), "F"), rules.get(0));
        assertEquals(new Rule("R2", List.of("has_hair"), "mammal"), rules.get(1));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
        "B,C            | line 1: rule must contain exactly one '=' or '=>'",
        "B=C=D          | line 1: rule must contain exactly one '=' or '=>'",
        "B,=C           | line 1: empty premise",
        "B=C,D          | line 1: rule must have exactly one conclusion",
        "B c=D          | line 1: invalid premise 'B c'",
    })
    void rejectsMalformedRules(String source, String expectedMessage) {
        KnowledgeBaseParseException ex = assertThrows(KnowledgeBaseParseException.class,
                () -> KnowledgeBaseParser.parseRules(source));

        assertEquals(expectedMessage, ex.getMessage());
        assertEquals(1, ex.getLineNumber());
    }

    @Test
    void reportsTheOffendingLineNumber() {
        KnowledgeBaseParseException ex = assertThrows(KnowledgeBaseParseException.class,
                () -> KnowledgeBaseParser.parseRules("A=B\n\nC=\n"));

        assertEquals(3, ex.getLineNumber());
    }

    @Test
    void loadsTheBundledClassicExample() throws Exception {
        KnowledgeBase kb = KnowledgeBaseParser.load(
                Path.of("examples/classic/facts.txt"), Path.of("examples/classic/rules.txt"));

        assertEquals(Set.of("B", "C"), kb.getFacts());
        assertEquals(9, kb.getRules().size());
        assertEquals(3, kb.rulesConcluding("A").size());
        assertTrue(kb.getVocabulary().contains("H"));
    }
}
