package com.ray.a2a.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.a2a.config.WebhookRegistry;
import com.ray.a2a.executor.AsyncTaskExecutor;
import com.ray.a2a.model.*;
import com.ray.a2a.repository.TaskRepository;
import com.ray.a2a.skill.ReportGenerationSkillHandler;
import com.ray.a2a.skill.SkillHandler;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * A2A 协议 JSON-RPC 同步入口（POST /）。
 *
 * 支持三种 method：
 *   - tasks/send   —— 同步提交任务，阻塞直到 Skill 执行完返回完整 Task
 *   - tasks/get    —— 按 taskId 查询任务当前状态
 *   - tasks/cancel —— 取消任务，状态改为 CANCELED
 *
 * 请求体结构（JSON-RPC 2.0）：
 * {
 *   "jsonrpc": "2.0",
 *   "method": "tasks/send",
 *   "params": { "id": "...", "sessionId": "...", "skillId": "...", "history": [...] },
 *   "id": "客户端请求ID"
 * }
 */
@RestController
public class A2aController {

    /** skillId → SkillHandler，Spring 自动把所有 SkillHandler 实现注入 List 后转 Map */
    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final WebhookRegistry webhookRegistry;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public A2aController(
            List<SkillHandler> handlers,
            TaskRepository taskRepository,
            ObjectMapper objectMapper,
            WebhookRegistry webhookRegistry,
            AsyncTaskExecutor asyncTaskExecutor) {

        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
        this.webhookRegistry = webhookRegistry;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    /** 统一 JSON-RPC 入口，按 method 分发 */
    @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        return switch (request.method()) {
            case "tasks/send"   -> handleTaskSend(request);
            case "tasks/get"    -> handleTaskGet(request);
            case "tasks/cancel" -> handleTaskCancel(request);
            case "tasks/pushNotification/set" -> handleSetPushNotification(request);
            default -> JsonRpcResponse.error(-32601, "Method not found: " + request.method(), request.id());
        };
    }

    /**
     * tasks/send：异步提交任务
     *
     *   - 解析参数（taskId、sessionId、skillId、history）
     *   - 先落库 SUBMITTED 状态，立刻返回（不等 LLM 执行完）
     *   - 后台 AsyncTaskExecutor 异步执行 Skill，完成后通过 Webhook 回调 client
     *   - 续传任务（input-required 状态）仍同步执行，因为需要基于上下文继续生成
     */
    private JsonRpcResponse handleTaskSend(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
        String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
        String skillId = (String) params.get("skillId");
        List<Message> history = parseHistory(
                (List<Map<String, Object>>) params.get("history"));
        // 检查是否续传任务
        Task existingTask = taskRepository.findById(taskId);
        boolean isContinuation = existingTask != null &&
                TaskState.INPUT_REQUIRED.equals(existingTask.status().state());
        // 续传任务走同步（input-required 场景不适合异步）
        if (isContinuation) {
            SkillHandler handler = skillHandlers.get(skillId);
            if (handler == null) {
                return JsonRpcResponse.error(-32602, "Unknown skill: " + skillId, request.id());
            }
            if (handler instanceof ReportGenerationSkillHandler reportHandler) {
                Task result = reportHandler.continueTask(taskId, sessionId, history);
                taskRepository.save(result);
                return JsonRpcResponse.success(result, request.id());
            }
        }
        // 新任务：交给 AsyncTaskExecutor 异步执行，立即返回 submitted 状态
        // 任务完成后 AsyncTaskExecutor 会自动向注册的 Webhook 地址发回调
        try {
            Task submitted = asyncTaskExecutor.submitAsync(taskId, sessionId, skillId, history);
            return JsonRpcResponse.success(submitted, request.id());
        } catch (IllegalArgumentException e) {
            return JsonRpcResponse.error(-32602, e.getMessage(), request.id());
        }
    }

    /** tasks/get：按 taskId 返回任务当前快照 */
    private JsonRpcResponse handleTaskGet(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        return JsonRpcResponse.success(task, request.id());
    }

    /** tasks/cancel：把任务状态改成 CANCELED，保留原 history 和 artifacts */
    private JsonRpcResponse handleTaskCancel(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        Task canceledTask = new Task(
                task.id(), task.sessionId(),
                new TaskStatus(TaskState.CANCELED, null, Instant.now().toString()),
                task.history(), task.artifacts(), task.metadata()
        );
        taskRepository.save(canceledTask);
        return JsonRpcResponse.success(canceledTask, request.id());
    }

    /**
     * 把 client 传过来的 List<Map> 历史消息转成强类型 List<Message>。
     * 
     * 每个 part 根据 type 字段决定走 Part.text 还是 Part.data。
     */
    @SuppressWarnings("unchecked")
    private List<Message> parseHistory(List<Map<String, Object>> rawHistory) {
        if (rawHistory == null) return List.of();
        return rawHistory.stream().map(raw -> {
            String role = (String) raw.get("role");
            List<Map<String, Object>> rawParts = (List<Map<String, Object>>) raw.get("parts");
            List<Part> parts = rawParts.stream().map(p -> {
                String type = (String) p.get("type");
                return switch (type) {
                    case "text" -> Part.text((String) p.get("text"));
                    case "data" -> Part.data((Map<String, Object>) p.get("data"));
                    default -> Part.text("");
                };
            }).toList();
            return new Message(role, parts);
        }).toList();
    }

    private JsonRpcResponse handleSetPushNotification(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        String taskId = (String) params.get("id");

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) params.get("pushNotification");
        String callbackUrl = (String) config.get("url");
        String secret = (String) config.getOrDefault("token", null);

        webhookRegistry.register(taskId, callbackUrl, secret);

        return JsonRpcResponse.success(Map.of(
                "id", taskId,
                "pushNotification", Map.of("url", callbackUrl)
        ), request.id());
    }
}
