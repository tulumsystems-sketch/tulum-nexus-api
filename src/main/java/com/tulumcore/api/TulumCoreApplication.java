package com.tulumcore.api;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
            try {
                // Seteamos el tenant ANTES de cualquier operación con la DB
                TenantContext.setCurrentTenant("demo");

                if (repo.findByEmail("admin@demo.com").isEmpty()) {
                    Usuario u = new Usuario();
                    u.setEmail("admin@demo.com");
                    u.setPassword(passwordEncoder.encode("12345678"));
                    u.setRol(Rol.ADMIN);
                    u.setTenantId("demo");
                    repo.save(u);
                    System.out.println(">>> Usuario ADMIN seed creado: admin@demo.com / tenant: demo");
                }
            } finally {
                // Siempre limpiamos el contexto al terminar
                TenantContext.clear();
            }
        };
    }
}