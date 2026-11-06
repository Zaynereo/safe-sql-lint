package com.safesqllint.analysis;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.safesqllint.model.Finding;
import com.safesqllint.model.RuleId;
import com.safesqllint.model.Severity;
import com.safesqllint.rules.RuleEngine;
import com.safesqllint.suppression.SuppressionIndex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SourceFileAnalyzer {

    private final RuleEngine ruleEngine;

    public SourceFileAnalyzer(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public List<Finding> analyze(Path filePath, CompilationUnit unit) {
        List<Finding> findings = new ArrayList<>();
        String path = filePath.toString().replace('\\', '/');
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            if (!method.getBody().isPresent()) {
                continue;
            }
            TaintState taint = new TaintState();
            taint.seedFromSources(method.getBody().get(), method.getParameters());
            findings.addAll(ruleEngine.checkMethod(path, method, taint));
        }
        return findings;
    }

    public static Finding buildFinding(
            RuleId ruleId,
            Severity severity,
            String filePath,
            Expression at,
            String message) {
        int line = 1;
        int column = 1;
        if (at.getRange().isPresent()) {
            Position begin = at.getRange().get().begin;
            line = begin.line;
            column = begin.column;
        }
        String snippet = at.toString();
        if (snippet.length() > 120) {
            snippet = snippet.substring(0, 117) + "...";
        }
        return new Finding(ruleId, severity, filePath, line, column, message, snippet);
    }

    public static boolean reportUnlessSuppressed(
            List<Finding> out,
            CallableDeclaration<?> scope,
            RuleId ruleId,
            Severity severity,
            String filePath,
            Expression at,
            String message) {
        if (SuppressionIndex.isSuppressed(at, ruleId)) {
            return false;
        }
        out.add(buildFinding(ruleId, severity, filePath, at, message));
        return true;
    }

    public static String literalPrefix(Expression expr) {
        if (expr instanceof StringLiteralExpr lit) {
            return lit.asString();
        }
        if (expr instanceof com.github.javaparser.ast.expr.BinaryExpr binary
                && binary.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS) {
            return literalPrefix(binary.getLeft()) + literalPrefix(binary.getRight());
        }
        return "";
    }
}
