package com.ray.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A Agent Server 启动类。
 * 
 * 启动后监听 8080 端口，对外暴露：
 * 
 *   - POST /            —— JSON-RPC 同步端点（tasks/send、tasks/get、tasks/cancel） *   - POST /stream      —— SSE 流式端点（tasks/sendSubscribe） *   - GET  /.well-known/agent.json —— Agent Card 发现接口 * 
 * 
 * Debug 提示：启动前需保证环境变量 DASHSCOPE_API_KEY 已设置，
 * 否则 Spring AI OpenAI 自动装配时会因缺 api-key 抛 IllegalArgumentException。
 */
@SpringBootApplication
class A2AServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2AServerApplication.class, args);
    }

}
