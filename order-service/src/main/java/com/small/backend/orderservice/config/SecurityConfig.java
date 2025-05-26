package com.small.backend.orderservice.config;

import com.small.backend.orderservice.security.CustomAuthenticationEntryPoint;
import com.small.backend.orderservice.security.GatewayHeaderAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {
    private final String internalToken;
    private final String internalAuthHeader;
    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    @Autowired
    public SecurityConfig(@Value("${internal.auth.token}") String internalToken,
                          @Value("${internal.auth.header}") String internalAuthHeader,
                          GatewayHeaderAuthFilter gatewayHeaderAuthFilter) {
        this.internalToken = internalToken;
        this.internalAuthHeader = internalAuthHeader;
        this.gatewayHeaderAuthFilter = gatewayHeaderAuthFilter;
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                         .requestMatchers(HttpMethod.POST, "/orders/{id}/complete").access(internalCallAuthorizationManager())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> internalCallAuthorizationManager() {
        return (authenticationSupplier, context) -> {
            HttpServletRequest request = context.getRequest();
            String internalHeader = request.getHeader(internalAuthHeader);
            boolean isGranted = internalToken.equals(internalHeader);
            return new AuthorizationDecision(isGranted);
        };
    }
}
