package com.safesqllint.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.safesqllint.analysis.TaintState;
import com.safesqllint.model.Finding;

import java.util.List;

public interface SqlRule {
    List<Finding> check(String filePath, MethodDeclaration method, TaintState taint);
}
