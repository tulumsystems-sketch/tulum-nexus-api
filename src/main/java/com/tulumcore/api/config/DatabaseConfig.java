package com.tulumcore.api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String rawUsername;

    @Value("${spring.datasource.password:}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = StringUtils.hasText(rawUrl) ? rawUrl : properties.getUrl();
        String username = StringUtils.hasText(rawUsername) ? rawUsername : properties.getUsername();
        String password = StringUtils.hasText(rawPassword) ? rawPassword : properties.getPassword();

        if (StringUtils.hasText(url)) {
            // Manejo de URIs estilo Railway / Heroku (postgresql://user:pass@host:port/db o postgres://...)
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                try {
                    String cleanUriString = url;
                    if (cleanUriString.startsWith("postgres://")) {
                        cleanUriString = "postgresql://" + cleanUriString.substring("postgres://".length());
                    }
                    URI uri = new URI(cleanUriString);
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length > 0 && !StringUtils.hasText(username)) {
                            username = userInfo[0];
                        }
                        if (userInfo.length > 1 && !StringUtils.hasText(password)) {
                            password = userInfo[1];
                        }
                    }
                    int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                    String path = uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "postgres";
                    String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
                    url = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + path + query;
                } catch (Exception e) {
                    if (!url.startsWith("jdbc:")) {
                        url = "jdbc:" + url;
                    }
                }
            } else if (!url.startsWith("jdbc:")) {
                url = "jdbc:" + url;
            }
        }

        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}
