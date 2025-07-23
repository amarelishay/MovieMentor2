package movieMentor.repository; // החבילה שבה נשמור את כל מחלקות הגישה למסד (repositories)
import movieMentor.beans.User; // ✅ זה ה-import שצריך
import org.springframework.data.jpa.repository.JpaRepository; // ממשק בסיסי של Spring לגישה למסד
import org.springframework.stereotype.Repository; // מסמן ל-Spring שהממשק הזה הוא bean של repository

import java.util.Optional;

@Repository // הופך את הממשק ל־Component שנטען על ידי Spring – מוכן להזרקה
public interface UserRepository extends JpaRepository<User, Long> {
    // הממשק יורש את כל הפעולות הבסיסיות כמו:
    // save(), findById(), deleteById(), findAll(), count() ועוד...

    // מתודה למציאת משתמש לפי שם (לא חובה, אבל שימושית)
    Optional<User> findByName(String name);

    // מתודה למציאת משתמש לפי שם ותאריך לידה (למשל לאימות)
    Optional<User> findByNameAndBirthDate(String name, java.time.LocalDate birthDate);

    // מתודה לבדוק אם משתמש קיים לפי שם (לשימוש בהרשמה)
    boolean existsByName(String name);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);

}
