package com.ray.a2a.model;

/**
 * JSON-RPC 错误对象。
 * 
 * code 语义参考 JSON-RPC 2.0 规范：
 * 
 *   - -32601: Method not found *   - -32602: Invalid params *   - -32603: Internal error * 
 */
public record JsonRpcError(int code, String message) {}
