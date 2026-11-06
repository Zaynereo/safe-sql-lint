package com.safesqllint.cli;

import com.safesqllint.analysis.AnalysisException;
import com.safesqllint.analysis.ProjectAnalyzer;
import com.safesqllint.model.Finding;
import com.safesqllint.report.JsonReporter;
import com.safesqllint.report.TableReporter;
import com.safesqllint.rules.RuleEngine;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "safe-sql-lint",
        mixinStandardHelpOptions = true,
        version = "safe-sql-lint 1.0.0-SNAPSHOT",
        description = "Static analyzer for SQL injection and unsafe JDBC/JPA query patterns in Java sources.")
public class SafeSqlLintCli implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(
            arity = "1..*",
            description = "Java source files or directories to analyze")
    private List<String> sourceStrings;

    @Option(
            names = {"--format"},
            description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    private OutputFormat format = OutputFormat.table;

    @Option(
            names = {"--quiet-table"},
            description = "When using json format, omit human-readable table from stderr")
    private boolean quietTable;

    enum OutputFormat {
        table,
        json,
        both
    }

    public static void main(String[] args) {
        int code = new CommandLine(new SafeSqlLintCli()).execute(args);
        System.exit(code);
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        RuleEngine engine = new RuleEngine();
        ProjectAnalyzer analyzer = new ProjectAnalyzer(engine);
        if (sourceStrings == null || sourceStrings.isEmpty()) {
            err.println("No Java sources provided.");
            return 2;
        }
        List<Path> sources = sourceStrings.stream().map(Path::of).toList();
        List<Finding> findings;
        try {
            findings = analyzer.analyzePaths(sources);
        } catch (AnalysisException e) {
            err.println(e.getMessage());
            return 2;
        }

        if (format == OutputFormat.json || format == OutputFormat.both) {
            out.println(JsonReporter.render(findings));
        }
        if (format == OutputFormat.table || format == OutputFormat.both) {
            if (format == OutputFormat.both && !quietTable) {
                err.println(TableReporter.render(findings));
            } else if (format == OutputFormat.table) {
                out.println(TableReporter.render(findings));
            }
        }

        return findings.isEmpty() ? 0 : 1;
    }
}
