package com.small.backend.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Value("${api.version.prefix}")
    private String apiPrefix;

    /* Client sends localhost:{gateway-port}/api/v1/auth/login.
    Route to localhost:{auth-port}/api/v1/auth/login.
    Strip prefix /api/v1.
    AuthController handles /auth/login. */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("account-service", r -> r
                        .path(apiPrefix + "/accounts/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://account-service"))
                .route("auth-service", r -> r
                        .path(apiPrefix + "/auth/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://auth-service"))
                .route("order-service", r -> r
                        .path(apiPrefix + "/orders/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://order-service"))
                .route("payment-service", r -> r
                        .path(apiPrefix + "/payments/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://payment-service"))
                .build();
    }
}
