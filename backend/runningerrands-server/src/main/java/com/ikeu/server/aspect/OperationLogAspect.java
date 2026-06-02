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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** 状态码→中文映射（参数名 → 值→标签） */
    private static final Map<String, Map<Integer, String>> STATUS_LABELS = Map.of(
            "isCertify", Map.of(0, "未认证", 1, "审核中", 2, "已认证", 3, "驳回"),
            "verifyStatus", Map.of(0, "未认证", 1, "审核中", 2, "已认证", 3, "驳回"),
            "status", Map.of(1, "待接单", 2, "已接单", 3, "配送中", 4, "待确认", 5, "已完成", 6, "已取消"),
            "orderStatus", Map.of(1, "待取货", 2, "配送中", 3, "待确认", 4, "已完成", 5, "已取消"),
            "enabled", Map.of(0, "禁用", 1, "启用")
    );

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
                        String placeholder = "#" + paramNames[i];
                        // 先处理 #param.field 语法
                        Pattern p = Pattern.compile(Pattern.quote(placeholder) + "\\.(\\w+)");
                        Matcher m = p.matcher(description);
                        StringBuffer sb = new StringBuffer();
                        while (m.find()) {
                            String fieldName = m.group(1);
                            m.appendReplacement(sb, Matcher.quoteReplacement(getFieldValue(args[i], fieldName)));
                        }
                        m.appendTail(sb);
                        description = sb.toString();
                        // 再处理裸 #paramName（替换为 toString，但过滤 DTO 类名前缀）
                        String strValue = args[i].toString();
                        // 整型参数尝试映射为中文标签
                        if (args[i] instanceof Number) {
                            String label = getStatusLabel(paramNames[i], ((Number) args[i]).intValue());
                            if (label != null) strValue = label;
                        }
                        // DTO 的 toString() 形如 "ClassName(field=value, ...)"，截取括号内容
                        if (strValue.contains("(") && strValue.endsWith(")")) {
                            strValue = strValue.substring(strValue.indexOf('(') + 1, strValue.length() - 1);
                        }
                        description = description.replace(placeholder, strValue);
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

    private String getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value != null ? value.toString() : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String getStatusLabel(String paramName, int value) {
        Map<Integer, String> labels = STATUS_LABELS.get(paramName);
        return labels != null ? labels.get(value) : null;
    }

    private Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); } catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
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
