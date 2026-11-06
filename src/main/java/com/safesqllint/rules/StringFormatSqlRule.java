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

/** SQL004: String.format / MessageFormat used to build SQL with tainted args. */
public final class StringFormatSqlRule implements SqlRule {

    @Override
    public List<Finding> check(String filePath, MethodDeclaration method, TaintState taint) {
        List<Finding> findings = new ArrayList<>();
        for (MethodCallExpr formatCall : method.findAll(MethodCallExpr.class)) {
            if (!SqlHeuristics.isStringFormatCall(formatCall)) {
                continue;
            }
            if (formatCall.getArguments().isEmpty()) {
                continue;
            }
            Expression pattern = formatCall.getArguments().get(0);
            String patternText = SourceFileAnalyzer.literalPrefix(pattern);
            if (!SqlHeuristics.looksLikeSqlLiteral(patternText)) {
                continue;
            }
            boolean taintedArg = formatCall.getArguments().stream().skip(1).anyMatch(taint::isTainted);
            if (!taintedArg) {
                continue;
            }
            if (!reachesSqlSink(formatCall, method, taint)) {
                continue;
            }
            SourceFileAnalyzer.reportUnlessSuppressed(
                    findings,
                    method,
                    RuleId.STRING_FORMAT_SQL,
                    Severity.ERROR,
                    filePath,
                    formatCall,
                    "SQL constructed via format() with tainted interpolation");
        }
        return findings;
    }

    private boolean reachesSqlSink(MethodCallExpr formatCall, MethodDeclaration method, TaintState taint) {
        for (MethodCallExpr sink : method.findAll(MethodCallExpr.class)) {
            if (!SqlHeuristics.isSqlSinkCall(sink) || sink.getArguments().isEmpty()) {
                continue;
            }
            Expression arg = sink.getArguments().get(0);
            if (arg.equals(formatCall) || taint.isTainted(arg)) {
                return true;
            }
        }
        return false;
    }
}
