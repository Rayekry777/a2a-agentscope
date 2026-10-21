package com.ray.a2a.model;

import java.util.List;

/**
 * Message：一条对话消息。
 *
 * @param role  "user"（用户）或 "agent"（Agent）
 * @param parts 消息内容分片（一段消息可以同时包含文本和结构化数据）
 */
public record Message(
        String role,
        List<Part> parts
) {}
