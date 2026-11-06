package com.safesqllint.model;

import java.util.Objects;

public final class Finding implements Comparable<Finding> {
    private final RuleId ruleId;
    private final Severity severity;
    private final String filePath;
    private final int line;
    private final int column;
    private final String message;
    private final String snippet;

    public Finding(
            RuleId ruleId,
            Severity severity,
            String filePath,
            int line,
            int column,
            String message,
            String snippet) {
        this.ruleId = Objects.requireNonNull(ruleId);
        this.severity = Objects.requireNonNull(severity);
        this.filePath = Objects.requireNonNull(filePath);
        this.line = line;
        this.column = column;
        this.message = Objects.requireNonNull(message);
        this.snippet = snippet == null ? "" : snippet;
    }

    public RuleId ruleId() {
        return ruleId;
    }

    public Severity severity() {
        return severity;
    }

    public String filePath() {
        return filePath;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public String message() {
        return message;
    }

    public String snippet() {
        return snippet;
    }

    @Override
    public int compareTo(Finding other) {
        int byFile = filePath.compareTo(other.filePath);
        if (byFile != 0) {
            return byFile;
        }
        if (line != other.line) {
            return Integer.compare(line, other.line);
        }
        if (column != other.column) {
            return Integer.compare(column, other.column);
        }
        return ruleId.compareTo(other.ruleId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Finding finding)) {
            return false;
        }
        return line == finding.line
                && column == finding.column
                && ruleId == finding.ruleId
                && severity == finding.severity
                && filePath.equals(finding.filePath)
                && message.equals(finding.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, severity, filePath, line, column, message);
    }
}
