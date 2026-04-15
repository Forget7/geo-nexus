package com.geonexus.api.validation;

/**
 * 验证异常
 */
public class ValidationException extends RuntimeException {
    private final String errors;
    
    public ValidationException(String errors) {
        super("Validation failed: " + errors);
        this.errors = errors;
    }
    
    public String getErrors() {
        return errors;
    }
}
