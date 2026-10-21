package com.ray.a2a.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Task 生命周期状态枚举。
 * 
 * 状态流转：
 * 
 * SUBMITTED → WORKING → COMPLETED
 *                  ↘ FAILED
 *                  ↘ INPUT_REQUIRED → WORKING → ...
 * 任意阶段 → CANCELED
 * 
 * 
 * {@code @JsonValue} 让 Jackson 序列化时输出小写字符串（如 "working"），
 * {@code @JsonCreator} 反序列化时按小写字符串映射回枚举。
 */
public enum TaskState {
    SUBMITTED("submitted"),
    WORKING("working"),
    INPUT_REQUIRED("input-required"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled");

    private final String value;

    TaskState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TaskState fromValue(String value) {
        for (TaskState s : values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown TaskState: " + value);
    }
}
