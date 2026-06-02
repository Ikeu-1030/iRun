package com.ikeu.server.aspect;

import com.ikeu.common.context.BaseContext;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.entity.OperationLog;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.OperationLogService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 操作日志切面，拦截标注 @OperationLog 的管理端方法，自动记录操作日志。
 * @author ikeu
 * @since 2026/05/31
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final AdminMapper adminMapper;

    /** 需要在操作日志中脱敏的字段名 */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldPassword", "newPassword",
            "loginPassword", "payPassword", "newPayPassword", "oldPayPassword",
            "code", "refreshToken"
    );

    /** 敏感字段替换值 */
    private static final String MASK_VALUE = "***";

    @AfterReturning(pointcut = "@annotation(anno)", returning = "ret")
    public void logOperation(JoinPoint joinPoint, Object ret, com.ikeu.server.annotation.OperationLog anno) {
        try {
            Long adminId = BaseContext.getCurrentId();
            if (adminId == null) return;

            Admin admin = adminMapper.selectById(adminId);
            if (admin == null) return;

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            String description = anno.description();
            if (paramNames != null && description != null && !description.isEmpty()) {
                for (int i = 0; i < paramNames.length; i++) {
                    if (args[i] != null) {
                        description = description.replace("#" + paramNames[i], args[i].toString());
                    }
                }
            }

            OperationLog log = new OperationLog();
            log.setAdminId(adminId);
            log.setAdminName(admin.getName());
            log.setModule(anno.module());
            log.setAction(anno.action());
            log.setDescription(description);

            // 提取请求参数（将 DTO 序列化为 JSON，过滤掉 MultipartFile 等不可序列化类型）
            List<Object> paramList = new ArrayList<>();
            if (args != null) {
                for (Object arg : args) {
                    if (arg != null && !(arg instanceof org.springframework.web.multipart.MultipartFile)
                            && !(arg instanceof HttpServletRequest)) {
                        try {
                            paramList.add(arg);
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (!paramList.isEmpty()) {
                try {
                    String json = JSONUtil.toJsonStr(paramList.size() == 1 ? paramList.get(0) : paramList);
                    log.setRequestParams(maskSensitiveFields(json));
                } catch (Exception ignored) {}
            }

            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setRequestMethod(request.getMethod());
                log.setRequestUrl(request.getRequestURI());
                log.setIp(getClientIp(request));
            }

            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    /** 对 JSON 字符串中的敏感字段值进行脱敏处理 */
    private String maskSensitiveFields(String json) {
        try {
            Object parsed = JSONUtil.parse(json);
            maskRecursive(parsed);
            return JSONUtil.toJsonStr(parsed);
        } catch (Exception e) {
            return json;
        }
    }

    @SuppressWarnings("unchecked")
    private void maskRecursive(Object obj) {
        if (obj instanceof JSONObject jsonObj) {
            for (String key : jsonObj.keySet()) {
                if (SENSITIVE_FIELDS.contains(key)) {
                    jsonObj.set(key, MASK_VALUE);
                } else {
                    maskRecursive(jsonObj.get(key));
                }
            }
        } else if (obj instanceof List<?> list) {
            for (Object item : list) {
                maskRecursive(item);
            }
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }
}
