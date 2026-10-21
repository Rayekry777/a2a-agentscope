package com.ray.a2a.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * A2A 协议客户端：封装对远程 Agent 的 JSON-RPC 调用。
 *
 * 对应 server 端的 A2aController（POST /）。
 *
 * 支持四种操作：
 *   - getAgentCard  —— GET /.well-known/agent.json，拉取 Agent 元信息
 *   - sendTask      —— POST /，method=tasks/send，同步提交任务
 *   - getTask       —— POST /，method=tasks/get，查询任务状态
 *   - cancelTask    —— POST /，method=tasks/cancel，取消任务
 */
@Component
public class A2aClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public A2aClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /** 拉取 Agent Card（注册时用） */
    public Map<String, Object> getAgentCard(String agentUrl) {
        String cardUrl = agentUrl + "/.well-known/agent.json";
        ResponseEntity<Map> response = restTemplate.getForEntity(cardUrl, Map.class);
        return response.getBody();
    }

    /** 同步发送任务（阻塞直到远程 Agent 返回 COMPLETED） */
    public Map<String, Object> sendTask(String agentUrl, String taskId,
                                        String sessionId, String skillId,
                                        List<Map<String, Object>> history) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", taskId);
        params.put("sessionId", sessionId);
        params.put("skillId", skillId);
        params.put("history", history);

        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/send",
                "params", params,
                "id", UUID.randomUUID().toString()
        );

        return postJsonRpc(agentUrl, rpcRequest);
    }

    /** 查询任务状态 */
    public Map<String, Object> getTask(String agentUrl, String taskId) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/get",
                "params", Map.of("id", taskId),
                "id", UUID.randomUUID().toString()
        );
        return postJsonRpc(agentUrl, rpcRequest);
    }

    /** 取消任务 */
    public Map<String, Object> cancelTask(String agentUrl, String taskId) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/cancel",
                "params", Map.of("id", taskId),
                "id", UUID.randomUUID().toString()
        );
        return postJsonRpc(agentUrl, rpcRequest);
    }

    /**
     * 统一发送 JSON-RPC 请求。
     * 
     * 错误处理：响应体里有 error 字段时抛 RuntimeException；
     * 成功时返回 result 字段（即 Task 对象）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postJsonRpc(String agentUrl, Map<String, Object> rpcRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 如果需要鉴权，在这里加 Authorization header
        // headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(rpcRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(agentUrl + "/", entity, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new RuntimeException("空响应");
        }
        if (body.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) body.get("error");
            throw new RuntimeException("A2A 错误: " + error.get("message"));
        }

        return (Map<String, Object>) body.get("result");
    }
}
