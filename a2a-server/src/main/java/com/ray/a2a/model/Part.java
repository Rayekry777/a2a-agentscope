package com.ray.a2a.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Part：消息内容分片（多态）。
 * 
 * 一条 Message 可以由多个 Part 组成，比如一个文本 Part + 一个数据 Part。
 * 工厂方法 {@link #text(String)} 和 {@link #data(Map)} 简化构造。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Part(
        String type,        // "text" | "data" | "file"
        String text,        // type=text 时的文本内容
        Map<String, Object> data  // type=data 时的结构化数据
) {
    public static Part text(String text) {
        return new Part("text", text, null);
    }

    public static Part data(Map<String, Object> data) {
        return new Part("data", null, data);
    }
}
