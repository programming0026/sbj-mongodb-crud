package com.juniverse.mongo.service;

import com.juniverse.mongo.dto.CategoryStats;
import com.juniverse.mongo.dto.ProductRequest;
import com.juniverse.mongo.exception.DuplicateSkuException;
import com.juniverse.mongo.exception.InsufficientStockException;
import com.juniverse.mongo.exception.ProductNotFoundException;
import com.juniverse.mongo.model.Product;
import com.juniverse.mongo.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Lógica del CRUD. Combina el repositorio (operaciones simples) con {@link MongoTemplate} para lo que un repositorio
 * no expresa bien: actualizaciones atómicas con {@code $inc} y agregaciones.
 */
@Service
public class ProductService {

    private final ProductRepository repository;
    private final MongoTemplate mongo;

    /**
     * Crea el servicio.
     *
     * @param repository repositorio de productos
     * @param mongo plantilla de MongoDB para operaciones avanzadas
     */
    public ProductService(ProductRepository repository, MongoTemplate mongo) {
        this.repository = repository;
        this.mongo = mongo;
    }

    /**
     * Lista productos con filtros opcionales: texto o categoría (el texto tiene prioridad).
     *
     * @param text texto a buscar en nombre o descripción, o {@code null}
     * @param category categoría, o {@code null}
     * @param pageable página y orden
     * @return página de productos
     */
    public Page<Product> search(String text, String category, Pageable pageable) {
        if (text != null && !text.isBlank()) {
            return repository.searchText(Pattern.quote(text.trim()), pageable);
        }
        if (category != null && !category.isBlank()) {
            return repository.findByCategoryIgnoreCase(category.trim(), pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Obtiene un producto.
     *
     * @param id identificador (ObjectId en texto)
     * @return el producto
     * @throws ProductNotFoundException si no existe
     */
    public Product get(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Productos con una etiqueta.
     *
     * @param tag etiqueta
     * @return lista de productos
     */
    public List<Product> findByTag(String tag) {
        return repository.findByTagsContaining(tag);
    }

    /**
     * Crea un producto.
     *
     * @param request datos validados
     * @return producto guardado, con su id generado
     * @throws DuplicateSkuException si el SKU ya existe
     */
    public Product create(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        return repository.save(new Product(request.sku(), request.name().trim(), request.description(), request.category().trim(),
                request.price(), request.stock(), request.tags()));
    }

    /**
     * Reemplaza los datos de un producto. Si otro cliente lo modificó mientras tanto, falla el control de versión.
     *
     * @param id identificador
     * @param request datos validados
     * @return producto actualizado
     * @throws ProductNotFoundException si no existe
     * @throws DuplicateSkuException si el SKU pertenece a otro producto
     */
    public Product replace(String id, ProductRequest request) {
        Product product = get(id);
        repository.findBySku(request.sku()).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new DuplicateSkuException(request.sku());
        });
        product.setSku(request.sku());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setTags(request.tags());
        return repository.save(product);
    }

    /**
     * Elimina un producto.
     *
     * @param id identificador
     * @throws ProductNotFoundException si no existe
     */
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }

    /**
     * Suma o resta existencias de forma <b>atómica</b> en el servidor ({@code $inc}), sin leer y reescribir el documento.
     * Para las salidas la condición {@code stock >= -delta} va en el mismo filtro, así dos peticiones simultáneas
     * nunca dejan el stock en negativo.
     *
     * @param id identificador
     * @param delta cantidad a sumar (negativa para una salida)
     * @return producto con el stock ya actualizado
     * @throws ProductNotFoundException si no existe
     * @throws InsufficientStockException si la salida supera las existencias
     */
    public Product adjustStock(String id, int delta) {
        Criteria criteria = Criteria.where("_id").is(id);
        if (delta < 0) {
            criteria = criteria.and("stock").gte(-delta);
        }
        Product updated = mongo.findAndModify(Query.query(criteria), new Update().inc("stock", delta),
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (updated == null) {
            get(id); // si no existe lanza 404; si existe es porque no alcanzaba el stock
            throw new InsufficientStockException(id, delta);
        }
        return updated;
    }

    /**
     * Estadísticas por categoría con una agregación ({@code $group}).
     *
     * @return una fila por categoría, ordenadas por nombre
     */
    public List<CategoryStats> statsByCategory() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("category").count().as("products").sum("stock").as("totalStock").avg("price").as("averagePrice"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "_id"));
        return mongo.aggregate(aggregation, "products", Document.class).getMappedResults().stream()
                .map(d -> new CategoryStats(d.getString("_id"), ((Number) d.get("products")).longValue(),
                        ((Number) d.get("totalStock")).longValue(), toBigDecimal(d.get("averagePrice"))))
                .toList();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof Decimal128 d) {
            return d.bigDecimalValue().setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
