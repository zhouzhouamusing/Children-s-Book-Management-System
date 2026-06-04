package com.kidsbook.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsbook.common.Result;
import com.kidsbook.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtUtil jwtUtil;

    private static final String[] PUBLIC_URLS = {
        "/api/admin/login",
        "/api/admin/register",
        "/api/admin/reset-password",
        "/api/reader/login",
        "/api/reader/register",
        "/api/reader/reset-password",
        "/api/reader/send-code",
        "/api/health",
        "/error",
        "/",
        "/index.html",
        "/favicon.svg",
        "/assets/**",
        "/uploads/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ObjectMapper mapper = new ObjectMapper();
                    response.getWriter().write(mapper.writeValueAsString(Result.error(401, "未登录或登录已过期，请重新登录")));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ObjectMapper mapper = new ObjectMapper();
                    response.getWriter().write(mapper.writeValueAsString(Result.error(403, "权限不足")));
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers("/api/admin/send-code").permitAll()
                .requestMatchers("/api/sys/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/appeals/**").hasRole("ADMIN")
                .requestMatchers("/api/admin-application/apply", "/api/admin-application/my-status").hasRole("READER")
                .requestMatchers("/api/admin-application/list", "/api/admin-application/*/approve", "/api/admin-application/*/reject").hasRole("ADMIN")
                .requestMatchers("/api/reader-center/**").hasAnyRole("READER", "ADMIN")
                .requestMatchers("/api/categories/all").hasAnyRole("READER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter());
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtUtil jwtUtil;
        private static final AntPathMatcher pathMatcher = new AntPathMatcher();

        public JwtAuthenticationFilter(JwtUtil jwtUtil) {
            this.jwtUtil = jwtUtil;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtUtil.validateToken(token)) {
                        String username = jwtUtil.getUsernameFromToken(token);
                        List<String> roles = jwtUtil.getRolesFromToken(token);
                        List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        for (String role : roles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                        // SUPER_ADMIN and ADMIN roles both get ROLE_ADMIN authority
                        if (roles.contains("SUPER_ADMIN") && !roles.contains("ADMIN")) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                        for (String perm : permissions) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }

                        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                username, token, authorities);
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ignored) {
                }
            }
            filterChain.doFilter(request, response);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            for (String pattern : PUBLIC_URLS) {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            }
            return false;
        }
    }
}
