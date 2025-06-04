package com.small.backend.apigateway.security;

import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import util.AuthConstants;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;
    private final RedisJtiService redisJtiService;

    @Value("${api.version.prefix}")
    private String apiPrefix;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, RedisJtiService redisJtiService) {
        this.jwtUtil = jwtUtil;
        this.redisJtiService = redisJtiService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String path = exchange.getRequest().getURI().getPath();

        // Allow public routes
        if (path.startsWith(apiPrefix + "/auth")) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        final String authHeader = exchange.getRequest().getHeaders().getFirst(AuthConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(AuthConstants.BEARER_PREFIX)) { // absent token

            // Let the frontend handle redirect.
            // If "Automatically follow redirects" is turned on in Postman,
            // this will fail because the login endpoint expects a JSON request body.
            // exchange.getResponse().setStatusCode(HttpStatus.FOUND); // 302 redirect
            // exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, "/api/v1/auth/login");
            // return exchange.getResponse().setComplete();

            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "User not logged in.");
        }

        // Validate JWT
        final String token = authHeader.substring(AuthConstants.BEARER_PREFIX.length());
        try {
            if (!jwtUtil.validateToken(token) || !redisJtiService.isJtiValid(jwtUtil.extractJti(token))) {
                throw new SignatureException("Invalid or expired access token.");
            }

            // Mutate the request and update the exchange for downstream services (e.g., account-service).
            // May use a more sophisticated mechanism instead, e.g., internal token from the API gateway.
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(AuthConstants.USER_EMAIL_HEADER, jwtUtil.extractEmail(token))
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
            return chain.filter(mutatedExchange);
        } catch (SignatureException | IllegalArgumentException ex) {
            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    // Ensure the filter runs early in the filter chain.
    @Override
    public int getOrder() {
        return -1; // Higher precedence
    }

    private static Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);

        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(message.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
