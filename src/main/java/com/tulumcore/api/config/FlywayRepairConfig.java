package com.tulumcore.api.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neon ya tiene V2/V6 aplicadas; en la rama los archivos cambiaron de checksum.
 * repair() actualiza el historial sin re-ejecutar migraciones; después corre migrate (V7+).
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
