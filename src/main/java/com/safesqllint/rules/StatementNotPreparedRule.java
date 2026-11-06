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

/** SQL002: Statement.execute* with dynamic SQL instead of prepareStatement + binds. */
public final class StatementNotPreparedRule implements SqlRule {

    @Override
    public List<Finding> check(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (!SqlHeuristics.isJdbcExecuteCall(call)) {
                continue;
            }
            if (!isStatementReceiver(call)) {
                continue;
            }
            if (call.getArguments().isEmpty()) {
                continue;
            }
            Expression sql = call.getArguments().get(0);
            if (!StringConcatSqlRule.isDynamicSql(sql, taint) && !taint.isTainted(sql)) {
                continue;
            }
            SourceFileAnalyzer.reportUnlessSuppressed(
                    findings,
                    method,
                    RuleId.STATEMENT_NOT_PREPARED,
                    Severity.ERROR,
                    filePath,
                    call,
                    "Dynamic SQL executed through Statement."
                            + "execute*; use PreparedStatement with parameter binding");
        }
        return findings;
    }

    private static boolean isStatementReceiver(MethodCallExpr executeCall) {
        return executeCall.getScope()
                .map(scope -> !scope.toString().toLowerCase().contains("prepared"))
                .orElse(true);
    }
}
