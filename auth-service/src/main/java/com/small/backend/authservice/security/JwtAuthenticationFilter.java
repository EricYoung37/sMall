package com.small.backend.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import util.AppConstants;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // The FilterChain parameter represents the remaining filters in the chain.
        // Recall addFilterBefore() in SecurityFilterChain.

        // For the access token.
        final String authHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(AppConstants.BEARER_PREFIX)) {
            final String jwt = authHeader.substring(AppConstants.BEARER_PREFIX.length());
            final String email = jwtUtil.extractEmail(jwt);

            // Each HTTP request creates its own Authentication object per user across multiple requests.
            // The Authentication object is stored in the SecurityContextHolder for the current thread/request.
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUtil.validateToken(jwt)) {
                    // Set userDetails (principal) in the Authentication object.
                    // Usage: updatePassword() in AuthController.
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // Web-related metadata about the request, e.g., remoteAddress, sessionId, etc.
                    // Retrieved by SecurityContextHolder.getContext().getAuthentication().getDetails();
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken); // store the Auth object.
                }
            }
        }

        filterChain.doFilter(request, response); // The current filter passes control to the next filter in the chain.
    }
}
