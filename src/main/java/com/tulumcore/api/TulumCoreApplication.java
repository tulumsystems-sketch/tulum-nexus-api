package com.tulumcore.api;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@SpringBootApplication
public class TulumCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(TulumCoreApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            UsuarioRepository repo,
            PasswordEncoder passwordEncoder,
            DataSource dataSource,
            @Value("${app.seed.super-admin.enabled:false}") boolean superAdminSeedEnabled,
            @Value("${app.seed.super-admin.email:}") String superAdminEmail,
            @Value("${app.seed.super-admin.password:}") String superAdminPassword,
            @Value("${app.seed.super-admin.tenant:superadmin}") String superAdminTenant) {
        return args -> {
            // TODO: migrar este DDL manual a Flyway/Liquibase antes de endurecer produccion.
            // Actualizar CHECK constraint del enum Rol (pasa de 'ADMIN','OPERADOR' a incluir 'SUPER_ADMIN')
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
                stmt.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('SUPER_ADMIN','ADMIN','OPERADOR'))");
                System.out.println(">>> CHECK constraint de rol actualizado");
            } catch (Exception e) {
                System.out.println(">>> [WARN] No se pudo actualizar constraint de rol: " + e.getMessage());
            }

            if (!superAdminSeedEnabled) {
                System.out.println(">>> Seed de Super Admin deshabilitado");
                return;
            }

            if (!StringUtils.hasText(superAdminEmail) || !StringUtils.hasText(superAdminPassword)) {
                System.out.println(">>> [WARN] Seed de Super Admin omitido: faltan SUPERADMIN_EMAIL o SUPERADMIN_PASSWORD");
                return;
            }

            try {
                TenantContext.setCurrentTenant(superAdminTenant);

                var opt = repo.findByEmail(superAdminEmail);
                if (opt.isEmpty()) {
                    Usuario u = new Usuario();
                    u.setEmail(superAdminEmail);
                    u.setPassword(passwordEncoder.encode(superAdminPassword));
                    u.setRol(Rol.SUPER_ADMIN);
                    u.setTenantId(superAdminTenant);
                    repo.save(u);
                    System.out.println(">>> Super Admin creado desde variables de entorno / Rol: SUPER_ADMIN");
                } else {
                    Usuario u = opt.get();
                    u.setRol(Rol.SUPER_ADMIN);
                    repo.save(u);
                    System.out.println(">>> Super Admin actualizado desde variables de entorno -> Rol: SUPER_ADMIN");
                }
            } finally {
                TenantContext.clear();
            }
        };
    }
}
