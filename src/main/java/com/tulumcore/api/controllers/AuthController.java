package com.tulumcore.api.controllers;

import com.tulumcore.api.security.JwtService; // Asegurate de que la ruta sea correcta
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService; // <--- Inyectamos tu servicio real

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        // 1. Validamos credenciales (Email + Password)
        // Si fallan, Spring lanza una excepción y devuelve 401 automáticamente
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        // 2. ¡GENERAMOS EL TOKEN REAL!
        // Usamos el email y el tenant que vienen del formulario de React
        String jwt = jwtService.generateToken(loginRequest.email(), loginRequest.tenant());

        // 3. Enviamos el JWT real al frontend
        // Ahora sí, React va a guardar un token válido que el Filter podrá leer
        return ResponseEntity.ok(jwt);
    }
}