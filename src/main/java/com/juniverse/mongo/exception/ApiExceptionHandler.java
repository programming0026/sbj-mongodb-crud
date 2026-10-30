package com.juniverse.mongo.exception;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduce las excepciones a respuestas {@code application/problem+json} (RFC 9457). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 404.
     *
     * @param e excepción
     * @return problema
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail notFound(ProductNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Producto no encontrado", e.getMessage());
    }

    /**
     * 409 por SKU repetido (comprobado por la aplicación o por el índice único de MongoDB).
     *
     * @param e excepción
     * @return problema
     */
    @ExceptionHandler({DuplicateSkuException.class, DuplicateKeyException.class})
    public ProblemDetail duplicate(RuntimeException e) {
        String detail = e instanceof DuplicateSkuException ? e.getMessage() : "Ya existe un producto con ese SKU";
        return problem(HttpStatus.CONFLICT, "Conflicto", detail);
    }

    /**
     * 409 por salida de stock mayor a las existencias.
     *
     * @param e excepción
     * @return problema
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail insufficientStock(InsufficientStockException e) {
        return problem(HttpStatus.CONFLICT, "Stock insuficiente", e.getMessage());
    }

    /**
     * 409 cuando otro cliente modificó el documento entre la lectura y el guardado (campo {@code @Version}).
     *
     * @param e excepción
     * @return problema
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail optimisticLock(OptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Modificación concurrente", "El producto fue modificado por otro cliente; vuelve a intentarlo");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList();
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Error de validación", "Uno o más campos no son válidos");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.juniverse.com/errors/" + status.value()));
        return problem;
    }
}
