package io.github.kevinandrey96.inference.engine;

import io.github.kevinandrey96.inference.model.KnowledgeBase;

/**
 * Common contract for every chaining strategy.
 */
public interface InferenceEngine {

    /**
     * Tries to establish {@code goal} from the knowledge base. The knowledge base is never mutated.
     */
    InferenceResult prove(KnowledgeBase knowledgeBase, String goal);
}
