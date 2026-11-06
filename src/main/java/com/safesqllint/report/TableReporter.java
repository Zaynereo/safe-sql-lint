package com.safesqllint.report;

import com.safesqllint.model.Finding;

import java.util.Comparator;
import java.util.List;

public final class TableReporter {

    private TableReporter() {}

    public static String render(List<Finding> findings) {
        if (findings.isEmpty()) {
            return "No SQL safety findings.";
        }
        int ruleW = 6;
        int sevW = 7;
        int locComputed = findings.stream()
                .mapToInt(f -> (f.filePath() + ":" + f.line()).length())
                .max()
                .orElse(20);
        final int locW = Math.min(Math.max(locComputed, 24), 80);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-" + ruleW + "s %-" + sevW + "s %-" + locW + "s %s%n", "RULE", "SEVERITY", "LOCATION", "MESSAGE"));
        sb.append("-".repeat(ruleW + sevW + locW + 40)).append('\n');
        findings.stream()
                .sorted(Comparator.comparing(Finding::filePath).thenComparingInt(Finding::line))
                .forEach(f -> {
                    String loc = truncate(f.filePath() + ":" + f.line(), locW);
                    sb.append(String.format(
                            "%-" + ruleW + "s %-" + sevW + "s %-" + locW + "s %s%n",
                            f.ruleId().code(),
                            f.severity().name(),
                            loc,
                            f.message()));
                });
        sb.append('\n').append(findings.size()).append(" finding(s).");
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return "..." + s.substring(s.length() - max + 3);
    }
}
