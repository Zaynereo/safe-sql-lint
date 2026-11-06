package com.safesqllint.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.safesqllint.analysis.SourceFileAnalyzer;
import com.safesqllint.analysis.SqlHeuristics;
import com.safesqllint.analysis.TaintState;
import com.safesqllint.model.Finding;
import com.safesqllint.model.RuleId;
import com.safesqllint.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SQL005: prepareStatement is used but tainted values are embedded in the SQL string rather than bound via set*.
 */
public final class DisabledParameterBindingRule implements SqlRule {

    private static final Set<String> BINDERS = Set.of(
            "setString",
            "setInt",
            "setLong",
            "setObject",
            "setBoolean",
            "setDate",
            "setTimestamp");

    @Override
    public List<Finding> check(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        for (MethodCallExpr prep : method.findAll(MethodCallExpr.class)) {
            if (!SqlHeuristics.isPrepareStatementCall(prep) || prep.getArguments().isEmpty()) {
                continue;
            }
            Expression sql = prep.getArguments().get(0);
            if (!StringConcatSqlRule.isDynamicSql(sql, taint)) {
                continue;
            }
            if (!hasAnyBinder(method)) {
                continue;
            }
            SourceFileAnalyzer.reportUnlessSuppressed(
                    findings,
                    method,
                    RuleId.DISABLED_PARAMETER_BINDING,
                    Severity.WARNING,
                    filePath,
                    sql,
                    "PreparedStatement used but tainted data is concatenated into SQL instead of set* parameters");
        }
        return findings;
    }

    private boolean hasAnyBinder(MethodDeclaration method) {
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (BINDERS.contains(call.getNameAsString())) {
                return true;
            }
        }
        return false;
    }
}
