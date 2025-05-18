package security;

import java.util.Date;

public interface JwtUtilBase {
    String extractEmail(String token);
    String extractJti(String token);
    Date extractExpiration(String token);
    boolean validateToken(String token);
}
