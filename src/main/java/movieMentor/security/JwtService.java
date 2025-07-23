package movieMentor.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;

    // מייצר טוקן חדש לפי שם משתמש
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // מתי נוצר
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // תוקף
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // הצפנה
                .compact();
    }

    // מחלץ את שם המשתמש מתוך הטוקן
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // בדיקת תוקף טוקן
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // מחלץ תביעה ספציפית מהטוקן
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // מחלץ את כל התביעות
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    // בדיקה אם הטוקן פג תוקף
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
