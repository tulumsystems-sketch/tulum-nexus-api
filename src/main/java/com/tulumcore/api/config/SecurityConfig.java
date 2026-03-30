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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private TenantFilter tenantFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()
                        .requestMatchers("/api/external/**").permitAll()

                        // Solo ADMIN puede gestionar configuración y caja
                        .requestMatchers(HttpMethod.GET, "/api/config/**")
                            .hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/config/**")
                            .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/caja/estado").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/caja/apertura").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/caja/cierre").hasAnyRole("ADMIN", "OPERADOR")

                        .requestMatchers("/api/caja/**").hasRole("ADMIN")

                        // ADMIN y OPERADOR pueden operar el resto
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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