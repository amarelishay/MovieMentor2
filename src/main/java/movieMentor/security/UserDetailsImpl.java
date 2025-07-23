package movieMentor.security;

import lombok.AllArgsConstructor;
import movieMentor.beans.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // אין הרשאות במערכת הזו כרגע
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // מחזיר את הסיסמה המוצפנת
    }

    @Override
    public String getUsername() {
        return user.getUsername(); // משמש ל-Authentication
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // נניח שאין חשבונות שפגו תוקף
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // נניח שאין נעילות
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // סיסמאות לא פגות תוקף
    }

    @Override
    public boolean isEnabled() {
        return true; // כל המשתמשים פעילים
    }

    public User getUser() {
        return user;
    }
}
