package io.github.kevinandrey96.inference.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InferenceCliTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @ParameterizedTest
    @CsvSource({
        "classic,         H,                     forward",
        "classic,         H,                     backward",
        "classic,         H,                     hybrid",
        "animals,         cheetah,               hybrid",
        "incident-triage, rollback_release,      backward",
        "incident-triage, page_on_call_engineer, forward",
    })
    void provesGoalsOfTheBundledExamples(String example, String goal, String mode) {
        int code = run("--example", example, "--goal", goal, "--mode", mode);

        assertEquals(InferenceCli.PROVEN, code, stdout());
        assertTrue(stdout().contains("result: " + goal + " proven"));
        assertTrue(stdout().contains("explanation:"));
    }

    @Test
    void explainsTheAnimalIdentification() {
        run("--example", "animals", "--goal", "cheetah", "--quiet");

        assertTrue(stdout().contains("cheetah was derived by R9: carnivore AND has_tawny_color AND has_dark_spots"));
        assertTrue(stdout().contains("mammal was derived by R1: has_hair => mammal"));
    }

    @Test
    void returnsNotProvenForUnreachableGoals() {
        int code = run("--example", "animals", "--goal", "zebra");

        assertEquals(InferenceCli.NOT_PROVEN, code);
        assertTrue(stdout().contains("result: zebra cannot be proven"));
    }

    @Test
    void saturatesWhenNoGoalIsGiven() {
        int code = run("--example", "classic", "--quiet");

        assertEquals(InferenceCli.PROVEN, code);
        assertTrue(stdout().contains("final facts:   [B, C, X, D, E, F, A, H]"));
    }

    @Test
    void comparesAllEngines() {
        int code = run("--example", "classic", "--goal", "H", "--mode", "compare", "--strategy", "specific");

        assertEquals(InferenceCli.PROVEN, code);
        assertTrue(stdout().contains("forward"));
        assertTrue(stdout().contains("backward"));
        assertTrue(stdout().contains("hybrid"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "--goal H                              | a knowledge base is required",
        "--example classic --mode backward     | --goal is required for mode backward",
        "--example classic --strategy random   | unknown strategy random",
        "--example classic --goal              | missing value for --goal",
        "--example missing --goal H            | knowledge base read",
        "--verbose                             | unknown option --verbose",
    })
    void rejectsInvalidInput(String args, String expectedError) {
        int code = run(args.split(" "));

        assertEquals(InferenceCli.INVALID_INPUT, code);
        assertTrue(stderr().contains(expectedError), stderr());
    }

    @Test
    void printsUsage() {
        int code = run("--help");

        assertEquals(InferenceCli.PROVEN, code);
        assertTrue(stdout().startsWith("usage: inference-engine"));
    }

    private int run(String... args) {
        PrintStream o = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream e = new PrintStream(err, true, StandardCharsets.UTF_8);

        return new InferenceCli(o, e).run(args);
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }
}
