package com.juniverse.mongo;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.juniverse.mongo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Prueba de integración contra una MongoDB real que levanta Testcontainers (requiere Docker).
 * {@code @ServiceConnection} configura la conexión de Spring hacia el contenedor automáticamente.
 */
@SpringBootTest(properties = "app.seed=false")
@Import(MongoTestConfiguration.class)
@DisplayName("CRUD de productos con MongoDB")
class ProductApiIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static String json(String sku, String name, String category, String price, int stock) {
        return """
                {"sku":"%s","name":"%s","description":"descripción de %s","category":"%s","price":%s,"stock":%d,"tags":["oferta","nuevo"]}
                """.formatted(sku, name, name, category, price, stock);
    }

    private String create(String sku, String name, String category, String price, int stock) throws Exception {
        String body = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(sku, name, category, price, stock)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"([a-f0-9]{24})\".*", "$1");
    }

    @Test
    @DisplayName("crear devuelve 201, Location, id de MongoDB y fechas de auditoría")
    void create() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json("LAP-001", "Laptop", "computo", "15999.00", 5)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/products/")))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("leer por id y 404 si no existe")
    void read() throws Exception {
        String id = create("LAP-001", "Laptop", "computo", "100.50", 5);
        mvc.perform(get("/api/products/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(100.50));
        mvc.perform(get("/api/products/000000000000000000000000")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SKU repetido -> 409 (aplicación) y datos inválidos -> 400")
    void duplicateAndValidation() throws Exception {
        create("LAP-001", "Laptop", "computo", "100", 5);
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json("LAP-001", "Otra", "computo", "10", 1)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json("mal sku", "", "computo", "-1", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    @DisplayName("el índice único de MongoDB también protege el SKU")
    void uniqueIndex() {
        repository.save(new com.juniverse.mongo.model.Product("DUP-1", "A", null, "x", java.math.BigDecimal.ONE, 1, null));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> repository.save(new com.juniverse.mongo.model.Product("DUP-1", "B", null, "x", java.math.BigDecimal.ONE, 1, null)));
    }

    @Test
    @DisplayName("PUT reemplaza y sube la versión; DELETE elimina")
    void updateAndDelete() throws Exception {
        String id = create("LAP-001", "Laptop", "computo", "100", 5);
        mvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content(json("LAP-001", "Laptop Pro", "computo", "200", 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("búsqueda por texto, categoría y etiqueta, con paginación")
    void search() throws Exception {
        create("LAP-001", "Laptop Gamer", "computo", "100", 5);
        create("MOU-001", "Mouse", "accesorios", "10", 50);
        create("MOU-002", "Mouse Pro", "accesorios", "20", 20);
        mvc.perform(get("/api/products").param("q", "mouse")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/products").param("category", "ACCESORIOS").param("size", "1")).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/products/by-tag/oferta")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/products").param("sort", "price").param("desc", "true")).andExpect(jsonPath("$.content[0].sku").value("LAP-001"));
    }

    @Test
    @DisplayName("ajuste de stock atómico: entradas, salidas y 409 si no alcanza")
    void stock() throws Exception {
        String id = create("LAP-001", "Laptop", "computo", "100", 5);
        mvc.perform(patch("/api/products/" + id + "/stock").contentType(MediaType.APPLICATION_JSON).content("{\"delta\": 10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(15));
        mvc.perform(patch("/api/products/" + id + "/stock").contentType(MediaType.APPLICATION_JSON).content("{\"delta\": -15}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(0));
        mvc.perform(patch("/api/products/" + id + "/stock").contentType(MediaType.APPLICATION_JSON).content("{\"delta\": -1}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/products/000000000000000000000000/stock").contentType(MediaType.APPLICATION_JSON).content("{\"delta\": -1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("estadísticas por categoría con una agregación")
    void stats() throws Exception {
        create("LAP-001", "Laptop", "computo", "100.00", 5);
        create("MON-001", "Monitor", "computo", "50.00", 3);
        create("MOU-001", "Mouse", "accesorios", "10.00", 50);
        mvc.perform(get("/api/products/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("accesorios"))
                .andExpect(jsonPath("$[1].category").value("computo"))
                .andExpect(jsonPath("$[1].products").value(2))
                .andExpect(jsonPath("$[1].totalStock").value(8))
                .andExpect(jsonPath("$[1].averagePrice").value(75.0));
    }
}
