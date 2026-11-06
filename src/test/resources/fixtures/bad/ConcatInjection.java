package fixtures.bad;

import java.sql.Connection;
import java.sql.Statement;
import javax.servlet.http.HttpServletRequest;

public class ConcatInjection {
    void lookup(Connection conn, HttpServletRequest request) throws Exception {
        String id = request.getParameter("id");
        Statement stmt = conn.createStatement();
        stmt.executeQuery("SELECT * FROM users WHERE id = " + id);
    }
}
