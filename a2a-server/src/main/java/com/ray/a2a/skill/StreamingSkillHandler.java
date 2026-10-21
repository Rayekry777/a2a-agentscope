package com.ray.a2a.skill;

import com.ray.a2a.model.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 支持流式中间状态推送的 Skill 接口。
 * 
 * 实现这个接口的 Skill 在 SSE 场景下（POST /stream）会收到 SseEmitter，
 * 可以主动推送多条事件（进度、artifact、完成），而不是等执行完一次性返回。
 * 
 * 同步场景（POST /）下仍然走 handle()，忽略 emitter。
 */
public interface StreamingSkillHandler extends SkillHandler {

    /**
     * 流式执行：通过 emitter.send() 推送中间事件。
     * 
     * 实现方负责在最后调用 emitter.complete() 关闭连接。
     */
    void handleStreaming(String taskId, String sessionId,
                         List<Message> history, SseEmitter emitter, String requestId);
}
