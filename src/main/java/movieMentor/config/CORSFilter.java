package movieMentor.config;

import javax.servlet.Filter; // שינוי כאן
import javax.servlet.FilterChain; // שינוי כאן
import javax.servlet.ServletException; // שינוי כאן
import javax.servlet.ServletRequest; // שינוי כאן
import javax.servlet.ServletResponse; // שינוי כאן
import javax.servlet.http.HttpServletRequest; // שינוי כאן
import javax.servlet.http.HttpServletResponse; // שינוי כאן

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // ודא שהוא מופעל ראשון בשרשרת הפילטרים
public class CORSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // הדפסות דיבוג - ניתן להסיר ב-Production
        System.out.println("CORSFilter: Processing request for path: " + request.getRequestURI());
        System.out.println("CORSFilter: Request Method: " + request.getMethod());


        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600"); // הגדר זמן Cache לבקשות Preflight

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("CORSFilter: Handling OPTIONS preflight request.");
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }
}