package movieMentor.config;

import javax.servlet.Filter; // שונה ל-javax
import javax.servlet.FilterChain; // שונה ל-javax
import javax.servlet.ServletException; // שונה ל-javax
import javax.servlet.ServletRequest; // שונה ל-javax
import javax.servlet.ServletResponse; // שונה ל-javax
import javax.servlet.http.HttpServletRequest; // שונה ל-javax
import javax.servlet.http.HttpServletResponse; // שונה ל-javax

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

        // --- הדפסות דיבוג - חשוב לבדוק בלוגים של Render! ---
        System.out.println("CORSFilter: Processing request for path: " + request.getRequestURI());
        System.out.println("CORSFilter: Request Method: " + request.getMethod());
        // ---------------------------------------------------

        response.setHeader("Access-Control-Allow-Origin", "*"); // פתוח לכל המקורות (לפיתוח!)
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600"); // מאפשר לדפדפן לשמור ב-Cache את תוצאות Preflight למשך שעה

        // טיפול בבקשות OPTIONS (Preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("CORSFilter: Handling OPTIONS preflight request. Setting status to OK.");
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }
}