package movieMentor.beans;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Genre implements Serializable {

    @EqualsAndHashCode.Include
    @Id
    private Long id;
    @Column(nullable = false, unique = true, length = 191)
    private String name;

}
