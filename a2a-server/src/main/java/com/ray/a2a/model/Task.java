package com.ray.a2a.model;

import java.util.List;
import java.util.Map;

/**
 * Task：A2A 协议的核心任务对象。
 * 
 * 一个 Task 对应一次"用户请求 → Agent 处理"的完整生命周期，
 * 包含状态、对话历史、产出物（artifacts）和元数据。
 */
public record Task(
        String id,                       // 任务唯一 ID
        String sessionId,                 // 会话 ID（同一会话的多轮任务共享）
        TaskStatus status,                // 当前状态（state + message + timestamp）
        List<Message> history,             // 对话历史
        List<Artifact> artifacts,          // 产出物（报告、数据等）
        Map<String, Object> metadata      // 扩展元数据
) {}
