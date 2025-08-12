package movieMentor.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_watch_history")
public class UserWatchEntry {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)   // FK ל־users.id
        private User user;

        @Embedded
        private MovieDTO movie;

        @Column(name = "watched_at", nullable = false)
        private LocalDateTime watchedAt = LocalDateTime.now();
        @Lob
        @Convert(converter = MovieDTOJsonConverterInline.class)
        @Column(name = "movie_snapshot", nullable = false, columnDefinition = "TEXT")
        private MovieDTO movieSnapshot;

        // ---- ממיר פנימי, אותו קובץ ----
        @Converter(autoApply = false)
        public static class MovieDTOJsonConverterInline implements AttributeConverter<MovieDTO, String> {
                private static final ObjectMapper MAPPER = new ObjectMapper()
                        .registerModule(new JavaTimeModule());

                @Override
                public String convertToDatabaseColumn(MovieDTO attribute) {
                        try { return attribute == null ? null : MAPPER.writeValueAsString(attribute); }
                        catch (Exception e) { throw new IllegalStateException("Failed to serialize MovieDTO", e); }
                }

                @Override
                public MovieDTO convertToEntityAttribute(String dbData) {
                        try { return dbData == null ? null : MAPPER.readValue(dbData, MovieDTO.class); }
                        catch (Exception e) { throw new IllegalStateException("Failed to deserialize MovieDTO", e); }
                }
        }
}
