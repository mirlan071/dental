package com.dentalcrm.common;

import com.dentalcrm.auth.AuthenticatedUserDetails;
import com.dentalcrm.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return username -> {
            var user = repository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return new AuthenticatedUserDetails(user);
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository,
                                            UserRepository repository, SessionRegistry sessionRegistry) throws Exception {
        return http
                .addFilterAfter(new ActiveUserFilter(repository), AbstractPreAuthenticatedProcessingFilter.class)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredSessionStrategy(event ->
                                event.getResponse().sendError(HttpStatus.UNAUTHORIZED.value())))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/csrf", "/api/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, error) -> response.sendError(HttpStatus.FORBIDDEN.value())))
                .build();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    private static final class ActiveUserFilter extends org.springframework.web.filter.OncePerRequestFilter {
        private final UserRepository users;

        private ActiveUserFilter(UserRepository users) {
            this.users = users;
        }

        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails details
                    && users.findByUsernameIgnoreCase(details.getUsername())
                    .map(user -> !user.isActive()
                            || details instanceof AuthenticatedUserDetails authenticated
                            && authenticated.authVersion() != user.getAuthVersion())
                    .orElse(true)) {
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                response.sendError(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            filterChain.doFilter(request, response);
        }
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .httpOnly(false)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax"));
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url:}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        if (!frontendUrl.isBlank()) {
            configuration.setAllowedOrigins(List.of(frontendUrl));
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
