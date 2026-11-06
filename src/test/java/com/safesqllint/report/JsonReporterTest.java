package com.safesqllint.report;

import com.safesqllint.model.Finding;
import com.safesqllint.model.RuleId;
import com.safesqllint.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReporterTest {

    @Test
    void rendersFindingCount() throws Exception {
        Finding f = new Finding(
                RuleId.SQL_CONCAT_INJECTION,
                Severity.ERROR,
                "Foo.java",
                10,
                4,
                "msg",
                "snippet");
        String json = JsonReporter.render(List.of(f));
        assertTrue(json.contains("\"findingCount\" : 1"));
        assertTrue(json.contains("\"SQL001\""));
    }
}
