package movieMentor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // הוסף את זה
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // הוסף את זה

@Configuration
public class WebConfig implements WebMvcConfigurer { // הרחב מ-WebMvcConfigurer

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // אפשר CORS לכל הנתיבים ב-API
                // חשוב: הגדר את המקורות המדויקים שלך ב-Production!
                // לדוגמה: .allowedOrigins("http://localhost:5173", "https://your-deployed-frontend.com")
                .allowedOrigins("*") // למטרות פיתוח - פתוח לכל המקורות
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // אפשר שיטות אלו
                .allowedHeaders("*") // אפשר את כל הכותרות
                .allowCredentials(true) // חשוב אם אתה שולח עוגיות או Authorization headers
                .maxAge(3600); // זמן Cache לבקשות Preflight (שניהם)
    }
}