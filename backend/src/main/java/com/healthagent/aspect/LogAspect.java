package com.healthagent.aspect;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Pointcut("execution(* com.healthagent.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        Object[] args = joinPoint.getArgs();

        log.info("========== 请求开始 ==========");
        log.info("请求URI: {} {}", method, uri);
        log.info("类方法: {}.{}", className, methodName);
        log.info("请求参数: {}", JSON.toJSONString(filterArgs(args)));

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;
            log.info("响应结果: {}", JSON.toJSONString(result));
            log.info("耗时: {} ms", costTime);
            log.info("========== 请求结束 ==========");
        }
    }

    private Object filterArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .toArray();
    }
}
