package security;

public interface JwtUtilBase {
    String extractEmail(String token);
    boolean validateToken(String token);
}
