package com.safesqllint.analysis;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Intra-procedural, flow-insensitive taint tracking for a single method body.
 */
public final class TaintState {

    private final Set<String> taintedNames = new HashSet<>();

    public void markTainted(String name) {
        if (name != null && !name.isBlank()) {
            taintedNames.add(name);
        }
    }

    public void seedFromSources(Node methodBody, Iterable<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (SqlHeuristics.isUserControlledParameter(parameter)) {
                markTainted(parameter.getNameAsString());
            }
        }
        for (MethodCallExpr call : methodBody.findAll(MethodCallExpr.class)) {
            if (SqlHeuristics.isTaintSourceCall(call)) {
                assignmentTarget(call).ifPresent(this::markTainted);
            }
        }
        for (VariableDeclarationExpr decl : methodBody.findAll(VariableDeclarationExpr.class)) {
            decl.getVariables().forEach(v -> v.getInitializer().ifPresent(init -> {
                if (expressionIsTainted(init, new HashSet<>())) {
                    markTainted(v.getNameAsString());
                }
            }));
        }
        propagateFixedPoint(methodBody);
    }

    private void propagateFixedPoint(Node methodBody) {
        boolean changed = true;
        int iterations = 0;
        while (changed && iterations < 32) {
            changed = false;
            iterations++;
            for (VariableDeclarationExpr decl : methodBody.findAll(VariableDeclarationExpr.class)) {
                for (var v : decl.getVariables()) {
                    Optional<Expression> init = v.getInitializer();
                    if (init.isPresent() && expressionIsTainted(init.get(), new HashSet<>()) && taintedNames.add(v.getNameAsString())) {
                        changed = true;
                    }
                }
            }
            for (BinaryExpr binary : methodBody.findAll(BinaryExpr.class)) {
                if (binary.getOperator() == BinaryExpr.Operator.PLUS
                        && (expressionIsTainted(binary.getLeft(), new HashSet<>())
                                || expressionIsTainted(binary.getRight(), new HashSet<>()))) {
                    if (assignmentTarget(binary).filter(taintedNames::add).isPresent()) {
                        changed = true;
                    }
                }
            }
            for (MethodCallExpr call : methodBody.findAll(MethodCallExpr.class)) {
                if (SqlHeuristics.isTaintPropagatingCall(call)) {
                    if (call.getArguments().stream().anyMatch(arg -> expressionIsTainted(arg, new HashSet<>()))) {
                        if (assignmentTarget(call).filter(taintedNames::add).isPresent()) {
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    public boolean isTainted(Expression expression) {
        if (expression == null) {
            return false;
        }
        return expressionIsTainted(expression, new HashSet<>());
    }

    private boolean expressionIsTainted(Expression expression, Set<Expression> visiting) {
        if (!visiting.add(expression)) {
            return false;
        }
        try {
            if (expression instanceof NameExpr name) {
                return taintedNames.contains(name.getNameAsString());
            }
            if (expression instanceof StringLiteralExpr) {
                return false;
            }
            if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
                return expressionIsTainted(binary.getLeft(), visiting)
                        || expressionIsTainted(binary.getRight(), visiting);
            }
            if (expression instanceof MethodCallExpr call) {
                if (SqlHeuristics.isTaintSourceCall(call)) {
                    return true;
                }
                if (SqlHeuristics.isTaintPropagatingCall(call)) {
                    return call.getArguments().stream().anyMatch(arg -> expressionIsTainted(arg, visiting))
                            || call.getScope().map(scope -> expressionIsTainted(scope, visiting)).orElse(false);
                }
                if ("append".equals(call.getNameAsString())
                        && call.getArguments().stream().anyMatch(arg -> expressionIsTainted(arg, visiting))) {
                    return true;
                }
                if ("toString".equals(call.getNameAsString())
                        && call.getScope().map(scope -> expressionIsTainted(scope, visiting)).orElse(false)) {
                    return true;
                }
            }
            return false;
        } finally {
            visiting.remove(expression);
        }
    }

    private static Optional<String> assignmentTarget(Expression expr) {
        Node parent = expr.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof VariableDeclarationExpr decl) {
                return decl.getVariables().stream().findFirst().map(v -> v.getNameAsString());
            }
            parent = parent.getParentNode().orElse(null);
        }
        return Optional.empty();
    }
}
