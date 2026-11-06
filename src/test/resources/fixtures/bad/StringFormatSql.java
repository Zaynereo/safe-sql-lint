package fixtures.bad;

import java.sql.Connection;
import java.sql.Statement;
import javax.servlet.http.HttpServletRequest;

public class StringFormatSql {
    void search(Connection conn, HttpServletRequest request) throws Exception {
        String q = request.getParameter("q");
        String sql = String.format("SELECT * FROM docs WHERE title LIKE '%%%s%%'", q);
        Statement stmt = conn.createStatement();
        stmt.executeQuery(sql);
    }
}
