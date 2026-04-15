package com.geonexus.common.exception;

/**
 * 几何处理异常
 * 用于 GeometryCalcService, SpatialAnalysisService, GeometryProcessService, ProjectionService, GeometryValidationService
 */
public class GeometryProcessException extends RuntimeException {
    
    private final String errorCode;
    
    public GeometryProcessException(String message) {
        super(message);
        this.errorCode = "GEOM_ERR";
    }
    
    public GeometryProcessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GEOM_ERR";
    }
    
    public GeometryProcessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public GeometryProcessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
