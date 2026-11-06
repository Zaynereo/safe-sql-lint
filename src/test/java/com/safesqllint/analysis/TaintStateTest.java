package com.safesqllint.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaintStateTest {

    @Test
    void marksRequestParameterAsTainted() {
        CompilationUnit cu = StaticJavaParser.parse(
                """
                import javax.servlet.http.HttpServletRequest;
                class T {
                  void m(HttpServletRequest req) {
                    String x = req.getParameter("a");
                    boolean t = false;
                  }
                }
                """);
        MethodDeclaration method = cu.findFirst(MethodDeclaration.class).orElseThrow();
        TaintState state = new TaintState();
        state.seedFromSources(method.getBody().orElseThrow(), method.getParameters());
        assertTrue(state.isTainted(new com.github.javaparser.ast.expr.NameExpr("x")));
        assertFalse(state.isTainted(new com.github.javaparser.ast.expr.NameExpr("t")));
    }
}
