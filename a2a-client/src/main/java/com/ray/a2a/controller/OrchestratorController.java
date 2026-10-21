package com.ray.a2a.controller;

import com.ray.a2a.agent.OrchestratorAgent;
import org.springframework.web.bind.annotation.*;

/**
 * 同步问答入口。
 * 
 * GET /api/orchestrator/ask?request=xxx
 * 
 * 流程：接收用户问题 → OrchestratorAgent 路由 → 调远程 Agent → 返回文本结果。
 * 注意：这个接口会阻塞几十秒（等 LLM 生成），前端要 loading。
 */
@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final OrchestratorAgent orchestratorAgent;

    public OrchestratorController(OrchestratorAgent orchestratorAgent) {
        this.orchestratorAgent = orchestratorAgent;
    }

    /** 同步问答入口 */
    @GetMapping("/ask")
    public String ask(@RequestParam String request) {
        return orchestratorAgent.handle(request);
    }
}
