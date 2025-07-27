package movieMentor.config;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CORSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // ✅ מאפשר גישה מ-כל דומיין
        res.setHeader("Access-Control-Allow-Origin", "*");

        // ✅ מתודות שמותרות
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // ✅ מאפשר לשלוח כל כותרת כולל Authorization
        res.setHeader("Access-Control-Allow-Headers", "*");

        // ✅ קובע caching של preflight
        res.setHeader("Access-Control-Max-Age", "3600");

        // ✅ אם הבקשה היא OPTIONS – מחזירים תשובת 200 מיד
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // ממשיכים בשרשרת
        chain.doFilter(request, response);
    }
}
