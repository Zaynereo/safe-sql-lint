package com.safesqllint.model;

public enum RuleId {
    SQL_CONCAT_INJECTION("SQL001", "User-influenced data concatenated into SQL passed to a JDBC/JPA sink"),
    STATEMENT_NOT_PREPARED("SQL002", "Dynamic SQL executed via Statement instead of PreparedStatement with binds"),
    DYNAMIC_ORDER_LIMIT("SQL003", "ORDER BY or LIMIT clause built from unsanitized input"),
    STRING_FORMAT_SQL("SQL004", "SQL built with String.format or MessageFormat using tainted values"),
    DISABLED_PARAMETER_BINDING("SQL005", "Values embedded in SQL string instead of PreparedStatement set* binding");

    private final String code;
    private final String description;

    RuleId(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    public static RuleId fromCode(String code) {
        for (RuleId id : values()) {
            if (id.code.equalsIgnoreCase(code)) {
                return id;
            }
        }
        return null;
    }
}
