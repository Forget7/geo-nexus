package com.geonexus.config;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TracingAspect {

    private final Tracer tracer;

    public TracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Pointcut("execution(* com.geonexus.api.v1..*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object traceController(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String className = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();

        Span span = tracer.spanBuilder(className + "." + methodName)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("class", className);
            span.setAttribute("method", methodName);
            Object result = pjp.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    @Pointcut("execution(* com.geonexus.service..*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object traceService(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String className = sig.getDeclaringType().getSimpleName();
        String methodName = sig.getName();

        Span span = tracer.spanBuilder(className + "." + methodName)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Object result = pjp.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }
}
