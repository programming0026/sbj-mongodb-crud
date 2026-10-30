package com.juniverse.mongo.exception;

/** No hay existencias suficientes para la salida pedida (409). */
public class InsufficientStockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id producto
     * @param delta salida solicitada (negativa)
     */
    public InsufficientStockException(String id, int delta) {
        super("Stock insuficiente en el producto " + id + " para una salida de " + (-delta));
    }
}
