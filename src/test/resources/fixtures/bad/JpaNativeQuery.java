package fixtures.bad;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

public class JpaNativeQuery {
    void nativeFind(EntityManager em, HttpServletRequest request) {
        String table = request.getParameter("table");
        em.createNativeQuery("SELECT * FROM " + table);
    }
}
