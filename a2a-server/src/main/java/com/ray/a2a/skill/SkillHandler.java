package com.ray.a2a.skill;

import com.ray.a2a.model.Message;
import com.ray.a2a.model.Task;

import java.util.List;

/**
 * Skill 处理器接口。
 * 
 * 每个实现类对应一个具体的业务能力（比如"销售异常分析"、"销售汇总查询"）。
 * Spring 会把所有实现自动注入到 A2aController 的 List&lt;SkillHandler&gt; 里，
 * 按 skillId() 注册到 Map，路由时用 skillId 查找。
 */
public interface SkillHandler {

    /** 唯一标识，必须和 AgentCardController 里声明的 id 一致 */
    String skillId();

    /**
     * 同步执行 Skill，返回 COMPLETED 状态的 Task。
     * 
     * 实现类内部通常会调 LLM 生成结果，组装成 Task 返回。
     */
    Task handle(String taskId, String sessionId, List<Message> history);
}
