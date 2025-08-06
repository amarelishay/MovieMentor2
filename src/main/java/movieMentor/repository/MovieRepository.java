package movieMentor.repository; // החבילה של כל ה־repositories

import movieMentor.beans.Movie;
import org.springframework.data.jpa.repository.JpaRepository; // הממשק של Spring שמנהל את הגישה למסד
import org.springframework.stereotype.Repository; // מסמן שהמחלקה היא bean של Repository

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository // מסמן ל־Spring שזו מחלקה לגישה למסד נתונים
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // הממשק יורש מ־JpaRepository – כל הפונקציות הבסיסיות מגיעות אוטומטית:
    // save(), findById(), deleteById(), findAll() וכו'

    // מתודה למציאת סרטים לפי תאריך יציאה
    List<Movie> findByReleaseDate(LocalDate releaseDate);

    // מתודה למציאת סרטים לפי מילה/חלק בשם (case insensitive)
    List<Movie> findByTitleContainingIgnoreCase(String title);

    // מתודה למציאת סרטים עם פופולריות גבוהה יותר מערך מסוים
    List<Movie> findByPopularityGreaterThan(Double popularityThreshold);

//    Optional<Movie> findByTitle(String title);
    List<Movie> findAllByTitle(String title);

}
