package com.geonexus.annotation;

import java.lang.annotation.*;

/**
 * 审计注解 - 标注需要审计的方法
 *
 * 使用方式：
 * @Audited(action = "CREATE", resourceType = "GIS_DATA")
 * public void createData(GISDataEntity data) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    /** 操作类型：CREATE, UPDATE, DELETE, READ, EXPORT */
    String action();

    /** 资源类型 */
    String resourceType() default "";

    /** 是否记录请求参数 */
    boolean logParams() default true;

    /** 是否记录返回值 */
    boolean logResult() default false;
}
