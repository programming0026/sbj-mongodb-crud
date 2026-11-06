package com.juniverse.mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * MongoDB para las pruebas de integración.
 *
 * <p>Si la variable de entorno {@code MONGODB_URI} está definida (en GitHub Actions apunta al servicio
 * {@code mongodb} del workflow), las pruebas usan esa base y no se crea ningún contenedor. Si no, Testcontainers
 * levanta MongoDB 8.0 y {@code @ServiceConnection} configura la conexión de Spring hacia él.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestConfiguration {

    /**
     * Contenedor de MongoDB; solo se crea cuando no hay {@code MONGODB_URI}.
     *
     * @return el contenedor, que Spring arranca y detiene con el contexto
     */
    @Bean
    @ServiceConnection
    @ConditionalOnExpression("'${MONGODB_URI:}'.isEmpty()")
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer("mongo:8.0");
    }
}
