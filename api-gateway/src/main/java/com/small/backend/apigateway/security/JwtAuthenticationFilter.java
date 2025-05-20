package com.small.backend.apigateway.security;

import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import util.AppConstants;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final RedisJtiService redisJtiService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, RedisJtiService redisJtiService) {
        this.jwtUtil = jwtUtil;
        this.redisJtiService = redisJtiService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String path = exchange.getRequest().getURI().getPath();

        // Allow public routes
        if (path.startsWith("/api/v1/auth")) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        final String authHeader = exchange.getRequest().getHeaders().getFirst(AppConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(AppConstants.BEARER_PREFIX)) { // absent token

            // Let the frontend handle redirect.
            // If "Automatically follow redirects" is turned on in Postman,
            // this will fail because the login endpoint expects a JSON request body.
            // exchange.getResponse().setStatusCode(HttpStatus.FOUND); // 302 redirect
            // exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, "/api/v1/auth/login");
            // return exchange.getResponse().setComplete();

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);

            String message = "Missing or invalid Authorization header. Expected format: 'Authorization: Bearer <token>'";
            DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(message.getBytes(StandardCharsets.UTF_8));

            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        // Validate JWT
        final String token = authHeader.substring(AppConstants.BEARER_PREFIX.length());
        try {
            if (!jwtUtil.validateToken(token) || !redisJtiService.isJtiValid(jwtUtil.extractJti(token))) {
                throw new SignatureException("Invalid or expired access token.");
            }

            // Mutate the request and update the exchange for downstream services (e.g., account-service).
            // May use a more sophisticated mechanism instead, e.g., internal token from the API gateway.
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(AppConstants.USER_EMAIL_HEADER, jwtUtil.extractEmail(token))
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
            return chain.filter(mutatedExchange);
        } catch (SignatureException | IllegalArgumentException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            // May return a message as in the case of absent token.
            return exchange.getResponse().setComplete();
        }
    }

    // Ensure the filter runs early in the filter chain.
    @Override
    public int getOrder() {
        return -1; // Higher precedence
    }
}
