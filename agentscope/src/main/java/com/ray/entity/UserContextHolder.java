// 用 ThreadLocal 存储当前请求的用户上下文，无需额外的 Service 类
package com.ray.entity;

public class UserContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void set(String context) {
        CONTEXT.set(context);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}