package com.juniverse.mongo.repository;

import com.juniverse.mongo.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de productos. {@link MongoRepository} ya trae el CRUD ({@code save}, {@code findById}, {@code findAll},
 * {@code deleteById}...); aquí se añaden consultas derivadas del nombre del método y una escrita con {@code @Query}.
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * Busca por SKU (consulta derivada del nombre).
     *
     * @param sku código
     * @return el producto, si existe
     */
    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Productos de una categoría, paginados.
     *
     * @param category categoría
     * @param pageable página y orden
     * @return página de productos
     */
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    /**
     * Productos que tienen una etiqueta (busca dentro del arreglo {@code tags}).
     *
     * @param tag etiqueta
     * @return productos con esa etiqueta
     */
    List<Product> findByTagsContaining(String tag);

    /**
     * Búsqueda por texto en nombre o descripción con expresión regular (sin distinguir mayúsculas), escrita como JSON de MongoDB.
     *
     * @param text fragmento de texto
     * @param pageable página y orden
     * @return página de productos
     */
    @Query("{ $or: [ { name: { $regex: ?0, $options: 'i' } }, { description: { $regex: ?0, $options: 'i' } } ] }")
    Page<Product> searchText(String text, Pageable pageable);
}
