package com.ray.a2a.skill;

import com.ray.a2a.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 销售报告生成 Skill。
 *
 * 对应 skillId: generate-sales-report。
 *
 * 本类是唯一实现了 {@link StreamingSkillHandler} 的 Skill：
 *   - 同步场景（POST /）：走 handle()，直接调 LLM 返回完整报告
 *   - 流式场景（POST /stream）：走 handleStreaming()，分步推送进度事件
 *     （正在获取数据 → 正在分析趋势 → 正在生成报告 → artifact → completed）
 *
 * Debug 提示：报告较长时注意 yml 里 spring.ai.openai.max-tokens 要给够（8192+），
 * 否则 DeepSeek 推理模型会因 reasoning_content 占满预算而输出空 content。
 */
@Component
public class SalesReportSkillHandler implements StreamingSkillHandler {

    private final ChatClient chatClient;

    public SalesReportSkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个专业的销售报告生成助手，生成结构清晰、数据详实的分析报告。")
                .build();
    }

    @Override
    public String skillId() {
        return "generate-sales-report";
    }

    /** 同步入口：直接生成完整报告 */
    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        String report = generateReport(extractUserMessage(history));
        TaskStatus status = new TaskStatus(TaskState.COMPLETED,
                new Message("agent", List.of(Part.text(report))),
                Instant.now().toString());
        return new Task(taskId, sessionId, status, history,
                List.of(new Artifact("销售报告", "生成的报告", List.of(Part.text(report)))),
                Map.of());
    }

    /**
     * 流式入口：分步推送进度事件。
     * 
     * 事件顺序：working(获取数据) → working(分析趋势) → working(生成报告) →
     *           artifact(完整报告) → completed。
     * Thread.sleep 是模拟耗时，生产环境可去掉。
     */
    @Override
    public void handleStreaming(String taskId, String sessionId, List<Message> history,
                                SseEmitter emitter, String requestId) {
        try {
            String userRequest = extractUserMessage(history);

            pushProgress(emitter, taskId, requestId, "正在获取销售数据...");
            Thread.sleep(1000); // 模拟数据获取耗时

            pushProgress(emitter, taskId, requestId, "正在分析数据趋势...");
            Thread.sleep(1500); // 模拟分析耗时

            pushProgress(emitter, taskId, requestId, "正在生成报告...");
            String report = generateReport(userRequest);

            pushArtifact(emitter, taskId, requestId, report);
            pushCompleted(emitter, taskId, requestId, report);

        } catch (Exception e) {
            try {
                pushFailed(emitter, taskId, requestId, e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /** 调 LLM 生成报告（.call() 是阻塞同步调用） */
    private String generateReport(String userRequest) {
        return chatClient.prompt()
                .user("请生成一份销售分析报告，需求：" + userRequest)
                .call()
                .content();
    }

    /** 推送 working 状态事件（中间进度，final=false） */
    private void pushProgress(SseEmitter emitter, String taskId, String requestId, String message) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "working",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", message))),
                                "timestamp", Instant.now().toString()),
                        "final", false)));
    }

    /** 推送 artifact 事件（产出物，final=false） */
    private void pushArtifact(SseEmitter emitter, String taskId, String requestId, String content) {
        sendEvent(emitter, "task_artifact_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "artifact", Map.of("name", "销售报告",
                                "parts", List.of(Map.of("type", "text", "text", content))),
                        "final", false)));
    }

    /** 推送 completed 状态（final=true，推完自动关闭 SSE 连接） */
    private void pushCompleted(SseEmitter emitter, String taskId, String requestId, String content) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "completed",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", content))),
                                "timestamp", Instant.now().toString()),
                        "final", true)));
        emitter.complete();
    }

    /** 推送 failed 状态（final=true） */
    private void pushFailed(SseEmitter emitter, String taskId, String requestId, String error) {
        sendEvent(emitter, "task_status_update", Map.of(
                "jsonrpc", "2.0", "id", requestId,
                "result", Map.of(
                        "id", taskId,
                        "status", Map.of("state", "failed",
                                "message", Map.of("role", "agent",
                                        "parts", List.of(Map.of("type", "text", "text", "执行失败: " + error))),
                                "timestamp", Instant.now().toString()),
                        "final", true)));
        emitter.complete();
    }

    /** 统一发送 SSE 事件，异常时关闭连接 */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    /** 取最后一条 user 消息的文本 */
    private String extractUserMessage(List<Message> history) {
        return history.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()))
                        .map(Part::text).findFirst().orElse(""))
                .orElse("");
    }
}
