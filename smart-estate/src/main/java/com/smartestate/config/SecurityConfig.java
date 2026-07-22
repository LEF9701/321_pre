package com.smartestate.config;

import com.smartestate.security.MobileTokenAuthFilter;
import com.smartestate.security.MobileTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stateless, CSRF-exempt chain for the native mobile app (/api/mobile/**). A native client can't
     * carry the session-cookie + CSRF token the web UI uses, so requests authenticate with an
     * opaque bearer token via {@link MobileTokenAuthFilter}. Ordered before the web chain so these
     * paths never fall through to form login. Only the login endpoint is public.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, MobileTokenService tokenService) throws Exception {
        http
            .securityMatcher("/api/mobile/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/mobile/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            // Return 401 for unauthenticated API calls instead of redirecting to the HTML login page.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new MobileTokenAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/app/**", "/h2-console/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // AI module: document ingestion is admin-only, evaluation is staff, chat is any role
                .requestMatchers("/ai/documents/**").hasRole("ADMIN")
                .requestMatchers("/ai/evaluation/**").hasAnyRole("MANAGEMENT", "ADMIN")
                .requestMatchers("/ai/**").authenticated()
                // management pages: management staff and admins (admin is a superset)
                .requestMatchers("/mgmt/**").hasAnyRole("MANAGEMENT", "ADMIN")
                .requestMatchers("/resident/**").hasRole("RESIDENT")
                .requestMatchers("/uploads/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // H2 console serves frames and posts without CSRF tokens; relax for it only
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
