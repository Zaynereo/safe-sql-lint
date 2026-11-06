package fixtures.bad;

import java.sql.Connection;
import java.sql.Statement;
import org.springframework.web.bind.annotation.RequestParam;

public class RequestParamInjection {
    void findByName(Connection conn, @RequestParam String name) throws Exception {
        Statement s = conn.createStatement();
        s.executeUpdate("DELETE FROM items WHERE name = '" + name + "'");
    }
}
