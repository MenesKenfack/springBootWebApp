package com.kaksha.library.config;

import com.kaksha.library.model.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify-email", "/api/auth/resend-verification").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/resources/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resources").permitAll()
                .requestMatchers("/api/purchase/webhook").permitAll()
                .requestMatchers("/api/purchase/campay/callback/**").permitAll()
                // Public pages (Thymeleaf templates)
                .requestMatchers("/", "/index", "/login", "/register").permitAll()
                .requestMatchers("/resource/**").permitAll()
                .requestMatchers("/dashboard", "/resources", "/purchases", "/profile", "/my-list").permitAll()
                .requestMatchers("/analytics", "/users", "/manage-resources", "/catalogs", "/terms", "/backup").permitAll()
                // Static resources
                .requestMatchers("/index.html", "/login.html", "/register.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/*.html", "/assets/**").permitAll()
                // Swagger/OpenAPI if needed
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Actuator health endpoints for monitoring (NFR-03)
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics").permitAll()
                .requestMatchers("/actuator/**").hasAuthority(UserRole.ROLE_MANAGER.name())
                // Secured endpoints
                .requestMatchers("/api/resources/**").hasAnyAuthority(
                    UserRole.ROLE_CLIENT.name(), 
                    UserRole.ROLE_LIBRARIAN.name(), 
                    UserRole.ROLE_MANAGER.name()
                )
                .requestMatchers("/api/purchase/**").hasAuthority(UserRole.ROLE_CLIENT.name())
                .requestMatchers("/api/admin/**", "/api/reports/**", "/api/backups/**").hasAuthority(UserRole.ROLE_MANAGER.name())
                .requestMatchers(HttpMethod.POST, "/api/resources").hasAnyAuthority(
                    UserRole.ROLE_LIBRARIAN.name(), 
                    UserRole.ROLE_MANAGER.name()
                )
                .requestMatchers(HttpMethod.PUT, "/api/resources/**").hasAnyAuthority(
                    UserRole.ROLE_LIBRARIAN.name(), 
                    UserRole.ROLE_MANAGER.name()
                )
                .requestMatchers(HttpMethod.DELETE, "/api/resources/**").hasAnyAuthority(
                    UserRole.ROLE_LIBRARIAN.name(), 
                    UserRole.ROLE_MANAGER.name()
                )
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    @SuppressWarnings("deprecation")
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
