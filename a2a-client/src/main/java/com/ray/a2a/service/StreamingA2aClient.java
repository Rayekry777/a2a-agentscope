package com.ray.a2a.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Consumer;

/**
 * 流式 A2A 客户端：订阅远程 Agent 的 SSE 事件流。
 * 
 * 对应 server 端的 A2aStreamController（POST /stream）。
 * 
 * 与 A2aClient 的区别：
 * 
 *   - A2aClient 是同步的（RestTemplate），阻塞直到任务完成 *   - StreamingA2aClient 是反应式的（WebClient），每个事件到达时回调 onEvent * 
 * 
 * Debug 提示：SSE 数据行格式是 "data: {...}"，这里做了简单解析；
 * 如果远程返回多行事件或 event: 行，需要更完整的 SSE 解析器。
 */
@Component
@Slf4j
public class StreamingA2aClient {

    private final WebClient webClient;

    public StreamingA2aClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /**
     * 流式订阅任务。
     *
     * @param agentUrl    远程 Agent 地址（如 http://localhost:8080）
     * @param taskId      任务 ID
     * @param sessionId   会话 ID
     * @param skillId     要调用的 Skill
     * @param history     对话历史
     * @param onEvent     每个 SSE 事件到达时的回调
     * @param onComplete  事件流结束时的回调
     */
    public void sendSubscribe(String agentUrl, String taskId, String sessionId,
                               String skillId, List<Map<String, Object>> history,
                               Consumer<Map<String, Object>> onEvent,
                               Runnable onComplete) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/sendSubscribe",
                "params", Map.of(
                        "id", taskId,
                        "sessionId", sessionId,
                        "skillId", skillId,
                        "history", history
                ),
                "id", UUID.randomUUID().toString()
        );

        webClient.post()
                .uri(agentUrl + "/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rpcRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        line -> {
                            // 简化的 SSE 解析：只处理 "data:" 开头的行
                            if (line.startsWith("data:")) {
                                onEvent.accept(Map.of("raw", line.substring(5).trim()));
                            }
                        },
                        error -> log.error("[A2A] SSE 订阅出错: {}", error.getMessage()),
                        onComplete
                );
    }
}
