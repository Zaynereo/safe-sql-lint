package com.safesqllint.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.safesqllint.analysis.TaintState;
import com.safesqllint.model.Finding;

import java.util.ArrayList;
import java.util.List;

public final class RuleEngine {

    private final List<SqlRule> rules;

    public RuleEngine() {
        this.rules = List.of(
                new StringConcatSqlRule(),
                new StatementNotPreparedRule(),
                new DynamicOrderLimitRule(),
                new StringFormatSqlRule(),
                new DisabledParameterBindingRule());
    }

    public List<Finding> checkMethod(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        for (SqlRule rule : rules) {
            findings.addAll(rule.check(filePath, method, taint));
        }
        return findings;
    }
}
