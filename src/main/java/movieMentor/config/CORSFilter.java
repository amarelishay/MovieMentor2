package movieMentor.config;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // לא צריך קוד בהתחלה
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;

        // ✅ מאפשר גם ל-5173 וגם ל-8080
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        res.setHeader("Vary", "Origin"); // כדי לאפשר דומיינים שונים בפיתוח
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        res.setHeader("Access-Control-Max-Age", "3600");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // אין צורך בקוד כאן
    }
}
