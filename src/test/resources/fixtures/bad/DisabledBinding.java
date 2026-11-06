package fixtures.bad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.http.HttpServletRequest;

public class DisabledBinding {
    void updateEmail(Connection conn, HttpServletRequest request) throws Exception {
        String email = request.getParameter("email");
        PreparedStatement ps = conn.prepareStatement("UPDATE users SET email = '" + email + "' WHERE id = 1");
        ps.setString(1, "unused");
        ps.executeUpdate();
    }
}
