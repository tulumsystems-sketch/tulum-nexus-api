package com.tulumcore.api.config;

import com.tulumcore.api.security.TenantFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
public class SecurityConfig {

    @Autowired
    private TenantFilter tenantFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Esto busca el bean corsConfigurationSource
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()
                        .requestMatchers("/api/external/**").permitAll()

                        // Config: ADMIN, OPERADOR y SUPER_ADMIN pueden leer
                        .requestMatchers(HttpMethod.GET, "/api/config/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        // Config: escribir solo ADMIN y SUPER_ADMIN
                        .requestMatchers("/api/config/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // Caja: estado, apertura, cierre para ADMIN, OPERADOR, SUPER_ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/caja/estado")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/caja/apertura")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/caja/cierre")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        // Caja: historial y demás para ADMIN y SUPER_ADMIN
                        .requestMatchers("/api/caja/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // Admin solo SUPER_ADMIN
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Dominios permitidos
        configuration.setAllowedOrigins(Arrays.asList(
                "https://tulum-core.netlify.app",
                "https://teal-tanuki-dea827.netlify.app",
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers permitidos (Importante incluir X-Tenant-ID si tu Front lo envía)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-Tenant-ID"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Caché de la respuesta preflight por 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}