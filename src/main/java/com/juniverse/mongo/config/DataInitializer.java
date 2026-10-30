package com.juniverse.mongo.config;

import com.juniverse.mongo.dto.ProductRequest;
import com.juniverse.mongo.repository.ProductRepository;
import com.juniverse.mongo.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Carga productos de ejemplo al arrancar si la colección está vacía (se desactiva con {@code app.seed=false}). */
@Configuration
public class DataInitializer {

    /**
     * Runner que inserta los datos de ejemplo.
     *
     * @param repository repositorio
     * @param service servicio
     * @return runner de arranque
     */
    @Bean
    @ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedProducts(ProductRepository repository, ProductService service) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            service.create(new ProductRequest("LAP-001", "Laptop 14 pulgadas", "Procesador de 8 núcleos, 16 GB de RAM", "computo", new BigDecimal("15999.00"), 12, List.of("oferta", "portatil")));
            service.create(new ProductRequest("MOU-001", "Mouse inalámbrico", "Mouse ergonómico recargable", "accesorios", new BigDecimal("349.90"), 80, List.of("oficina")));
            service.create(new ProductRequest("KEY-001", "Teclado mecánico", "Teclado con switches rojos", "accesorios", new BigDecimal("1299.00"), 35, List.of("oficina", "gaming")));
            service.create(new ProductRequest("MON-001", "Monitor 27 pulgadas", "Panel IPS 144 Hz", "computo", new BigDecimal("5499.00"), 9, List.of("gaming", "oferta")));
        };
    }
}
