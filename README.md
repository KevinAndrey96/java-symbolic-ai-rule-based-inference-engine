# Symbolic AI Rule-Based Inference Engine
### Explainable Forward, Backward and Hybrid Chaining over Horn Clauses in Java
**A dependency-free expert system shell with conflict resolution, cycle detection and "how" explanations**

[![CI](https://github.com/KevinAndrey96/java-symbolic-ai-rule-based-inference-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/KevinAndrey96/java-symbolic-ai-rule-based-inference-engine/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-11%20%7C%2017%20%7C%2021-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.10%20%C2%B7%2049%20tests-25A162?logo=junit5)](https://junit.org/junit5/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage%20%C2%B7%20Non--Root-2496ED?logo=docker)](https://www.docker.com/)
[![Dependencies](https://img.shields.io/badge/Runtime%20dependencies-0-informational)](pom.xml)

Symbolic AI (also called GOFAI, knowledge-based systems or expert systems) solves problems by reasoning over explicit facts and `IF ... THEN ...` rules instead of learned weights. Every conclusion can be traced back to the rules and facts that produced it, which is why rule engines still power decision support, compliance checks, configuration validation and runbook automation.

This repository implements the three classic inference strategies of a production rule system on propositional Horn clauses:

| Strategy | Direction | Best when | Engine |
| :--- | :--- | :--- | :--- |
| **Forward chaining** | Data driven: facts to conclusions | You want everything that follows from the data | `ForwardChainingEngine` |
| **Backward chaining** | Goal driven: hypothesis to evidence | You have one question to answer | `BackwardChainingEngine` |
| **Hybrid chaining** | Backward relevance analysis, then forward firing | Large rule bases where most rules are irrelevant to the goal | `HybridChainingEngine` |

---

## ✨ Features

* **Three interchangeable engines** behind one `InferenceEngine` interface, verified to agree on reachability for every symbol of the reference knowledge base.
* **Conflict resolution strategies**: first declared rule, or most specific rule (largest number of premises). The same strategy orders candidate rules in backward chaining.
* **Refraction**: each rule fires at most once, so forward chaining always terminates, even on cyclic rule bases.
* **Cycle detection** in backward chaining (`A => B`, `B => A`) with pruning that keeps exploring alternative rules, plus memoization of goals that failed without touching a cycle.
* **Explainability**: a structured, depth-annotated trace of every step and a "how" explanation that rebuilds the derivation tree of any fact down to the initial evidence.
* **Immutable knowledge base**: engines work on their own working memory, so one knowledge base can be reused across runs and threads.
* **Validated parser** with line-numbered error messages, comments and both `=` and `=>` implication operators.
* **CLI** with five modes (`forward`, `backward`, `hybrid`, `saturate`, `compare`) and meaningful exit codes for scripting.
* **Zero runtime dependencies**, Java 11+, multi-stage non-root Docker image and a JDK 11/17/21 CI matrix.

---

## 🧠 How the Engines Work

### Knowledge representation

A rule is a definite Horn clause $p_1 \land p_2 \land \dots \land p_n \Rightarrow q$ over propositional symbols. The working memory $WM$ starts with the initial facts and only grows (monotonic reasoning).

### Forward chaining (recognize-act cycle)

1. **Match**: build the conflict set $CS = \{ r \mid premises(r) \subseteq WM,\ conclusion(r) \notin WM,\ r \text{ not fired} \}$.
2. **Resolve**: pick one rule from $CS$ with the configured strategy.
3. **Act**: add its conclusion to $WM$ and record the rule as the justification of that fact.
4. Repeat until the goal is in $WM$ or $CS = \emptyset$ (fixpoint).

With $R$ rules of at most $P$ premises there are at most $R$ cycles of $O(R \cdot P)$ matching each, so the run is $O(R^2 \cdot P)$.

### Backward chaining (depth-first SLD resolution)

To prove $g$: succeed if $g \in WM$; otherwise try each rule concluding $g$ and recursively prove all of its premises. The engine keeps the goals of the current proof path to cut cycles, caches proven goals in $WM$ and memoizes goals that failed without hitting a cycle. On acyclic rule bases every symbol is expanded at most once.

### Hybrid chaining

A backward pass walks from the goal through rule conclusions and collects the rules that can contribute to it. A forward pass then fires only those rules. Reachability is identical to plain forward chaining, but facts that cannot support the goal are never derived:

```text
facts: A        rules: R1 A => B, R2 A => C, R3 C => D, R4 B => G        goal: G

forward  derives [B, C, D, G]
hybrid   derives [B, G]        relevant rules [R1, R4], pruned [R2, R3]
```

---

## 🚀 Quick Start

### Docker

```bash
docker build -t symbolic-ai-inference-engine .
docker run --rm symbolic-ai-inference-engine
docker run --rm symbolic-ai-inference-engine --example incident-triage --goal rollback_release --mode backward
```

### Maven

```bash
mvn verify
java -jar target/inference-engine.jar --example animals --goal cheetah
java -jar target/inference-engine.jar --example classic --goal H --mode compare --strategy specific
```

### Example: production incident triage (backward chaining)

```text
$ java -jar target/inference-engine.jar --example incident-triage --goal page_on_call_engineer --mode backward --quiet
knowledge base: 4 facts, 12 rules
mode: backward, strategy: first declared rule

result: page_on_call_engineer proven, 5 rules fired

explanation:
  page_on_call_engineer was derived by R12: rollback_release AND incident_active => page_on_call_engineer
    rollback_release was derived by R5: application_regression => rollback_release
      application_regression was derived by R4: deploy_suspected AND db_cpu_normal => application_regression
        deploy_suspected was derived by R3: incident_active AND recent_deploy => deploy_suspected
          incident_active was derived by R1: http_5xx_spike AND error_rate_above_slo => incident_active
            http_5xx_spike is an initial fact
            error_rate_above_slo is an initial fact
          recent_deploy is an initial fact
        db_cpu_normal is an initial fact
    incident_active was derived by R1: http_5xx_spike AND error_rate_above_slo => incident_active
      http_5xx_spike is an initial fact
      error_rate_above_slo is an initial fact
```

### Example: comparing engines and strategies

```text
$ java -jar target/inference-engine.jar --example classic --goal H --mode compare --strategy specific
engine     proven   fired   derived facts
forward    true     3       [X, A, H]
backward   true     6       [X, D, E, F, A, H]
hybrid     true     3       [X, A, H]
```

With `--strategy first`, forward chaining needs 6 cycles to reach `H`; preferring the most specific rule cuts it to 3.

---

## 📚 Bundled Knowledge Bases

| Example | Domain | Facts | Rules | Try |
| :--- | :--- | :---: | :---: | :--- |
| `classic` | Textbook propositional exercise (`B, C` with 9 rules) | 2 | 9 | `--goal H` |
| `animals` | Animal identification after Winston's ZOOKEEPER (*Artificial Intelligence*, 1992) | 4 | 15 | `--goal cheetah` |
| `incident-triage` | SRE runbook: from monitoring signals to a remediation action | 4 | 12 | `--goal rollback_release` |

### File format

```text
# facts.txt: comma separated, one or more lines
has_hair, eats_meat, has_tawny_color, has_dark_spots

# rules.txt: one rule per line, rule ids are assigned in order (R1, R2, ...)
has_hair => mammal
mammal, eats_meat => carnivore
carnivore, has_tawny_color, has_dark_spots => cheetah
```

Symbols accept letters, digits, `_` and `-`. Malformed input is rejected with the offending line, for example `line 3: rule must have exactly one conclusion`.

---

## 🖥️ CLI Reference

```text
usage: inference-engine (--example NAME | --facts FILE --rules FILE) [options]

  --example NAME      bundled knowledge base: classic, animals, incident-triage
  --facts FILE        comma separated initial facts
  --rules FILE        one rule per line: p1, p2 => conclusion
  --goal SYMBOL       fact to prove
  --mode MODE         forward | backward | hybrid | saturate | compare
                      (default: hybrid with --goal, saturate without)
  --strategy NAME     first | specific (default: first)
  --quiet             print only the result and the explanation
  -h, --help          show this help

exit codes: 0 goal proven, 1 goal not proven, 2 invalid input
```

---

## 🏗️ Architecture

```text
src/main/java/io/github/kevinandrey96/inference
├── model
│   ├── Rule.java                        immutable Horn clause
│   ├── KnowledgeBase.java               immutable facts and rules
│   ├── KnowledgeBaseParser.java         validated text parser
│   └── KnowledgeBaseParseException.java line-numbered parse errors
├── engine
│   ├── InferenceEngine.java             common contract
│   ├── ForwardChainingEngine.java       recognize-act cycle with refraction
│   ├── BackwardChainingEngine.java      depth-first proof search with cycle pruning
│   ├── HybridChainingEngine.java        relevance analysis plus forward firing
│   ├── ConflictResolutionStrategy.java  first declared, most specific
│   ├── WorkingMemory.java               facts plus justifications for one run
│   ├── InferenceResult.java             immutable outcome and "how" explanations
│   └── TraceStep.java                   structured trace entry
└── cli
    ├── InferenceCli.java                entry point and output rendering
    └── CliOptions.java                  argument parsing and validation
```

Using the engine as a library:

```java
KnowledgeBase kb = KnowledgeBaseParser.load(Path.of("facts.txt"), Path.of("rules.txt"));
InferenceResult result = new BackwardChainingEngine(ConflictResolutionStrategy.MOST_SPECIFIC).prove(kb, "cheetah");

if (result.isGoalReached()) {
    result.explain("cheetah").forEach(System.out::println);
}
```

---

## 🧪 Testing

49 JUnit 5 tests cover the parser, every engine and the CLI:

* exact derivation order for each engine and strategy on the reference knowledge base;
* agreement of forward, backward and hybrid chaining on the reachability of every symbol;
* termination and correct pruning on cyclic rule bases, with a preemptive timeout guard;
* immutability of rules and knowledge bases across runs;
* explanation trees, trace depth and CLI exit codes for valid and invalid input.

```bash
mvn verify
```

---

## 🛠️ What Changed from the 2017 Version

The original project was a NetBeans Swing application with three windows (forward, backward and mixed chaining). Version 2.0 is a full rewrite that keeps the reference knowledge base and fixes these defects:

| 2017 behavior | 2.0 behavior |
| :--- | :--- |
| Forward chaining loop hard coded to 9 rules (`i < 9`) | Works with any number of rules |
| Backward chaining recursed forever on cyclic rules such as `A => B, B => A` | Cycles are detected, pruned and reported in the trace |
| The "goal already known" check only looked at the last initial fact | Constant-time membership check on the working memory |
| Parse errors printed to stdout and ignored | Line-numbered `KnowledgeBaseParseException` |
| Trace written to a hard coded Windows path | Structured, in-memory trace rendered by the CLI |
| Compiled `.class` files and IDE metadata versioned | Maven build, CI matrix and Docker image |

---

## 👥 Credits

* **Kevin Andrey Herrera Silva**: version 2.0 rewrite (architecture, engines, explanation system, CLI, tests, CI and Docker).
* The 2017 Swing version was committed by Viviana A. and its source files credit the author "chaima". That code remains in the first commit of the history.
