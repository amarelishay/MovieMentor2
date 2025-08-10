package movieMentor.controllers;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.MovieDTO;
import movieMentor.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @PostMapping("/favorites/{title}")
    public ResponseEntity<String> addFavorite(@PathVariable String title, Authentication auth) {
        String username = auth.getName();
        log.info("📌 {} adds '{}' to favorites", username, title);
        userService.addFavoriteMovie(username, title);
        return ResponseEntity.ok("✔️ נוסף למועדפים");
    }

    @DeleteMapping("/delete_favorites/{movieId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long movieId, Authentication auth) {
        userService.removeFavoriteMovie(auth.getName(), movieId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/history/{title}")
    public ResponseEntity<String> addToHistory(@PathVariable String title, Authentication auth) {
        userService.addToWatchHistory(auth.getName(), title);
        return ResponseEntity.ok("👁️ נוסף להיסטוריית צפייה");
    }

    @PostMapping("/recommendations")
    public ResponseEntity<String> updateRecommendations(@RequestBody List<String> titles, Authentication auth) {
        userService.setRecommendedMovies(auth.getName(), titles);
        return ResponseEntity.ok("✅ רשימת ההמלצות עודכנה");
    }

//    @GetMapping("/recommendations")
//    public ResponseEntity<List<MovieDTO>> getRecommendations(Authentication auth) {
//        return ResponseEntity.ok(userService.getRecommendations(auth.getName()));
//    }
@GetMapping("/recommendations")
public ResponseEntity<List<MovieDTO>> getRecommendations(Authentication auth) {
    String username = auth.getName();
    log.info("🎯 getRecommendations called for user: {}", username);

    try {
        List<MovieDTO> recommendations = userService.getRecommendations(username);
        log.info("📊 Found {} recommendations for user {}: {}",
                recommendations.size(), username,
                recommendations.stream().map(MovieDTO::getTitle).collect(Collectors.toList()));

        return ResponseEntity.ok(recommendations);
    } catch (Exception e) {
        log.error("❌ Error fetching recommendations for user {}: {}", username, e.getMessage(), e);
        return ResponseEntity.status(500).body(Collections.emptyList());
    }
}
    @GetMapping("/favorites")
    public ResponseEntity<List<MovieDTO>> getFavorites(Authentication auth){
        return ResponseEntity.ok(userService.getFavorites(auth.getName()));
    }
}
