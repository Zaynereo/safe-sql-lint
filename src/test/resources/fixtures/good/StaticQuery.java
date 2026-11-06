package fixtures.good;

import java.sql.Connection;
import java.sql.Statement;

public class StaticQuery {
    void count(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeQuery("SELECT COUNT(*) FROM users");
    }
}
