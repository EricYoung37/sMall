package util;

public class AppConstants {
    // using this instead of application.properties because @RequestHeader doesn't support value injection via @Value.
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
}
