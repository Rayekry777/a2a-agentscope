package com.ray.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A Client（Orchestrator）启动类。
 * 
 * 启动后监听 8090 端口，是用户请求的入口：
 * 
 *   - GET /api/orchestrator/ask          —— 同步问答：LLM 路由 → 远程 Agent → 返回结果 *   - GET /api/orchestrator/stream-ask   —— 流式问答：SSE 实时转发远程 Agent 的事件 * 
 * 
 * 它本身不直接调 LLM 做业务分析，而是通过 OrchestratorAgent 决定调用哪个远程 Agent。
 */
@SpringBootApplication
class A2AClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2AClientApplication.class, args);
    }

}
