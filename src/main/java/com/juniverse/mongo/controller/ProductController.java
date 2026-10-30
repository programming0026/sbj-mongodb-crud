package com.juniverse.mongo.controller;

import com.juniverse.mongo.dto.CategoryStats;
import com.juniverse.mongo.dto.ProductRequest;
import com.juniverse.mongo.dto.StockAdjustment;
import com.juniverse.mongo.model.Product;
import com.juniverse.mongo.service.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API REST del CRUD de productos en {@code /api/products}. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    /**
     * Crea el controlador.
     *
     * @param service servicio de productos
     */
    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * Lista productos con búsqueda, filtro por categoría, paginación y orden.
     *
     * @param q texto a buscar en nombre o descripción
     * @param category categoría
     * @param page página (desde 0)
     * @param size tamaño de página (máximo 100)
     * @param sort campo de orden
     * @param desc {@code true} para orden descendente
     * @return página de productos
     */
    @GetMapping
    public Page<Product> list(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "false") boolean desc) {
        Sort order = Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, sort);
        return service.search(q, category, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), order));
    }

    /**
     * Obtiene un producto.
     *
     * @param id identificador
     * @return el producto
     */
    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return service.get(id);
    }

    /**
     * Productos con una etiqueta.
     *
     * @param tag etiqueta
     * @return lista de productos
     */
    @GetMapping("/by-tag/{tag}")
    public List<Product> byTag(@PathVariable String tag) {
        return service.findByTag(tag);
    }

    /**
     * Estadísticas por categoría (agregación).
     *
     * @return resumen por categoría
     */
    @GetMapping("/stats")
    public List<CategoryStats> stats() {
        return service.statsByCategory();
    }

    /**
     * Crea un producto.
     *
     * @param request datos validados
     * @return 201 con el producto y la cabecera Location
     */
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product created = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    /**
     * Reemplaza un producto.
     *
     * @param id identificador
     * @param request datos validados
     * @return el producto actualizado
     */
    @PutMapping("/{id}")
    public Product replace(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return service.replace(id, request);
    }

    /**
     * Ajusta las existencias de forma atómica.
     *
     * @param id identificador
     * @param adjustment cantidad a sumar (negativa para una salida)
     * @return el producto con el stock nuevo
     */
    @PatchMapping("/{id}/stock")
    public Product adjustStock(@PathVariable String id, @Valid @RequestBody StockAdjustment adjustment) {
        return service.adjustStock(id, adjustment.delta());
    }

    /**
     * Elimina un producto.
     *
     * @param id identificador
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
