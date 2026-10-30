package com.juniverse.mongo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Ajuste de existencias: positivo para entradas, negativo para salidas.
 *
 * @param delta cantidad a sumar (puede ser negativa)
 */
public record StockAdjustment(@NotNull(message = "delta es obligatorio") Integer delta) {
}
