package security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class AbstractJwtUtilBase implements JwtUtilBase{
    protected Key key;

    protected void initKey(String base64Secret) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(decodedKey);
    }

    @Override
    public String extractEmail(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid or expired JWT token");
        }
    }

    @Override
    public String extractJti(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getId();
    }

    @Override
    public Date extractExpiration(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
