package com.juniverse.mongo.exception;

/** Ya existe un producto con ese SKU (409). */
public class DuplicateSkuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param sku código repetido
     */
    public DuplicateSkuException(String sku) {
        super("Ya existe un producto con el SKU " + sku);
    }
}
