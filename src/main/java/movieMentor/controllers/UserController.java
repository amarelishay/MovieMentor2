package movieMentor.controllers;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @PostMapping("/favorites/{title}")
    public ResponseEntity<?> addFavorite(@PathVariable String title, Authentication auth) {
        String username = auth.getName();
        log.info("📌 {} is adding '{}' to favorites", username, title);
        userService.addFavoriteMovie(username, title);
        return ResponseEntity.ok("✔️ סרט נוסף למועדפים");
    }

    @PostMapping("/history/{title}")
    public ResponseEntity<?> addToHistory(@PathVariable String title, Authentication auth) {
        String username = auth.getName();
        log.info("📌 {} is adding '{}' to watch history", username, title);
        userService.addToWatchHistory(username, title);
        return ResponseEntity.ok("👁️ נוסף להיסטוריית צפייה");
    }

    @PostMapping("/recommendations")
    public ResponseEntity<?> updateRecommendations(@RequestBody List<String> titles, Authentication auth) {
        String username = auth.getName();
        log.info("🔁 {} is updating recommendations with: {}", username, titles);
        userService.setRecommendedMovies(username, titles);
        return ResponseEntity.ok("✅ רשימת ההמלצות עודכנה");
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<Movie>> getRecommendations(Authentication auth) {
        String username = auth.getName();
        log.info("📥 {} is retrieving recommendations", username);
        return ResponseEntity.ok(userService.getRecommendations(username));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<Movie>> getFavorites(Authentication auth){
        String username = auth.getName();
        log.info("📥 {} is retrieving favorites movies", username);
        return ResponseEntity.ok(userService.getFavorites(username));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Movie>> getHistory(Authentication auth){
        String username = auth.getName();
        log.info("📥 {} is retrieving watch history", username);
        return ResponseEntity.ok(userService.getHistory(username));
    }
    @DeleteMapping("/delete_favorites/{movieId}")
    public ResponseEntity<Void> removeFavoriteMovie(@PathVariable Long movieId,Authentication auth) {
        String username = auth.getName();
        userService.removeFavoriteMovie(username, movieId);
        return ResponseEntity.noContent().build();
    }

}
