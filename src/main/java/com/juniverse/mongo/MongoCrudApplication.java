package com.juniverse.mongo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * CRUD de productos sobre MongoDB con Spring Data MongoDB.
 * {@code @EnableMongoAuditing} activa el llenado automático de {@code createdAt} y {@code updatedAt}.
 */
@SpringBootApplication
@EnableMongoAuditing
public class MongoCrudApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(MongoCrudApplication.class, args);
    }
}
