# safe-sql-lint

**safe-sql-lint** is a Java static analyzer that flags common SQL injection and unsafe JDBC/JPA query patterns. It is designed as a portfolio project demonstrating security-minded tooling: AST parsing, lightweight taint tracking, explicit rules with tests, and CI-friendly CLI output.

This tool is **not** a substitute for code review, penetration testing, or production SAST platforms. It implements a **small, well-tested rule set** and will miss vulnerabilities as well as report false positives.

## Overview

| Area | Support |
|------|---------|
| Input | `.java` source files or directories (recursive) |
| Analysis scope | Intra-procedural, per method |
| Output | JSON, ASCII table, or both |
| Exit codes | `0` = no findings, `1` = findings, `2` = usage/parse error |
| License | MIT (this project); dependencies use MIT/Apache/BSD-compatible licenses |

## How it works

1. **Parse** — Source is parsed with [JavaParser](https://github.com/javaparser/javaparser) (Apache-2.0).
2. **Taint seeds** — User-influenced values are marked tainted, including:
   - `HttpServletRequest` getters (`getParameter`, `getHeader`, …)
   - Spring/JAX-RS style parameters (`@RequestParam`, `@PathVariable`, `@RequestBody`, …)
   - `Scanner` reads, `System.getenv` / `System.getProperty`
3. **Taint propagation** — Flow-insensitive propagation across assignments, string concatenation, and `String.format` / `MessageFormat.format`.
4. **Rules** — Each rule inspects method bodies for SQL **sinks** (e.g. `Statement.execute*`, `Connection.prepareStatement`, `EntityManager.createNativeQuery`, `JdbcTemplate.query/update`) and reports when tainted or dynamically built SQL reaches them.
5. **Suppressions** — Findings can be suppressed with annotations or comments (see below).

```
  [ HTTP / annotations / env ]     assignment / concat / format
            │                                │
            ▼                                ▼
       TaintState ──────────────────► SQL sink call?
            │                                │
            └──────────► RuleEngine ──► Finding
```

## Quick start

### Requirements

- JDK 17+
- Maven 3.9+

### Build & test

```bash
mvn test
```

### Run the CLI

```bash
mvn -q package
java -jar target/safe-sql-lint-1.0.0-SNAPSHOT.jar src/main/java
```

```bash
# JSON for CI ingestion
java -jar target/safe-sql-lint-1.0.0-SNAPSHOT.jar --format json path/to/sources

# Table (default)
java -jar target/safe-sql-lint-1.0.0-SNAPSHOT.jar path/to/MyDao.java
```

### Docker

```bash
docker build -t safe-sql-lint .
docker run --rm -v "$PWD:/work" safe-sql-lint /work/src/test/resources/fixtures/bad
```

Mount your project at `/work` and pass paths relative to that directory.

## Rule reference

| ID | Enum | Severity | Description |
|----|------|----------|-------------|
| SQL001 | `SQL_CONCAT_INJECTION` | ERROR | Tainted or concatenated SQL passed to a JDBC/JPA/JdbcTemplate sink |
| SQL002 | `STATEMENT_NOT_PREPARED` | ERROR | Dynamic SQL executed via `Statement.execute*` instead of bound `PreparedStatement` |
| SQL003 | `DYNAMIC_ORDER_LIMIT` | WARNING | `ORDER BY` or `LIMIT` fragment incorporates unsanitized input |
| SQL004 | `STRING_FORMAT_SQL` | ERROR | SQL built with `String.format` / `MessageFormat` using tainted values |
| SQL005 | `DISABLED_PARAMETER_BINDING` | WARNING | `prepareStatement` used but tainted data is still embedded in the SQL string while bind calls exist |

### Suppressions

- **Annotation** (method or class): `@SuppressWarnings("safe-sql-lint:SQL001")` or `"safe-sql-lint:SQL_CONCAT_INJECTION"`
- **Line comment** (within ±1 line of the expression): `// safe-sql-lint: suppress SQL002`

Use suppressions sparingly and document why the pattern is safe.

## Testing

- **Unit tests** — CLI, reporters, taint helper behavior (`src/test/java`).
- **Fixture corpus** — `src/test/resources/fixtures/bad` (expected to produce findings) and `fixtures/good` (expected clean). See `FixtureCorpusTest`.

```bash
mvn test
```

## Design decisions

- **JavaParser over bytecode** — Readable locations and snippets for developers; accepts source-only CI jobs without full classpath resolution.
- **Intra-procedural taint** — Keeps implementation understandable and fast; avoids false confidence from whole-program analysis we do not implement.
- **Explicit servlet parameter heuristics** — `getParameter`-style calls are treated as sources even when the receiver name is not `request`, which may false-positive on unrelated APIs with the same method name.
- **Shaded JAR** — Single executable for CLI and Docker.
- **Picocli** — Standard CLI UX (`--help`, `--version`, exit codes).

## Limitations

- No inter-procedural or cross-file taint tracking
- No full SQL grammar; string heuristics only (`SELECT`, `WHERE`, `ORDER BY`, etc.)
- No automatic verification of allow-lists (e.g. validated `ORDER BY` columns)
- Does not analyze Kotlin, XML SQL mappers, or JPA JPQL in annotations unless present as Java string literals in methods
- **No claim of complete coverage or zero false positives**

## AI assistance

This project was built with help from an **AI coding agent in Cursor** and reviewed/edited by the author. Generated code was validated with unit tests, fixture corpora, and CI before merge.

## License

MIT — see [LICENSE](LICENSE).
