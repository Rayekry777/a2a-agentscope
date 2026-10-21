package com.ray.a2a.model;

/**
 * TaskStatus：任务状态快照。
 *
 * @param state     状态枚举
 * @param message   最近一条状态消息（Agent 的自然语言说明，可为 null）
 * @param timestamp ISO-8601 时间戳
 */
public record TaskStatus(
        TaskState state,
        Message message,
        String timestamp
) {}
