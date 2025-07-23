package movieMentor.services;

import movieMentor.beans.Movie;
import movieMentor.beans.User;

import java.util.List;

public interface UserService {

    void addFavoriteMovie(String username, String movieTitle);

    void removeFavoriteMovie(String username, Long movieId);

    void addToWatchHistory(String username, String movieTitle);

    List<Movie> getRecommendations(String username);

    List<Movie> getFavorites(String username);

    List<Movie> getHistory(String username);

    void setRecommendedMovies(String username, List<String> recommendedTitles);

    void updateRecommendations(User user);
}
