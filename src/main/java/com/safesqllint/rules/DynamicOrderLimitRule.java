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

/** SQL003: ORDER BY / LIMIT built from tainted fragments. */
public final class DynamicOrderLimitRule implements SqlRule {

    @Override
    public List<Finding> check(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        for (MethodCallExpr sink : method.findAll(MethodCallExpr.class)) {
            if (!SqlHeuristics.isSqlSinkCall(sink) || sink.getArguments().isEmpty()) {
                continue;
            }
            Expression sql = sink.getArguments().get(0);
            if (!containsDynamicOrderOrLimit(sql, taint)) {
                continue;
            }
            SourceFileAnalyzer.reportUnlessSuppressed(
                    findings,
                    method,
                    RuleId.DYNAMIC_ORDER_LIMIT,
                    Severity.WARNING,
                    filePath,
                    sql,
                    "ORDER BY or LIMIT appears to incorporate unsanitized input");
        }
        return findings;
    }

    private boolean containsDynamicOrderOrLimit(Expression expr, TaintState taint) {
        String prefix = SourceFileAnalyzer.literalPrefix(expr);
        if (SqlHeuristics.isDynamicSqlFragment(prefix) && taint.isTainted(expr)) {
            return true;
        }
        if (expr instanceof com.github.javaparser.ast.expr.BinaryExpr binary
                && binary.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS) {
            String leftPrefix = SourceFileAnalyzer.literalPrefix(binary.getLeft());
            if (SqlHeuristics.isDynamicSqlFragment(leftPrefix) && taint.isTainted(binary.getRight())) {
                return true;
            }
            return containsDynamicOrderOrLimit(binary.getLeft(), taint)
                    || containsDynamicOrderOrLimit(binary.getRight(), taint);
        }
        if (taint.isTainted(expr)) {
            String p = SourceFileAnalyzer.literalPrefix(expr);
            return SqlHeuristics.isDynamicSqlFragment(p);
        }
        return false;
    }
}
