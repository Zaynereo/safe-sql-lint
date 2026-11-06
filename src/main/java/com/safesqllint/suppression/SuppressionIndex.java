package com.safesqllint.suppression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.safesqllint.model.RuleId;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves suppressions from {@code @SuppressWarnings("safe-sql-lint:SQL001")} and line comments
 * {@code // safe-sql-lint: suppress SQL001}.
 */
public final class SuppressionIndex {

    private static final Pattern COMMENT_SUPPRESS =
            Pattern.compile("safe-sql-lint:\\s*suppress\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private SuppressionIndex() {}

    public static boolean isSuppressed(Node node, RuleId ruleId) {
        if (node == null) {
            return false;
        }
        if (hasAdjacentSuppressComment(node, ruleId)) {
            return true;
        }
        Optional<Comment> comment = node.getComment();
        if (comment.isPresent() && commentMatches(comment.get().getContent(), ruleId)) {
            return true;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof NodeWithAnnotations<?> withAnnotations) {
                if (annotationsSuppress(withAnnotations.getAnnotations(), ruleId)) {
                    return true;
                }
            }
            if (current instanceof CallableDeclaration<?> callable) {
                if (annotationsSuppress(callable.getAnnotations(), ruleId)) {
                    return true;
                }
            }
            if (current instanceof TypeDeclaration<?> type) {
                if (annotationsSuppress(type.getAnnotations(), ruleId)) {
                    return true;
                }
            }
            current = current.getParentNode().orElse(null);
        }
        return false;
    }

    private static boolean commentMatches(String content, RuleId ruleId) {
        Matcher m = COMMENT_SUPPRESS.matcher(content);
        while (m.find()) {
            RuleId suppressed = RuleId.fromCode(m.group(1));
            if (suppressed == null) {
                suppressed = ruleIdFromToken(m.group(1));
            }
            if (suppressed == ruleId) {
                return true;
            }
        }
        return false;
    }

    private static RuleId ruleIdFromToken(String token) {
        for (RuleId id : RuleId.values()) {
            if (id.name().equalsIgnoreCase(token)) {
                return id;
            }
        }
        return null;
    }

    private static boolean hasAdjacentSuppressComment(Node node, RuleId ruleId) {
        if (!node.getRange().isPresent()) {
            return false;
        }
        int targetLine = node.getRange().get().begin.line;
        Node root = node;
        while (root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        for (Comment comment : root.findAll(Comment.class)) {
            if (!comment.getRange().isPresent()) {
                continue;
            }
            int commentLine = comment.getRange().get().begin.line;
            if (commentLine >= targetLine - 1 && commentLine <= targetLine + 1) {
                if (commentMatches(comment.getContent(), ruleId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean annotationsSuppress(
            Iterable<AnnotationExpr> annotations, RuleId ruleId) {
        for (AnnotationExpr annotation : annotations) {
            if (!"SuppressWarnings".equals(annotation.getNameAsString())) {
                continue;
            }
            Set<String> values = AnnotationValues.readSuppressWarningValues(annotation);
            for (String value : values) {
                if (value.equalsIgnoreCase("safe-sql-lint")
                        || value.equalsIgnoreCase("safe-sql-lint:" + ruleId.code())
                        || value.equalsIgnoreCase("safe-sql-lint:" + ruleId.name())) {
                    return true;
                }
                RuleId parsed = RuleId.fromCode(value.replace("safe-sql-lint:", ""));
                if (parsed == ruleId) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Collect all rule IDs suppressed on a compilation unit (for tests). */
    public static EnumSet<RuleId> suppressedRulesOn(CallableDeclaration<?> method) {
        EnumSet<RuleId> set = EnumSet.noneOf(RuleId.class);
        for (RuleId id : RuleId.values()) {
            if (isSuppressed(method, id)) {
                set.add(id);
            }
        }
        return set;
    }
}
