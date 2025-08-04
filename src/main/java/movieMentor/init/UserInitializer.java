package movieMentor.init;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.beans.User;
import movieMentor.enums.TopMoviesData;
import movieMentor.repository.UserRepository;
import movieMentor.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserInitializer implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private final TmdbService tmdbService;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;



    @Override
    public void run(String... args) {
        userRepository.deleteAll();
        final String username = "Elishayamar";
        if (userRepository.findByUsername(username).isPresent()) {
            logger.info("User already exists: {}", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("14789510"))
                .email("elishay61@gmail.com")
                .name("Elishay Amar")
                .birthDate(LocalDate.of(1999, 6, 3))
                .build();

        userRepository.save(user);
        logger.info("Created new user: {}", username);

        // סרטים אהובים
        Arrays.asList("harry potter", "spongebob").forEach(title ->
                safe(() -> userService.addFavoriteMovie(username, title)));

        // היסטוריית צפייה
        Arrays.asList("The Matrix", "Avatar", "Tenet","The LEGO Batman Movie","The LEGO Ninjago Movie","לשחרר את שולי","תמונת הניצחון","חגיגה בסנוקר","אבא גנוב","שושנה").forEach(title ->
                safe(() -> userService.addToWatchHistory(username, title)));

        logger.info("Initialized favorite movies and watch history for user: {}", username);
    }

    private void safe(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error("Error during initialization: {}", e.getMessage(), e);
        }
    }
}