package movieMentor.services;

import movieMentor.beans.User;
import movieMentor.beans.MovieDTO;

import java.util.List;

public interface UserService {

    void addFavoriteMovie(String username, String movieTitle);

    void removeFavoriteMovie(String username, Long movieId);

    void addToWatchHistory(String username, String movieTitle);

    List<MovieDTO> getRecommendations(String username);

    List<MovieDTO> getFavorites(String username);

    List<MovieDTO> getHistoryDTO(String username);

    void setRecommendedMovies(String username, List<String> recommendedTitles);

    void updateRecommendations(User user);
}
