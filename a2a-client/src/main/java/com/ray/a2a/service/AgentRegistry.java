package com.ray.a2a.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册表：客户端侧维护所有已注册远程 Agent 的信息。
 * 
 * 启动时由 AgentRegistrar 调用 register() 注册；
 * OrchestratorAgent 通过 buildAgentsSummary() 拿到所有 Agent 的能力摘要，喂给 LLM 做路由。
 */
@Component
public class AgentRegistry {

    private final A2aClient a2aClient;

    /** key: Agent 名称（来自 Agent Card 的 name 字段），value: Agent 信息 */
    private final Map<String, AgentInfo> agents = new ConcurrentHashMap<>();

    public AgentRegistry(A2aClient a2aClient) {
        this.a2aClient = a2aClient;
    }

    /**
     * 注册一个 Agent：先拉它的 Agent Card，再按 name 存进 Map。
     * 同名 Agent 会被覆盖。
     */
    public void register(String agentUrl) {
        Map<String, Object> card = a2aClient.getAgentCard(agentUrl);
        String name = (String) card.get("name");
        agents.put(name, new AgentInfo(name, agentUrl, card));
    }

    /**
     * 把所有 Agent 的能力拼成一段文本，喂给 LLM 做路由决策。
     * 
     * 输出格式：
     * 
     * ## 销售数据分析 Agent
     * URL: http://localhost:8080
     * 描述: ...
     * Skills:
     *   - [query-sales-summary] 查询销售汇总: ...
     *   - [analyze-sales-anomaly] 销售异常检测: ...
     * 
     */
    @SuppressWarnings("unchecked")
    public String buildAgentsSummary() {
        StringBuilder sb = new StringBuilder("可用的 Agent 列表：\n\n");
        agents.forEach((name, info) -> {
            sb.append("## ").append(name).append("\n");
            sb.append("URL: ").append(info.url()).append("\n");
            sb.append("描述: ").append(info.card().get("description")).append("\n");

            List<Map<String, Object>> skills = (List<Map<String, Object>>) info.card().get("skills");
            if (skills != null) {
                sb.append("Skills:\n");
                skills.forEach(skill ->
                    sb.append("  - [").append(skill.get("id")).append("] ")
                      .append(skill.get("name")).append(": ")
                      .append(skill.get("description")).append("\n")
                );
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    /** 按名称取 Agent */
    public AgentInfo getAgent(String name) {
        return agents.get(name);
    }

    /** 取所有 Agent */
    public Collection<AgentInfo> getAllAgents() {
        return agents.values();
    }

    /** Agent 信息 record：名称 + URL + 完整 Agent Card */
    public record AgentInfo(String name, String url, Map<String, Object> card) {}
}
