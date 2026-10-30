package com.juniverse.mongo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Datos para crear o reemplazar un producto.
 *
 * @param sku código único (letras mayúsculas, dígitos y guiones)
 * @param name nombre
 * @param description descripción opcional
 * @param category categoría
 * @param price precio
 * @param stock existencias
 * @param tags etiquetas opcionales
 */
public record ProductRequest(
        @NotBlank(message = "El SKU es obligatorio")
        @Pattern(regexp = "[A-Z0-9-]{3,20}", message = "El SKU solo admite mayúsculas, dígitos y guiones (3 a 20 caracteres)")
        String sku,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String name,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String description,
        @NotBlank(message = "La categoría es obligatoria")
        String category,
        @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal price,
        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock,
        List<@NotBlank(message = "Las etiquetas no pueden estar vacías") String> tags) {
}
