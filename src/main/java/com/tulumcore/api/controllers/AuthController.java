package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.TenantConfig;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.TenantConfigRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import com.tulumcore.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO req) {
        if (req.tenant() == null || req.tenant().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo tenant es obligatorio"));
        }
        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo email es obligatorio"));
        }
        if (req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener al menos 6 caracteres"));
        }

        try {
            TenantContext.setCurrentTenant(req.tenant());

            if (usuarioRepository.findByEmail(req.email()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un usuario con ese email"));
            }

            Usuario usuario = new Usuario();
            usuario.setEmail(req.email());
            usuario.setPassword(passwordEncoder.encode(req.password()));
            usuario.setRol(Rol.ADMIN);
            usuario.setTenantId(req.tenant());
            usuarioRepository.save(usuario);

            TenantConfig config = new TenantConfig();
            config.setNombreEmpresa(req.companyName() != null ? req.companyName() : req.tenant());
            config.setTenantId(req.tenant());
            tenantConfigRepository.save(config);

            String jwt = jwtService.generateToken(req.email(), req.tenant(), Rol.ADMIN.name());

            return ResponseEntity.ok(Map.of(
                "token", jwt,
                "rol", Rol.ADMIN.name(),
                "email", req.email(),
                "tenant", req.tenant()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al registrar: " + e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            // 1. Validar que la empresa (tenant) venga en el body
            if (loginRequest.tenant() == null || loginRequest.tenant().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "El campo empresa (tenant) es obligatorio"));
            }

            // 2. Setear TenantContext para que el @TenantId de Hibernate filtre correctamente
            TenantContext.setCurrentTenant(loginRequest.tenant());

            // 3. Intentar autenticar con Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            // 4. Buscar el usuario
            Usuario usuario = usuarioRepository.findByEmail(loginRequest.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 5. Generar Token
            String jwt = jwtService.generateToken(
                    loginRequest.email(),
                    loginRequest.tenant(),
                    usuario.getRol().name()
            );

            // 6. Respuesta exitosa
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "rol", usuario.getRol().name(),
                    "email", usuario.getEmail(),
                    "tenant", loginRequest.tenant()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email o contraseña incorrectos"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno: " + e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }
}