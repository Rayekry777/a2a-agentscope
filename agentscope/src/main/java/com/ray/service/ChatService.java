package com.ray.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final DashScopeChatModel model;

    // 用 ConcurrentHashMap 简单管理多用户 Agent 实例
    private final Map<String, ReActAgent> agentPool = new ConcurrentHashMap<>();

    public ChatService(DashScopeChatModel model) {
        this.model = model;
    }

    public String chat(String userId, String userMessage) {
        // 每个 userId 对应一个独立的 Agent 实例
        ReActAgent agent = agentPool.computeIfAbsent(userId, id ->
                ReActAgent.builder()
                        .name("助手-" + id)
                        .model(model)
                        .sysPrompt("你是一个助手。")
                        .memory(new InMemoryMemory())
                        .build()
        );

        Msg response = agent.call(
                Msg.builder().textContent(userMessage).build()
        ).block();

        return response.getTextContent();
    }

    public void clearSession(String userId) {
        ReActAgent agent = agentPool.remove(userId);
        if (agent != null) {
            agent.getMemory().clear();
        }
    }
}