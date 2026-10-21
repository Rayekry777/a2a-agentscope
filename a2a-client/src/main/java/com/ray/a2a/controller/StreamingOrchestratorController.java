package com.ray.a2a.controller;

import com.ray.a2a.service.StreamingA2aClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * 流式问答入口。
 * 
 * GET /api/orchestrator/stream-ask?question=xxx&skillId=yyy
 * 
 * 与同步接口的区别：
 * 
 *   - 同步：等远程 Agent 全部执行完才返回 *   - 流式：远程 Agent 每推一个事件（working/artifact/completed），
 *       本接口立刻转发给浏览器，用户能看到实时进度 * 
 * 
 * 注意：这里 skillId 是前端直接指定的，没有走 OrchestratorAgent 的 LLM 路由。
 */
@RestController
@RequestMapping("/api/orchestrator")
public class StreamingOrchestratorController {

    private final StreamingA2aClient streamingA2aClient;

    public StreamingOrchestratorController(StreamingA2aClient streamingA2aClient) {
        this.streamingA2aClient = streamingA2aClient;
    }

    /**
     * 流式问答：把 A2A SSE 事件实时转发给浏览器。
     * 
     * 这里硬编码了远程 Agent URL（localhost:8080），生产环境应从 AgentRegistry 取。
     */
    @GetMapping(value = "/stream-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestParam String question, @RequestParam String skillId) {
        SseEmitter emitter = new SseEmitter(300_000L);

        streamingA2aClient.sendSubscribe(
                "http://localhost:8080",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                skillId,
                List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("type", "text", "text", question)))),
                event -> {
                    try {
                        emitter.send(event);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::complete
        );

        return emitter;
    }
}
