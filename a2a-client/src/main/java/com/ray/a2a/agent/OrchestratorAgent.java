package com.ray.a2a.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.a2a.service.A2aClient;
import com.ray.a2a.service.AgentRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orchestrator Agent：客户端侧的任务路由器。
 *
 * 核心流程：
 *   - 从 AgentRegistry 拉取所有已注册 Agent 的能力摘要
 *   - 把"用户请求 + Agent 列表"喂给 LLM，让它输出 JSON 路由决策
 *     （选哪个 Agent、哪个 skillId、为什么）
 *   - 用 Jackson 解析 JSON（早期版本用字符串 contains 匹配，
 *     DeepSeek 输出紧凑 JSON 时会误判，已改成 ObjectMapper.readTree）
 *   - 通过 A2aClient 把任务发给选中的远程 Agent，同步等待结果
 *   - 从返回的 Task 里提取 status.message.parts[0].text 返回给用户
 *
 * Debug 提示：如果 LLM 返回的 JSON 被 ```json 包裹，parseDecision 会先剥掉 markdown 围栏。
 */
@Service
public class OrchestratorAgent {

    private final ChatClient chatClient;
    private final AgentRegistry agentRegistry;
    private final A2aClient a2aClient;
    private final ObjectMapper objectMapper;

    public OrchestratorAgent(
            ChatClient.Builder builder,
            AgentRegistry agentRegistry,
            A2aClient a2aClient,
            ObjectMapper objectMapper) {
        this.agentRegistry = agentRegistry;
        this.a2aClient = a2aClient;
        this.objectMapper = objectMapper;
        this.chatClient = builder.build();
    }

    /**
     * 处理用户请求：LLM 决定路由，委托给专业 Agent。
     * 
     * 这是一个"薄编排层"——它自己不做业务分析，只做路由 + 转发。
     */
    public String handle(String userRequest) {
        // 第一步：让 LLM 分析用户意图，决定调哪个 Agent 的哪个 Skill
        String agentsSummary = agentRegistry.buildAgentsSummary();

        String routingDecision = chatClient.prompt()
                .system("""
                        你是一个任务路由器。根据用户请求和可用 Agent 列表，
                        决定应该调用哪个 Agent 的哪个 Skill。
                        
                        只输出 JSON，格式：
                        {"agentName": "Agent名称", "skillId": "skill-id", "reason": "选择原因"}
                        
                        如果没有合适的 Agent，输出：
                        {"agentName": null, "skillId": null, "reason": "原因"}
                        """)
                .user("可用 Agent：\n" + agentsSummary + "\n\n用户请求：" + userRequest)
                .call()
                .content();

        // 解析路由决策
        RoutingDecision decision = parseDecision(routingDecision);

        if (decision.agentName() == null) {
            return "抱歉，没有找到能处理这个请求的 Agent：" + decision.reason();
        }

        // 第二步：委托给对应 Agent 执行
        AgentRegistry.AgentInfo agentInfo = agentRegistry.getAgent(decision.agentName());
        if (agentInfo == null) {
            return "Agent 不存在：" + decision.agentName();
        }

        String taskId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        // 构造 A2A 协议要求的 history 结构
        List<Map<String, Object>> history = List.of(
                Map.of("role", "user",
                       "parts", List.of(Map.of("type", "text", "text", userRequest)))
        );

        // 同步调用远程 Agent（阻塞直到返回）
        Map<String, Object> taskResult = a2aClient.sendTask(
                agentInfo.url(), taskId, sessionId, decision.skillId(), history);

        // 第三步：从 Task 里提取文本结果
        return extractResultText(taskResult);
    }

    /**
     * 从 Task 响应里提取 status.message.parts[0].text。
     * 响应结构是嵌套 Map：task → status → message → parts[] → text。
     */
    @SuppressWarnings("unchecked")
    private String extractResultText(Map<String, Object> task) {
        Map<String, Object> status = (Map<String, Object>) task.get("status");
        Map<String, Object> message = (Map<String, Object>) status.get("message");
        if (message == null) return "任务完成，但无文本输出";

        List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
        return parts.stream()
                .filter(p -> "text".equals(p.get("type")))
                .map(p -> (String) p.get("text"))
                .findFirst()
                .orElse("无文本输出");
    }

    /**
     * 用 Jackson 解析 LLM 返回的路由 JSON。
     * 
     * 先剥掉 ```json ... ``` markdown 围栏，再 readTree。
     * agentName 为 null 时表示"无合适 Agent"。
     */
    private RoutingDecision parseDecision(String json) {
        String cleaned = json.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            JsonNode agentNode = node.get("agentName");
            if (agentNode == null || agentNode.isNull()) {
                return new RoutingDecision(null, null,
                        node.path("reason").asText("未找到合适 Agent"));
            }
            return new RoutingDecision(
                    agentNode.asText(),
                    node.path("skillId").asText(null),
                    node.path("reason").asText(""));
        } catch (Exception e) {
            return new RoutingDecision(null, null, "解析失败: " + e.getMessage());
        }
    }

    /** 路由决策结果 record */
    record RoutingDecision(String agentName, String skillId, String reason) {}
}
