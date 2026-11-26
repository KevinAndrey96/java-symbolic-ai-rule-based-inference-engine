package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.KnowledgeBase;
import io.github.kevinandrey96.inference.model.KnowledgeBaseParser;
import java.nio.file.Path;

final class Fixtures {

    private Fixtures() {
    }

    static KnowledgeBase classic() throws Exception {
        return KnowledgeBaseParser.load(Path.of("examples/classic/facts.txt"), Path.of("examples/classic/rules.txt"));
    }

    static KnowledgeBase inline(String facts, String rules) throws Exception {
        return new KnowledgeBase(KnowledgeBaseParser.parseFacts(facts), KnowledgeBaseParser.parseRules(rules));
    }
}
