package fixtures.bad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.http.HttpServletRequest;

public class DynamicOrderBy {
    void listUsers(Connection conn, HttpServletRequest request) throws Exception {
        String sort = request.getParameter("sort");
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users ORDER BY " + sort);
        ps.executeQuery();
    }
}
