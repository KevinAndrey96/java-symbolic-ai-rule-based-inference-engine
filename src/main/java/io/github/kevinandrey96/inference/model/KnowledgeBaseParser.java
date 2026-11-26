package io.github.kevinandrey96.inference.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses the plain-text knowledge base format.
 *
 * <p>Facts: comma separated symbols, one or more lines ({@code B, C}).
 * Rules: one rule per line, premises separated by commas and the conclusion after {@code =} or
 * {@code =>} ({@code B, D, E => F}). Blank lines and lines starting with {@code #} are ignored.
 */
public final class KnowledgeBaseParser {

    private static final Pattern SYMBOL = Pattern.compile("[\\p{L}\\p{N}_\\-]+");
    private static final Pattern IMPLICATION = Pattern.compile("=>|=");

    private KnowledgeBaseParser() {
    }

    public static KnowledgeBase load(Path factsFile, Path rulesFile) throws IOException, KnowledgeBaseParseException {
        String facts = Files.readString(factsFile, StandardCharsets.UTF_8);
        String rules = Files.readString(rulesFile, StandardCharsets.UTF_8);

        return new KnowledgeBase(parseFacts(facts), parseRules(rules));
    }

    public static Set<String> parseFacts(String source) throws KnowledgeBaseParseException {
        Set<String> facts = new LinkedHashSet<>();
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (isIgnorable(line)) {
                continue;
            }
            facts.addAll(parseSymbols(line, i + 1, "fact"));
        }

        return facts;
    }

    public static List<Rule> parseRules(String source) throws KnowledgeBaseParseException {
        List<Rule> rules = new ArrayList<>();
        String[] lines = source.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (isIgnorable(line)) {
                continue;
            }
            rules.add(parseRule(line, i + 1, "R" + (rules.size() + 1)));
        }

        return rules;
    }

    private static Rule parseRule(String line, int lineNumber, String id) throws KnowledgeBaseParseException {
        String[] sides = IMPLICATION.split(line, -1);
        if (sides.length != 2) {
            throw new KnowledgeBaseParseException(lineNumber, "rule must contain exactly one '=' or '=>'");
        }
        List<String> premises = parseSymbols(sides[0], lineNumber, "premise");
        List<String> conclusion = parseSymbols(sides[1], lineNumber, "conclusion");
        if (conclusion.size() != 1) {
            throw new KnowledgeBaseParseException(lineNumber, "rule must have exactly one conclusion");
        }

        return new Rule(id, premises, conclusion.get(0));
    }

    private static List<String> parseSymbols(String text, int lineNumber, String kind)
            throws KnowledgeBaseParseException {
        List<String> symbols = new ArrayList<>();
        for (String token : text.split(",", -1)) {
            String symbol = token.strip();
            if (symbol.isEmpty()) {
                throw new KnowledgeBaseParseException(lineNumber, "empty " + kind);
            }
            if (!SYMBOL.matcher(symbol).matches()) {
                throw new KnowledgeBaseParseException(lineNumber, "invalid " + kind + " '" + symbol + "'");
            }
            symbols.add(symbol);
        }

        return symbols;
    }

    private static boolean isIgnorable(String line) {
        return line.isEmpty() || line.startsWith("#");
    }
}
