package com.ray.a2a.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON-RPC 2.0 响应体。
 * 
 * result 和 error 互斥：成功时 error=null，失败时 result=null。
 * {@code @JsonInclude(NON_NULL)} 让 Jackson 序列化时忽略 null 字段，避免输出 "error": null。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        Object result,        // 成功时携带 Task 对象
        JsonRpcError error,   // 失败时携带错误码和消息
        String id
) {
    /** 成功响应工厂方法 */
    public static JsonRpcResponse success(Object result, String id) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    /** 错误响应工厂方法（JSON-RPC 标准错误码：-32601 方法不存在，-32602 参数错误，-32603 内部错误） */
    public static JsonRpcResponse error(int code, String message, String id) {
        return new JsonRpcResponse("2.0", null, new JsonRpcError(code, message), id);
    }
}
