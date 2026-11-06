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

/** SQL001: concatenation or tainted SQL expression passed to a query sink. */
public final class StringConcatSqlRule implements SqlRule {

    @Override
    public List<Finding> check(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        method.findAll(MethodCallExpr.class).stream()
                .filter(SqlHeuristics::isSqlSinkCall)
                .forEach(sink -> {
                    if (sink.getArguments().isEmpty()) {
                        return;
                    }
                    Expression sqlArg = sink.getArguments().get(0);
                    boolean dynamic = isDynamicSql(sqlArg, taint);
                    if (!dynamic) {
                        return;
                    }
                    SourceFileAnalyzer.reportUnlessSuppressed(
                            findings,
                            method,
                            RuleId.SQL_CONCAT_INJECTION,
                            Severity.ERROR,
                            filePath,
                            sqlArg,
                            "Tainted or concatenated SQL passed to "
                                    + sink.getNameAsString()
                                    + "()");
                });
        return findings;
    }

    static boolean isDynamicSql(Expression expr, TaintState taint) {
        if (taint.isTainted(expr)) {
            return true;
        }
        if (expr instanceof com.github.javaparser.ast.expr.BinaryExpr binary
                && binary.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS) {
            String prefix = SourceFileAnalyzer.literalPrefix(expr);
            if (SqlHeuristics.looksLikeSqlLiteral(prefix) || prefix.isEmpty()) {
                return isDynamicSql(binary.getLeft(), taint) || isDynamicSql(binary.getRight(), taint);
            }
        }
        return false;
    }
}
