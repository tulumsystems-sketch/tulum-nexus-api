package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Usuario;
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

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            // 1. Validar que la empresa (tenant) venga en el body
            if (loginRequest.tenant() == null || loginRequest.tenant().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "El campo empresa (tenant) es obligatorio"));
            }

            // NOTA PARA EL FRONTEND: Es vital que en Axios (Login.tsx) envíes el Header 'X-Tenant-ID'
            // en esta misma petición de login, de lo contrario CustomUserDetailsService fallará.

            // 2. Intentar autenticar con Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            // 3. Buscar el usuario
            Usuario usuario = usuarioRepository.findByEmail(loginRequest.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 4. Generar Token
            String jwt = jwtService.generateToken(
                    loginRequest.email(),
                    loginRequest.tenant(),
                    usuario.getRol().name()
            );

            // 5. Respuesta exitosa
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "rol", usuario.getRol().name(),
                    "email", usuario.getEmail(),
                    "tenant", loginRequest.tenant()
            ));

        } catch (BadCredentialsException e) {
            // Si la contraseña es incorrecta, devolvemos 401 en lugar de 500
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email o contraseña incorrectos"));
        } catch (AuthenticationException e) {
            // Cualquier otro error de Spring Security
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autorizado: " + e.getMessage()));
        } catch (Exception e) {
            // Si explota por base de datos o nulos, evitamos el 500 genérico y mostramos la causa real
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
}