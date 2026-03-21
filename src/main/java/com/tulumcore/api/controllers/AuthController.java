package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.UsuarioRepository;
import com.tulumcore.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        Usuario usuario = usuarioRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String jwt = jwtService.generateToken(
                loginRequest.email(),
                loginRequest.tenant(),
                usuario.getRol().name()
        );

        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "rol", usuario.getRol().name(),
                "email", usuario.getEmail()
        ));
    }

}