package com.geonexus.config;

/**
 * 简单 ThreadLocal 上下文持有者
 * 用于在请求范围内传递当前用户 ID
 * 
 * 使用方式：
 * ChatContextHolder.setCurrentUser(userId);
 * // ... 业务代码 ...
 * ChatContextHolder.clear();
 * 
 * 建议在 Controller 或 Filter 中设置和清理
 */
public class ChatContextHolder {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(String userId) {
        currentUser.set(userId);
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
