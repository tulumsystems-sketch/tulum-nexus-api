package com.tulumcore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class TulumCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(TulumCoreApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (repo.findByEmail("usuario@empresa.com").isEmpty()) {
                Usuario u = new Usuario();
                u.setEmail("usuario@empresa.com");
                u.setPassword(passwordEncoder.encode("12345678"));
                repo.save(u);
                System.out.println("Usuario de prueba creado: usuario@empresa.com / Pass: 12345678");
            }
        };

    }
}