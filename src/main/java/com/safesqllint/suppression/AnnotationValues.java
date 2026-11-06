package com.safesqllint.suppression;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.util.HashSet;
import java.util.Set;

final class AnnotationValues {

    private AnnotationValues() {}

    static Set<String> readSuppressWarningValues(AnnotationExpr annotation) {
        Set<String> out = new HashSet<>();
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            addExpressionValues(single.getMemberValue(), out);
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if ("value".equals(pair.getNameAsString())) {
                    addExpressionValues(pair.getValue(), out);
                }
            }
        }
        return out;
    }

    private static void addExpressionValues(Expression expr, Set<String> out) {
        if (expr instanceof StringLiteralExpr literal) {
            out.add(literal.asString());
        } else if (expr instanceof ArrayInitializerExpr array) {
            for (Expression element : array.getValues()) {
                addExpressionValues(element, out);
            }
        }
    }
}
