package movieMentor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
                .allowedOrigins("http://localhost:5173", "https://moviementor-sdhn.onrender.com") // הגדר במדויק את המקורות המותרים
                // עבור סביבת פיתוח, '*' אפשרי אך פחות מאובטח
                // .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // אפשר שיטות אלו
                .allowedHeaders("*") // אפשר את כל הכותרות
                .allowCredentials(true) // אפשר credentials (לדוגמה, עוגיות או כותרות Authorization)
                .maxAge(3600); // זמן Cache לבקשות Preflight
    }
}