package com.ikeu.common.context;

/**
 * ThreadLocal 上下文工具类，保存当前请求的用户/管理员 ID 和角色。
 *
 * <p>拦截器在请求开始时设值，afterCompletion 时清理，防止线程池泄漏。
 *
 * @author ikeu
 * @since 2026/06/22
 */
public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Integer> roleLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /** 设置当前管理员角色（仅管理端请求有效，用户端请求不设此值）。 */
    public static void setCurrentRole(Integer role) {
        roleLocal.set(role);
    }

    /** 获取当前管理员角色，可能为 null（用户端请求或未登录）。 */
    public static Integer getCurrentRole() {
        return roleLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        roleLocal.remove();
    }

}
