package com.ray.a2a.model;

import java.util.Map;

/**
 * JSON-RPC 2.0 请求体。
 * 
 * 对应 A2A 协议 POST / 和 POST /stream 的请求外层结构。
 * params 里按 method 不同携带不同字段（tasks/send 带 history，tasks/get 只带 id）。
 */
public record JsonRpcRequest(
        String jsonrpc,      // 固定 "2.0"
        String method,       // tasks/send | tasks/get | tasks/cancel | tasks/sendSubscribe
        Map<String, Object> params,
        String id            // 客户端请求 ID，响应里原样带回
) {}
