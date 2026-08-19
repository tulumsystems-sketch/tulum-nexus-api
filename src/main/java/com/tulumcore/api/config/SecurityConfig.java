package com.tulumcore.api.config;

import com.tulumcore.api.security.TenantFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Esto busca el bean corsConfigurationSource
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/api/health").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()
                        .requestMatchers("/api/external/**").authenticated()

                        // Config: cualquier rol autenticado del tenant puede leerla,
                        // el front la necesita para armar el menú y la marca
                        .requestMatchers(HttpMethod.GET, "/api/config/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "PREVENTISTA", "SUPER_ADMIN")
                        // Config: escribir solo ADMIN y SUPER_ADMIN
                        .requestMatchers("/api/config/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // Punto de venta: el PREVENTISTA toma pedidos y remitos, no cobra en el mostrador.
                        // Se bloquea acá y no sólo escondiendo el botón en el front.
                        .requestMatchers(HttpMethod.POST, "/api/ventas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ventas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers("/api/pagos/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        // Cobranzas de remitos: el preventista arma hojas de ruta, no cobra.
                        .requestMatchers("/api/remitos/cobranzas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/remitos/*/pagos")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/remitos/*/pagos")
                        .hasAnyRole("ADMIN", "OPERADOR", "SUPER_ADMIN")
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
                        // Usuarios: administración solo ADMIN y SUPER_ADMIN
                        .requestMatchers("/api/usuarios/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // Admin solo SUPER_ADMIN
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/superadmin/**").hasRole("SUPER_ADMIN")
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Dominios permitidos, configurables por entorno con CORS_ALLOWED_ORIGINS
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers permitidos (Importante incluir X-Tenant-ID si tu Front lo envía)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-Tenant-ID",
                "X-Bot-Secret"
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
