package com.safesqllint.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeSqlLintCliTest {

    @TempDir
    Path tempDir;

    @Test
    void exitsZeroOnCleanSource() throws IOException {
        Path file = tempDir.resolve("Clean.java");
        Files.writeString(
                file,
                """
                public class Clean {
                  void m(java.sql.Connection c) throws Exception {
                    var ps = c.prepareStatement("SELECT 1");
                    ps.executeQuery();
                  }
                }
                """);

        SafeSqlLintCli cli = new SafeSqlLintCli();
        CommandLine cmd = new CommandLine(cli);
        int code = cmd.execute(file.toString());
        assertEquals(0, code);
    }

    @Test
    void exitsNonZeroWhenFindingsExist() throws IOException {
        Path file = tempDir.resolve("Bad.java");
        Files.writeString(
                file,
                """
                import javax.servlet.http.HttpServletRequest;
                public class Bad {
                  void m(java.sql.Connection c, HttpServletRequest r) throws Exception {
                    String id = r.getParameter("id");
                    c.createStatement().executeQuery("SELECT * FROM t WHERE id = " + id);
                  }
                }
                """);

        SafeSqlLintCli cli = new SafeSqlLintCli();
        CommandLine cmd = new CommandLine(cli);
        int code = cmd.execute("--format", "json", file.toString());
        assertEquals(1, code);
    }

    @Test
    void jsonOutputContainsRuleCode() throws IOException {
        Path file = tempDir.resolve("Bad.java");
        Files.writeString(
                file,
                """
                import javax.servlet.http.HttpServletRequest;
                public class Bad {
                  void m(java.sql.Connection c, HttpServletRequest r) throws Exception {
                    String id = r.getParameter("id");
                    c.createStatement().executeQuery("SELECT * FROM t WHERE id = " + id);
                  }
                }
                """);

        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.PrintWriter writer = new java.io.PrintWriter(bytes, true, java.nio.charset.StandardCharsets.UTF_8);
        SafeSqlLintCli cli = new SafeSqlLintCli();
        CommandLine cmd = new CommandLine(cli);
        cmd.setOut(writer);
        int code = cmd.execute("--format", "json", file.toString());
        writer.flush();
        assertEquals(1, code);
        String json = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("SQL001"), () -> "output was: " + json);
    }
}
