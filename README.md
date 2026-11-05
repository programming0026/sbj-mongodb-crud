# CRUD con Spring Boot 4 y MongoDB

Ejemplo con **Spring Boot 4** y **Java 21** de un CRUD de productos sobre **MongoDB** con Spring Data MongoDB. Además del CRUD básico muestra lo que distingue a una base documental: documentos con datos anidados, índices, actualizaciones atómicas y agregaciones.

## Qué muestra el ejemplo

| Tema | Dónde |
|------|-------|
| **Documento** `@Document` con `@Id` (ObjectId), arreglo de etiquetas anidado y `BigDecimal` guardado como **Decimal128** | `Product` |
| **Índices**: `@Indexed(unique = true)` en el SKU (la base impide duplicados) | `Product` |
| **Auditoría** automática (`@CreatedDate`, `@LastModifiedDate`) | `Product`, `MongoCrudApplication` |
| **Control de concurrencia optimista** con `@Version` (409 si otro cliente modificó el documento) | `Product`, `ApiExceptionHandler` |
| **MongoRepository**: CRUD listo, consultas derivadas (`findBySku`, `findByTagsContaining`), paginación y `@Query` con JSON | `ProductRepository` |
| **MongoTemplate**: actualización atómica `$inc` con `findAndModify` (stock nunca negativo, aun con peticiones simultáneas) | `ProductService.adjustStock` |
| **Aggregation Framework**: `$group` con conteo, suma y promedio por categoría | `ProductService.statsByCategory` |
| Validación (Bean Validation) y errores en formato Problem Details | `ProductRequest`, `ApiExceptionHandler` |
| Pruebas contra una **MongoDB real** con Testcontainers y `@ServiceConnection` | `ProductApiIT` |

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/products?q=&category=&page=&size=&sort=&desc=` | Lista paginada con búsqueda por texto o categoría |
| GET | `/api/products/{id}` | Obtiene un producto (el id es un ObjectId de 24 caracteres hexadecimales) |
| GET | `/api/products/by-tag/{tag}` | Productos con una etiqueta |
| GET | `/api/products/stats` | Estadísticas por categoría (agregación) |
| POST | `/api/products` | Crea (201 + `Location`) |
| PUT | `/api/products/{id}` | Reemplaza |
| PATCH | `/api/products/{id}/stock` | Suma o resta existencias de forma atómica: `{"delta": -3}` |
| DELETE | `/api/products/{id}` | Elimina (204) |

Códigos de error: 400 (datos inválidos), 404 (no existe), 409 (SKU repetido, stock insuficiente o modificación concurrente).

## Estructura

```text
src/main/java/com/juniverse/mongo/
├── MongoCrudApplication.java
├── model/Product.java                 # el documento
├── repository/ProductRepository.java  # MongoRepository + consultas
├── service/ProductService.java        # CRUD, $inc atómico y agregación (MongoTemplate)
├── controller/ProductController.java
├── dto/                               # ProductRequest, StockAdjustment, CategoryStats
├── exception/                         # excepciones y ApiExceptionHandler
└── config/DataInitializer.java        # 4 productos de ejemplo si la colección está vacía
```

## Requisitos

- JDK 21 y Maven 3.9 o superior.
- Docker (para levantar MongoDB y para las pruebas de integración).

## Ejecutar

1. Levanta MongoDB:

```bash
docker compose up -d
```

2. Arranca la aplicación (usa `mongodb://localhost:27017/catalog`; cámbialo con la variable `MONGODB_URI`):

```bash
mvn spring-boot:run
```

3. Prueba:

```bash
curl "http://localhost:8080/api/products?q=mouse"
curl "http://localhost:8080/api/products?category=computo&sort=price&desc=true"
curl http://localhost:8080/api/products/stats
curl -X POST http://localhost:8080/api/products -H "Content-Type: application/json" \
     -d '{"sku":"HDM-001","name":"Cable HDMI","category":"accesorios","price":149.00,"stock":40,"tags":["oferta"]}'
curl -X PATCH http://localhost:8080/api/products/<id>/stock -H "Content-Type: application/json" -d '{"delta":-5}'
```

También puedes inspeccionar la base: `docker exec -it sbj-mongodb mongosh catalog --eval "db.products.find()"`.

## Pruebas

```bash
mvn verify
```

`ProductApiIT` (8 pruebas) levanta un contenedor `mongo:8.0` con **Testcontainers**, de modo que no hace falta tener MongoDB instalado y cada ejecución parte de una base limpia. Cubre crear, leer, 404, validación, SKU duplicado (por la aplicación y por el índice único), actualizar con versión, borrar, búsquedas, ajuste atómico de stock y la agregación. Si Docker no está disponible las pruebas se omiten. La primera vez descarga la imagen de MongoDB y puede tardar.

## Buenas prácticas

- Modela según cómo consultas los datos: lo que se lee junto, se guarda junto (etiquetas dentro del documento).
- Para cambios numéricos concurrentes usa operadores atómicos (`$inc`) en vez de leer, modificar y guardar.
- Crea los índices que respalden tus consultas y restricciones de unicidad; en producción gestiónalos con migraciones en vez de `auto-index-creation`.
- Guarda dinero como `Decimal128` (`spring.mongodb.representation.big-decimal=decimal128`).
- Nunca construyas filtros concatenando texto del usuario: aquí el texto se escapa con `Pattern.quote` antes de usarlo en la expresión regular.

Fat jar ejecutable: `mvn clean package` y `java -jar target/sbj-mongodb-crud-0.0.1-SNAPSHOT.jar`.

## Licencia

Código de ejemplo de uso libre con fines educativos.
