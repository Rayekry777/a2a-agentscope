package com.ray.a2a.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Agent Card 发现接口。
 *
 * A2A 协议规定：Client 启动时访问 <agent-url>/.well-known/agent.json
 * 拉取该 Agent 的元信息（名字、能力、支持的 skills），用于注册和路由。
 *
 * 类似 Web 生态的 /robots.txt 或 /.well-known/openid-configuration，是固定路径。
 */
@RestController
public class AgentCardController {

    /** 返回固定的 Agent Card JSON，描述本 Agent 的能力 */
    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "name", "销售数据分析 Agent",
                "description", "专门处理销售数据分析任务，能生成报表、识别异常、预测趋势",
                "url", "http://localhost:8080",
                "version", "1.0.0",
                "capabilities", Map.of(
                        "streaming", false,
                        "pushNotifications", false
                ),
                "defaultInputModes", List.of("text"),
                "defaultOutputModes", List.of("text", "data"),
                "skills", buildSkills()
        );
    }

    /**
     * 声明本 Agent 支持的所有 Skill。
     * skillId 必须和 SalesAnomalySkillHandler.skillId() / SalesQuerySkillHandler.skillId() 等保持一致，
     * 否则 A2aController 里 skillHandlers.get(skillId) 会返回 null。
     */
    private List<Map<String, Object>> buildSkills() {
        return List.of(
                Map.of(
                        "id", "query-sales-summary",
                        "name", "查询销售汇总",
                        "description", "查询指定日期范围内的销售数据汇总，包括总销售额、订单数、客单价等核心指标",
                        "tags", List.of("sales", "query", "summary"),
                        "examples", List.of(
                                "查一下上周的销售情况",
                                "本月销售额是多少？和上月比怎么样？"
                        ),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                ),
                Map.of(
                        "id", "analyze-sales-anomaly",
                        "name", "销售异常检测",
                        "description", "分析指定时间段内的销售数据，识别异常波动，找出原因并给出处置建议",
                        "tags", List.of("sales", "analytics", "anomaly"),
                        "examples", List.of(
                                "昨天下午销售额突然跌了，帮我分析原因",
                                "最近7天有没有销售异常的商品？"
                        ),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                ),
                Map.of(
                        "id", "generate-sales-report",
                        "name", "生成销售报告",
                        "description", "生成结构化的销售分析报告，包含核心指标、趋势、产品分析",
                        "tags", List.of("sales", "report", "analysis"),
                        "examples", List.of(
                                "帮我生成本周销售报告",
                                "出一份本月销售分析"
                        ),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                ),
                Map.of(
                        "id", "generate-sales-report",
                        "name", "生成销售报告",
                        "description", "根据用户提供的时间范围和地区生成销售分析报告，信息不全时会主动追问",
                        "tags", List.of("sales", "report"),
                        "examples", List.of("帮我生成一份销售报告", "生成上个月华东地区的销售报告"),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                )
        );
    }
}
