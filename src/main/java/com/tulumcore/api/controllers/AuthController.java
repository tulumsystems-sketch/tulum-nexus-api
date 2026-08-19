package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import com.tulumcore.api.security.JwtService;
import com.tulumcore.api.security.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.sesion.inactividad-minutos:30}")
    private int inactividadMinutos;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        String claveIntento = claveLogin(loginRequest);
        try {
            if (loginRequest.tenant() == null || loginRequest.tenant().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "El campo empresa (tenant) es obligatorio"));
            }

            if (loginAttemptService.estaBloqueado(claveIntento)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Demasiados intentos. Proba de nuevo en unos minutos."));
            }

            TenantContext.setCurrentTenant(loginRequest.tenant());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            Usuario usuario = usuarioRepository.findByEmailAndTenantId(loginRequest.email(), loginRequest.tenant())
                    .orElse(null);
            if (usuario == null) {
                loginAttemptService.registrarFallo(claveIntento);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email o contraseña incorrectos"));
            }

            if (!comercioActivo(loginRequest.tenant(), usuario.getRol())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Comercio inactivo"));
            }

            loginAttemptService.registrarExito(claveIntento);

            String jwt = jwtService.generateToken(
                    loginRequest.email(),
                    loginRequest.tenant(),
                    usuario.getRol().name()
            );

            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "rol", usuario.getRol().name(),
                    "email", usuario.getEmail(),
                    "tenant", loginRequest.tenant(),
                    "expiresInMs", jwtExpirationMs,
                    "inactividadMinutos", inactividadMinutos
            ));

        } catch (AuthenticationException e) {
            loginAttemptService.registrarFallo(claveIntento);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email o contraseña incorrectos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno"));
        } finally {
            TenantContext.clear();
        }
    }

    private String claveLogin(LoginRequestDTO loginRequest) {
        String tenant = loginRequest != null && loginRequest.tenant() != null ? loginRequest.tenant().trim().toLowerCase() : "";
        String email = loginRequest != null && loginRequest.email() != null ? loginRequest.email().trim().toLowerCase() : "";
        return tenant + "|" + email;
    }

    private boolean comercioActivo(String tenantId, Rol rol) {
        if (rol == Rol.SUPER_ADMIN) {
            return true;
        }
        return tenantConfigRepository.findByTenantId(tenantId)
                .map(TenantConfig::isActivo)
                .orElse(false);
    }
}
