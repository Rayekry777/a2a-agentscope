package com.ray.a2a.model;

import java.util.List;

/**
 * Artifact：任务产出物。
 * 
 * Agent 完成任务后产出的结果（如报告、分析数据），
 * 通过 task_artifact_update 事件推送给 Client。
 *
 * @param name        产出物名称
 * @param description 描述
 * @param parts       产出内容分片（文本 + 结构化数据）
 */
public record Artifact(
        String name,
        String description,
        List<Part> parts
) {}
