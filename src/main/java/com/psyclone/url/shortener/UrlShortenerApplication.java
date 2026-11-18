package com.psyclone.url.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }

    @Bean
    public FlywayMigrationStrategy migrationStrategy() {
        return flyway -> {
            System.out.println(
                ">>> FLYWAY BEAN IS ALIVE: " + flyway.info().current()
            );
            flyway.migrate();
        };
    }
}