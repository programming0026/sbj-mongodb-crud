package com.juniverse.mongo.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Documento de la colección {@code products}. A diferencia de una tabla relacional, los datos anidados (como la lista
 * de etiquetas) se guardan dentro del mismo documento.
 */
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    /** Código único: el índice {@code unique} lo garantiza la propia base de datos. */
    @Indexed(unique = true)
    private String sku;

    private String name;

    private String description;

    @Indexed
    private String category;

    private BigDecimal price;

    private int stock;

    @Field("tags")
    private List<String> tags = List.of();

    /** Control de concurrencia optimista: Spring Data lo incrementa en cada guardado. */
    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Constructor vacío requerido por el mapeo. */
    public Product() {
    }

    /**
     * Crea un producto nuevo (sin id: lo asigna MongoDB al guardar).
     *
     * @param sku código único
     * @param name nombre
     * @param description descripción
     * @param category categoría
     * @param price precio
     * @param stock existencias
     * @param tags etiquetas
     */
    public Product(String sku, String name, String description, String category, BigDecimal price, int stock, List<String> tags) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.tags = tags == null ? List.of() : tags;
    }

    public String getId() { return id; }

    public String getSku() { return sku; }

    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }

    public int getStock() { return stock; }

    public void setStock(int stock) { this.stock = stock; }

    public List<String> getTags() { return tags; }

    public void setTags(List<String> tags) { this.tags = tags == null ? List.of() : tags; }

    public Long getVersion() { return version; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
