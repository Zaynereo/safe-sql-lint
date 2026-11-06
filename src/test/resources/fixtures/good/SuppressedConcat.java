package fixtures.good;

import java.sql.Connection;
import java.sql.Statement;
import javax.servlet.http.HttpServletRequest;

@SuppressWarnings({"safe-sql-lint:SQL001", "safe-sql-lint:SQL002"})
public class SuppressedConcat {
    void legacy(Connection conn, HttpServletRequest request) throws Exception {
        String id = request.getParameter("id");
        Statement stmt = conn.createStatement();
        stmt.executeQuery("SELECT * FROM audit WHERE id = " + id);
    }
}
