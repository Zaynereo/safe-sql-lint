package com.safesqllint.analysis;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Locale;
import java.util.Set;

public final class SqlHeuristics {

    private static final Set<String> REQUEST_GETTERS = Set.of(
            "getParameter",
            "getParameterValues",
            "getParameterMap",
            "getHeader",
            "getHeaders",
            "getQueryString",
            "getInputStream",
            "getReader");

    private static final Set<String> JDBC_EXECUTE = Set.of(
            "execute",
            "executeQuery",
            "executeUpdate",
            "executeBatch",
            "executeLargeUpdate");

    private static final Set<String> JPA_NATIVE = Set.of("createNativeQuery", "createQuery");

    private SqlHeuristics() {}

    public static boolean isUserControlledParameter(Parameter parameter) {
        return parameter.getAnnotations().stream()
                .anyMatch(a -> {
                    String n = a.getNameAsString();
                    return "RequestParam".equals(n)
                            || "PathVariable".equals(n)
                            || "RequestBody".equals(n)
                            || "QueryParam".equals(n)
                            || "FormParam".equals(n);
                });
    }

    public static boolean isTaintSourceCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (REQUEST_GETTERS.contains(name)) {
            return true;
        }
        if ("next".equals(name) || "nextLine".equals(name)) {
            return scopeEndsWith(call, "Scanner");
        }
        if ("getenv".equals(name)) {
            return call.getScope().map(s -> s.toString()).orElse("").contains("System");
        }
        if ("getProperty".equals(name)) {
            return call.getScope().map(s -> s.toString()).orElse("").contains("System");
        }
        return false;
    }

    public static boolean isTaintPropagatingCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if ("format".equals(name)) {
            String scope = call.getScope().map(Object::toString).orElse("");
            return scope.contains("String") || scope.contains("MessageFormat");
        }
        return false;
    }

    public static boolean looksLikeSqlLiteral(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        return upper.contains("SELECT ")
                || upper.contains("INSERT ")
                || upper.contains("UPDATE ")
                || upper.contains("DELETE ")
                || upper.contains(" FROM ")
                || upper.startsWith("SELECT")
                || upper.contains(" WHERE ");
    }

    public static boolean isDynamicSqlFragment(String text) {
        if (text == null) {
            return false;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        return upper.contains("ORDER BY") || upper.contains(" LIMIT ") || upper.endsWith(" LIMIT");
    }

    public static boolean isJdbcExecuteCall(MethodCallExpr call) {
        return JDBC_EXECUTE.contains(call.getNameAsString());
    }

    public static boolean isPrepareStatementCall(MethodCallExpr call) {
        return "prepareStatement".equals(call.getNameAsString());
    }

    public static boolean isCreateStatementCall(MethodCallExpr call) {
        return "createStatement".equals(call.getNameAsString());
    }

    public static boolean isJpaQueryCall(MethodCallExpr call) {
        return JPA_NATIVE.contains(call.getNameAsString());
    }

    public static boolean isJdbcTemplateSqlCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (!Set.of("query", "update", "execute", "queryForObject", "queryForList").contains(name)) {
            return false;
        }
        return call.getScope().map(s -> s.toString()).orElse("").toLowerCase(Locale.ROOT).contains("template");
    }

    public static boolean isSqlSinkCall(MethodCallExpr call) {
        return isJdbcExecuteCall(call)
                || isPrepareStatementCall(call)
                || isJpaQueryCall(call)
                || isJdbcTemplateSqlCall(call);
    }

    public static boolean isStringFormatCall(MethodCallExpr call) {
        if (!"format".equals(call.getNameAsString())) {
            return false;
        }
        String scope = call.getScope().map(Object::toString).orElse("");
        return scope.contains("String") || scope.contains("MessageFormat");
    }

    private static boolean scopeEndsWith(MethodCallExpr call, String typeHint, String... names) {
        String scope = call.getScope().map(Object::toString).orElse("").toLowerCase(Locale.ROOT);
        for (String n : names) {
            if (scope.endsWith(n.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return scope.contains(typeHint.toLowerCase(Locale.ROOT));
    }
}
