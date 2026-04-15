package com.geonexus.api.config;

import com.geonexus.config.ChatContextHolder;
import com.geonexus.domain.AuditLog;
import com.geonexus.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全局异常处理器 + 异常审计日志
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditLogRepository auditLogRepository;

    private void logExceptionAudit(HttpServletRequest request, Exception ex, int httpStatus, String action) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("uri", request.getRequestURI());
            details.put("method", request.getMethod());
            details.put("httpStatus", httpStatus);
            details.put("exception", ex.getClass().getSimpleName());
            details.put("errorMessage", ex.getMessage());
            details.put("userAgent", request.getHeader("User-Agent"));

            AuditLog auditLog = AuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(ChatContextHolder.getCurrentUser() != null
                            ? ChatContextHolder.getCurrentUser() : "anonymous")
                    .action(action)
                    .resourceType("EXCEPTION")
                    .resourceId(null)
                    .details(details)
                    .ipAddress(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("异常审计日志记录失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        logExceptionAudit(request, ex, 404, "READ");
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                          HttpServletRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        logExceptionAudit(request, ex, 400, "UPDATE");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);
        logExceptionAudit(request, ex, 422, "UPDATE");
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                     HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        logExceptionAudit(request, ex, 400, "UPDATE");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex,
                                                        HttpServletRequest request) {
        log.error("Unexpected error at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        logExceptionAudit(request, ex, 500, "READ");
        // Return generic message - never expose internal details to clients
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "An unexpected error occurred. Please try again later."));
    }

    /**
     * 错误响应体
     */
    public record ErrorResponse(
            int code,
            String message,
            Map<String, String> details,
            Instant timestamp
    ) {
        public static ErrorResponse of(int code, String message) {
            return new ErrorResponse(code, message, null, Instant.now());
        }

        public static ErrorResponse of(int code, String message, Map<String, String> details) {
            return new ErrorResponse(code, message, details, Instant.now());
        }
    }

    /**
     * 资源未找到异常
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * 业务异常
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
