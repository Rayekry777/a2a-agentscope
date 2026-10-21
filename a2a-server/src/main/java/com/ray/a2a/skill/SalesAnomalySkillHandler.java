package com.ray.a2a.skill;

import com.ray.a2a.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 销售异常检测 Skill。
 * 
 * 对应 skillId: analyze-sales-anomaly。
 * 
 * 职责：拿到一段模拟的异常销售数据（支付失败率飙升、转化率下跌），
 * 交给 LLM 生成结构化的异常分析报告（异常描述 + 可能原因 + 处置建议），
 * 同时把结构化结果放进 Artifact.data 方便程序化消费。
 */
@Component
public class SalesAnomalySkillHandler implements SkillHandler {

    private final ChatClient chatClient;

    public SalesAnomalySkillHandler(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个销售异常分析专家。
                        分析给定的销售数据，识别异常情况，找出可能的原因，给出处置建议。
                        输出结构：1.异常情况描述 2.可能原因 3.建议措施
                        """)
                .build();
    }

    @Override
    public String skillId() {
        return "analyze-sales-anomaly";
    }

    @Override
    public Task handle(String taskId, String sessionId, List<Message> history) {
        // 从历史消息里取出最后一条用户提问，作为分析请求
        String userQuery = extractLastUserMessage(history);

        // 模拟异常数据（生产环境应从数据库/监控系统实时取）
        String anomalyData = """
                异常数据：
                昨日 14:00-16:00 期间：
                - 销售额：¥12,000（正常时段均值 ¥45,000，下降 73%）
                - 订单转化率：1.2%（正常 4.5%，下降 73%）
                - 页面 PV 正常（说明流量没问题）
                - 支付失败率：38%（正常 < 2%）
                """;

        // 调 LLM 生成分析结论
        String analysis = chatClient.prompt()
                .user("请分析以下异常情况：\n" + anomalyData + "\n\n" + userQuery)
                .call()
                .content();

        // 结构化结果：方便下游程序读取严重等级、时间段等字段
        Map<String, Object> structuredResult = Map.of(
                "anomalyPeriod", "2025-04-15 14:00-16:00",
                "severity", "HIGH",
                "analysis", analysis
        );

        // Artifact 同时携带文本分析和结构化数据
        Artifact artifact = new Artifact(
                "异常分析报告",
                "销售异常检测分析结果",
                List.of(Part.text(analysis), Part.data(structuredResult))
        );

        // 组装 COMPLETED 状态的 Task
        TaskStatus status = new TaskStatus(
                TaskState.COMPLETED,
                new Message("agent", List.of(Part.text(analysis))),
                Instant.now().toString()
        );

        return new Task(taskId, sessionId, status, history, List.of(artifact), Map.of());
    }

    /** 从历史消息里取最后一条 user 消息的文本内容 */
    private String extractLastUserMessage(List<Message> history) {
        return history.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(m -> m.parts().stream()
                        .filter(p -> "text".equals(p.type()))
                        .map(Part::text)
                        .findFirst().orElse(""))
                .orElse("");
    }
}
