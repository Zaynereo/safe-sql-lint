package com.safesqllint;

import com.safesqllint.analysis.ProjectAnalyzer;
import com.safesqllint.model.Finding;
import com.safesqllint.model.RuleId;
import com.safesqllint.rules.RuleEngine;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureCorpusTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");
    private static final Path BAD = FIXTURES.resolve("bad");
    private static final Path GOOD = FIXTURES.resolve("good");

    @Test
    void badFixturesEachTriggerExpectedRules() throws Exception {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(new RuleEngine());
        List<Finding> badFindings = analyzer.analyzePaths(List.of(BAD));

        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("ConcatInjection.java")
                && f.ruleId() == RuleId.SQL_CONCAT_INJECTION));
        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("RequestParamInjection.java")
                && (f.ruleId() == RuleId.SQL_CONCAT_INJECTION || f.ruleId() == RuleId.STATEMENT_NOT_PREPARED)));
        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("DynamicOrderBy.java")
                && f.ruleId() == RuleId.DYNAMIC_ORDER_LIMIT));
        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("StringFormatSql.java")
                && f.ruleId() == RuleId.STRING_FORMAT_SQL));
        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("DisabledBinding.java")
                && f.ruleId() == RuleId.DISABLED_PARAMETER_BINDING));
        assertTrue(badFindings.stream().anyMatch(f -> f.filePath().endsWith("JpaNativeQuery.java")
                && f.ruleId() == RuleId.SQL_CONCAT_INJECTION));
    }

    @Test
    void goodFixturesStayClean() throws Exception {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(new RuleEngine());
        List<Finding> goodFindings = analyzer.analyzePaths(List.of(GOOD));
        assertTrue(
                goodFindings.isEmpty(),
                () -> "Unexpected findings: "
                        + goodFindings.stream().map(Finding::toString).collect(Collectors.joining("; ")));
    }

    @Test
    void suppressedFixtureProducesNoFindings() throws Exception {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(new RuleEngine());
        List<Finding> findings =
                analyzer.analyzePaths(List.of(GOOD.resolve("SuppressedConcat.java")));
        assertTrue(findings.isEmpty(), () -> "Suppressed file should be clean but got " + findings);
    }

    @Test
    void badCorpusIsNonEmpty() throws Exception {
        ProjectAnalyzer analyzer = new ProjectAnalyzer(new RuleEngine());
        List<Finding> badFindings = analyzer.analyzePaths(List.of(BAD));
        assertFalse(badFindings.isEmpty());
        EnumSet<RuleId> rules = EnumSet.noneOf(RuleId.class);
        badFindings.forEach(f -> rules.add(f.ruleId()));
        assertTrue(rules.size() >= 4, () -> "Expected multiple rule types, got " + rules);
    }
}
