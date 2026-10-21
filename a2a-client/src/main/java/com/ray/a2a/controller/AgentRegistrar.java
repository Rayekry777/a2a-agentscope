package com.ray.a2a.controller;

import com.ray.a2a.service.AgentRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Agent 注册器：应用启动后自动注册远程 Agent。
 * 
 * 实现 ApplicationRunner，在 Spring Boot 启动完成后执行 run()。
 * 当前硬编码注册本地 8080 的 Sales Agent；生产环境应从配置文件读取 Agent URL 列表。
 */
@Component
public class AgentRegistrar implements ApplicationRunner {

    private final AgentRegistry registry;

    public AgentRegistrar(AgentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 注册本地 Sales Agent（开发时）
        registry.register("http://localhost:8080");

        // 生产环境从配置文件读取 Agent URL 列表
        // agentUrls.forEach(registry::register);
    }
}
