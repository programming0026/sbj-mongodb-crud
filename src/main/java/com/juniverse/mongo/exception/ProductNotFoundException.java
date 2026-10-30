package com.juniverse.mongo.exception;

/** El producto no existe (404). */
public class ProductNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param id identificador buscado
     */
    public ProductNotFoundException(String id) {
        super("No existe un producto con id " + id);
    }
}
