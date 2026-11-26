package io.github.kevinandrey96.inference.engine;

/**
 * One entry of the inference trace. {@code depth} is the recursion level for backward chaining
 * and zero for forward chaining, so the trace can be rendered as an indented tree.
 */
public final class TraceStep {

    /**
     * Category of a trace step.
     */
    public enum Kind {
        PHASE,
        CONFLICT_SET,
        RULE_FIRED,
        SUBGOAL,
        FACT_KNOWN,
        PROVEN,
        FAILED,
        CYCLE_DETECTED,
        FIXPOINT,
        GOAL_REACHED,
        GOAL_NOT_REACHED
    }

    private final Kind kind;
    private final int depth;
    private final String message;

    public TraceStep(Kind kind, int depth, String message) {
        this.kind = kind;
        this.depth = depth;
        this.message = message;
    }

    public Kind getKind() {
        return kind;
    }

    public int getDepth() {
        return depth;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "  ".repeat(depth) + "[" + kind + "] " + message;
    }
}
