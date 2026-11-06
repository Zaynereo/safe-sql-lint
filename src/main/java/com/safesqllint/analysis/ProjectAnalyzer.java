package com.safesqllint.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.safesqllint.model.Finding;
import com.safesqllint.rules.RuleEngine;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class ProjectAnalyzer {

    private final RuleEngine ruleEngine;
    private final JavaParser parser;

    public ProjectAnalyzer(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        config.setAttributeComments(true);
        this.parser = new JavaParser(config);
    }

    public List<Finding> analyzePaths(List<Path> roots) throws IOException {
        List<Finding> all = new ArrayList<>();
        for (Path root : roots) {
            if (Files.isRegularFile(root) && root.toString().endsWith(".java")) {
                all.addAll(analyzeFile(root));
            } else if (Files.isDirectory(root)) {
                try (Stream<Path> walk = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
                    walk.filter(p -> p.toString().endsWith(".java"))
                            .forEach(p -> {
                                try {
                                    all.addAll(analyzeFile(p));
                                } catch (IOException e) {
                                    throw new AnalysisException("Failed to read " + p + ": " + e.getMessage(), e);
                                }
                            });
                }
            }
        }
        Collections.sort(all);
        return all;
    }

    private List<Finding> analyzeFile(Path file) throws IOException {
        ParseResult<CompilationUnit> result = parser.parse(file);
        if (result.getProblems().size() > 0 && result.getResult().isEmpty()) {
            throw new AnalysisException(
                    "Parse errors in " + file + ": " + result.getProblems().get(0).getMessage());
        }
        CompilationUnit unit = result.getResult().orElseThrow(() -> new AnalysisException("Empty parse: " + file));
        SourceFileAnalyzer fileAnalyzer = new SourceFileAnalyzer(ruleEngine);
        return fileAnalyzer.analyze(file, unit);
    }
}
