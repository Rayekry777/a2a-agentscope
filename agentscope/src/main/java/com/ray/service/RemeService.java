package com.ray.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.LongTermMemory;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemeService {

    private final DashScopeChatModel model;

    public synchronized String chat(String userId, String userMessage) {
        // 新版 ReMe 使用文件 API；用户标识编码后作为独立文件路径，防止路径穿越和串用户。
        String key = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(userId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String path = "digest/chat-users/" + key + ".md";
        RestClient client = RestClient.create("http://localhost:2333");
        LongTermMemory longTermMemory = new LongTermMemory() {
            private String history = "";

            private Object request(String endpoint, Map<String, Object> body) {
                Map<?, ?> result = client.post().uri(endpoint).body(body).retrieve().body(Map.class);
                if (endpoint.equals("/stat") && result != null) {
                    return result.get("metadata");
                }
                if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
                    throw new IllegalStateException("ReMe 调用失败：" + endpoint);
                }
                return result.get("answer");
            }

            @Override
            public Mono<String> retrieve(Msg message) {
                return Mono.fromCallable(() -> {
                    Object stat = request("/stat", Map.of("path", path));
                    if (stat instanceof Map<?, ?> details && Boolean.TRUE.equals(details.get("exists"))) {
                        history = String.valueOf(request("/load", Map.of("path", path)));
                    }
                    return history;
                });
            }

            @Override
            public Mono<Void> record(List<Msg> messages) {
                return Mono.fromRunnable(() -> {
                    StringBuilder content = new StringBuilder(history);
                    for (Msg message : messages) {
                        if (message.getRole() == io.agentscope.core.message.MsgRole.USER
                                || message.getRole() == io.agentscope.core.message.MsgRole.ASSISTANT) {
                            content.append("\n\n").append(message.getRole()).append(": ")
                                    .append(message.getTextContent());
                        }
                    }
                    request("/write", Map.of("path", path, "name", "用户对话记忆",
                            "description", "按用户隔离的历史对话", "content", content.toString()));
                });
            }
        };

        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .build();

        return agent.call(Msg.builder().textContent(userMessage).build())
                .block()
                .getTextContent();
    }
}
