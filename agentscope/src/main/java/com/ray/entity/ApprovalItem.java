package com.ray.entity;

// ApprovalItem.java（待审批的工单记录）
public record ApprovalItem(
        String approvalId,
        String agentName,
        String toolName,
        String toolInput,
        long   createdAt
) {}