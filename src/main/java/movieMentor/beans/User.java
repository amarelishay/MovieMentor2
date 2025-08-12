package movieMentor.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.Email;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, length = 191)
    @Email
    private String email;

    @Column(unique = true, length = 191)
    private String username;

    private String password;

    private LocalDate birthDate;

    // רשימת סרטים מועדפים
    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorite_movies",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private List<Movie> favoriteMovies = new ArrayList<>();

    // היסטוריית צפייה בפורמט JSON
    @ElementCollection
    @CollectionTable(name = "user_watch_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "history_entry", columnDefinition = "LONGTEXT")
    private List<String> watchHistoryJson = new ArrayList<>();



    // המלצות סרטים למשתמש
    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_recommendations",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    @OrderColumn(name = "recommendation_order")
    private List<MovieDTO> recommendedMovies = new ArrayList<>();
}
