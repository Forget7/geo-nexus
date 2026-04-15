package com.geonexus.common.exception;

/**
 * 空间索引异常
 * 用于 SpatialIndexService
 */
public class SpatialIndexException extends RuntimeException {
    
    private final String errorCode;
    
    public SpatialIndexException(String message) {
        super(message);
        this.errorCode = "SPATIAL_IDX_ERR";
    }
    
    public SpatialIndexException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SPATIAL_IDX_ERR";
    }
    
    public SpatialIndexException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public SpatialIndexException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
