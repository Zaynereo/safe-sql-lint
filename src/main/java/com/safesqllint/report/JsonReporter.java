package com.safesqllint.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.safesqllint.model.Finding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonReporter {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonReporter() {}

    public static String render(List<Finding> findings) throws Exception {
        List<Map<String, Object>> items = findings.stream().map(JsonReporter::toMap).toList();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("tool", "safe-sql-lint");
        root.put("version", "1.0.0-SNAPSHOT");
        root.put("findingCount", findings.size());
        root.put("findings", items);
        return MAPPER.writeValueAsString(root);
    }

    private static Map<String, Object> toMap(Finding f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rule", f.ruleId().code());
        m.put("ruleName", f.ruleId().name());
        m.put("severity", f.severity().name());
        m.put("file", f.filePath());
        m.put("line", f.line());
        m.put("column", f.column());
        m.put("message", f.message());
        m.put("snippet", f.snippet());
        return m;
    }
}
