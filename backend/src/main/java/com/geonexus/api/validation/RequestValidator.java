package com.geonexus.api.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 请求验证器 - 使用Bean Validation
 */
@Slf4j
@Component
public class RequestValidator {
    
    private final Validator validator;
    
    public RequestValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * 验证对象
     */
    public <T> ValidationResult validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (violations.isEmpty()) {
            return ValidationResult.success();
        }
        
        String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("Validation failed: {}", errors);
        
        return ValidationResult.failure(errors);
    }
    
    /**
     * 验证对象并抛出异常
     */
    public <T> void validateAndThrow(T object) {
        ValidationResult result = validate(object);
        if (!result.isValid()) {
            throw new ValidationException(result.getErrors());
        }
    }
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errors;
        
        private ValidationResult(boolean valid, String errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrors() {
            return errors;
        }
    }
    
    /**
     * 验证异常
     */
    public static class ValidationException extends RuntimeException {
        private final String errors;
        
        public ValidationException(String errors) {
            super("Validation failed: " + errors);
            this.errors = errors;
        }
        
        public String getErrors() {
            return errors;
        }
    }
    
}
