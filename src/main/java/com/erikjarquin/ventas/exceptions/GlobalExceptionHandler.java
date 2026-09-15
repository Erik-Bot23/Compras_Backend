package com.erikjarquin.ventas.exceptions;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

/**
 * Manejador GLOBAL de excepciones (@RestControllerAdvice).
 * Centraliza la conversión de cada error en una respuesta JSON uniforme:
 *
 * <pre>
 * { "timestamp": ..., "status": ..., "code": "...", "message": "..." }
 * </pre>
 *
 * Reglas aplicadas:
 *  - Los mensajes internos/reales NUNCA se filtran al cliente en errores 500
 *    (se loguean en el servidor y se responde un mensaje genérico).
 *  - Los errores de validación de {@code @Valid} devuelven 400 con los detalles.
 *  - Errores de Spring Security (401/403) tienen su propio handler para que
 *    el frontend siempre reciba el mismo formato de error.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------- Excepciones de negocio del proyecto -------------------

    /**
     * Caja: errores de apertura/cierre/consulta de caja.
     * Nota: se mapea con el estado que traiga la excepción (por defecto 404).
     */
    @ExceptionHandler(CashException.class)
    public ResponseEntity<ErrorResponse> handleCashException(CashException ex) {
        return buildErrorResponse(ex.getStatus(), "CASH_ERROR", ex.getMessage());
    }

    /**
     * Usuarios: errores de negocio del módulo de usuarios.
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        return buildErrorResponse(ex.getStatus(), "USER_ERROR", ex.getMessage());
    }

    /**
     * Roles: errores de negocio del módulo de roles (no encontrado, nombre
     * duplicado, rol con usuarios asignados).
     */
    @ExceptionHandler(RoleException.class)
    public ResponseEntity<ErrorResponse> handleRoleException(RoleException ex) {
        log.warn("Error de rol: {}", ex.getMessage());
        return buildErrorResponse(ex.getStatus(), "ROLE_ERROR", ex.getMessage());
    }

    /**
     * Categorías: errores de negocio del módulo de categorías (no encontrada,
     * nombre duplicado, categoría con productos asociados).
     */
    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ErrorResponse> handleCategoryException(CategoryException ex) {
        log.warn("Error de categoría: {}", ex.getMessage());
        return buildErrorResponse(ex.getStatus(), "CATEGORY_ERROR", ex.getMessage());
    }

    /**
     * Proveedores: errores de negocio del módulo de proveedores (no encontrado,
     * RFC duplicado, proveedor con compras asociadas).
     */
    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ErrorResponse> handleProviderException(ProviderException ex) {
        log.warn("Error de proveedor: {}", ex.getMessage());
        return buildErrorResponse(ex.getStatus(), "PROVIDER_ERROR", ex.getMessage());
    }

    /**
     * Compras: errores de negocio del módulo de compras (no encontrada, items
     * inválidos, proveedor inexistente, inconsistencia al cancelar).
     */
    @ExceptionHandler(PurchaseException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseException(PurchaseException ex) {
        log.warn("Error de compra: {}", ex.getMessage());
        return buildErrorResponse(ex.getStatus(), "PURCHASE_ERROR", ex.getMessage());
    }

    /**
     * Ventas: errores de negocio al procesar una venta (400).
     */
    @ExceptionHandler(SaleException.class)
    public ResponseEntity<ErrorResponse> handleSaleException(SaleException e) {
        log.warn("Error de venta: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "SALE_ERROR", e.getMessage());
    }

    /**
     * Pagos: se intenta deducir el código HTTP según el tipo de fallo
     * (timeout, problema de terminal o rechazo). También respeta el estado
     * que venga explícito en la excepción.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException e) {
        log.warn("Error de pago: {}", e.getMessage());

        if (e.getStatus() != null) {
            return buildErrorResponse(e.getStatus(), "PAYMENT_ERROR", e.getMessage());
        }

        if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("TIMEOUT"))) {
            return buildErrorResponse(HttpStatus.REQUEST_TIMEOUT, "PAYMENT_TIMEOUT", e.getMessage());
        }

        if (e.getMessage() != null && (e.getMessage().contains("comunicación") || e.getMessage().contains("terminal"))) {
            return buildErrorResponse(HttpStatus.BAD_GATEWAY, "TERMINAL_ERROR", e.getMessage());
        }

        if (e.getMessage() != null && e.getMessage().contains("rechazado")) {
            return buildErrorResponse(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_REJECTED", e.getMessage());
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", e.getMessage());
    }

    // ------------------- Seguridad (401 / 403) -------------------

    /**
     * Usuario autenticado sin el permiso requerido → 403 FORBIDDEN.
     * Se dispara cuando un @PreAuthorize no se cumple (o AccessDeniedException
     * llega hasta aquí). Confirma que el JSON de error es uniforme para el front.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tienes permisos para realizar esta acción");
    }

    /**
     * Request sin autenticación válida → 401 UNAUTHORIZED.
     * (BadCredentialsException, AuthenticationCredentialsNotFoundException, etc.)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No autenticado");
    }

    // ------------------- Errores de validación y de request -------------------

    /**
     * Falla la validación de un DTO con anotaciones tipo @NotBlank/@Size/@Min → 400.
     * Devuelve la lista de campos con su mensaje para que el frontend pueda
     * mostrarlos por campo. Antes caía al 500 genérico.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                details.isEmpty() ? "Datos de entrada inválidos" : details);
    }

    /**
     * Parámetro de path/query con tipo incorrecto, p. ej. /api/sales/{id} no
     * numérico → 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "El parámetro '" + ex.getName() + "' no tiene el tipo esperado");
    }

    /**
     * JSON mal formado o sin el body esperado → 400 (no un 500 interno).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Cuerpo de la petición inválido");
    }

    /**
     * Entrada del usuario inválida que no es de negocio (p. ej. archivo de imagen
     * con formato no permitido, lanzada por FileStorageService) → 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    // ------------------- Error genérico -------------------

    /**
     * Cualquier excepción no contemplada → 500 con mensaje genérico.
     * El detalle real se loguea en el servidor (no se filtra al cliente).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Error interno del servidor", e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno en el servidor");
    }

    // ------------------- Helpers -------------------

    /**
     * Construye la respuesta JSON de error con el mismo formato en todos los casos.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), status.value(), code, message);
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Formato estándar de respuesta de error que consume el frontend.
     */
    public static class ErrorResponse {
        private final LocalDateTime timestamp;
        private final int status;
        private final String code;
        private final String message;

        public ErrorResponse(LocalDateTime timestamp, int status, String code, String message) {
            this.timestamp = timestamp;
            this.status = status;
            this.code = code;
            this.message = message;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}