package com.juniverse.mongo.dto;

import java.math.BigDecimal;

/**
 * Resultado de la agregación por categoría.
 *
 * @param category categoría (viene del campo {@code _id} del grupo)
 * @param products número de productos
 * @param totalStock suma de existencias
 * @param averagePrice precio promedio
 */
public record CategoryStats(String category, long products, long totalStock, BigDecimal averagePrice) {
}
