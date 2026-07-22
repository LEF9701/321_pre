package com.smartestate.security;

import com.smartestate.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates mobile API requests from an {@code Authorization: Bearer <token>} header. On a valid
 * token it populates the {@link SecurityContextHolder} with the resolved user's role authority, so
 * downstream code (e.g. {@code CurrentUserService}) works as it does for web sessions.
 * Invalid/absent tokens are left unauthenticated; the security chain then rejects protected routes.
 */
public class MobileTokenAuthFilter extends OncePerRequestFilter {

    private final MobileTokenService tokenService;

    public MobileTokenAuthFilter(MobileTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            User user = tokenService.resolve(token).orElse(null);
            if (user != null) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
