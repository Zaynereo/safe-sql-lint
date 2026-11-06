package fixtures.good;

import java.util.List;

public class ConstantOrderBy {
    private static final List<String> ALLOWED = List.of("name", "created_at");

    void safeSort(org.springframework.jdbc.core.JdbcTemplate template, String sort) {
        if (!ALLOWED.contains(sort)) {
            throw new IllegalArgumentException("invalid sort");
        }
        template.query("SELECT * FROM users ORDER BY " + sort, rs -> null);
    }
}
