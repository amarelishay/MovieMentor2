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

        // ✅ מותר ל־Vite ול־React לדבר עם ה־API
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        // ✅ אילו מתודות מותרות
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // ✅ אילו כותרות מותרות (כולל Authorization)
        res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");

        res.setHeader("Vary", "Origin");
        res.setHeader("Access-Control-Max-Age", "3600");

        // ✅ אם הבקשה היא OPTIONS (Preflight) – מחזירים 200 ריק וזהו
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // ממשיכים את השרשרת
        chain.doFilter(request, response);
    }
}