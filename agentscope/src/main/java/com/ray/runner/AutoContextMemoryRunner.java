package com.ray.runner;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.memory.autocontext.ContextOffloadTool;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


public class AutoContextMemoryRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public AutoContextMemoryRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 配置压缩策略
        // 注意：msgThreshold 是触发阈值，不是硬上限。
        // 压缩在每次 agent.call() 开始时检测，超过阈值则把历史压到 lastKeep 条，
        // 然后本轮 ReAct 循环继续追加消息。一轮对话通常追加 2-4 条（用户+回复+工具调用等），
        // 所以实际消息数会在 lastKeep ~ msgThreshold+单轮消息数 之间波动，这是正常现象。
        AutoContextConfig config = AutoContextConfig.builder()
                .msgThreshold(10)     // 超过 10 条时在下次 call 开始前触发压缩
                .lastKeep(5)          // 压缩后保留最近 5 条不动
                .tokenRatio(0.3)      // Token 使用率超过 30% 时也会触发（两个条件任一满足）
                .build();

        // AutoContextMemory 需要传入 model，触发压缩时用它生成摘要
        AutoContextMemory memory = new AutoContextMemory(config, model);

        // ContextOffloadTool：让 Agent 能按需重载被卸载的历史内容
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ContextOffloadTool(memory));

        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .sysPrompt("你是一个助手，能记住对话历史。")
                .memory(memory)
                .toolkit(toolkit)
                .build();

        // 模拟多轮对话，观察消息数变化
        String[] questions = {
                "我在做一个电商项目",
                "项目技术栈是 Spring Boot + MySQL",
                "我遇到了库存超卖的问题",
                "我打算用分布式锁解决",
                "刚才我说的项目背景是什么"   // 测试记忆是否生效
        };

        for (String q : questions) {
            Msg response = agent.call(Msg.builder().textContent(q).build()).block();
            System.out.printf("[消息数: %d] 用户: %s%n", memory.getMessages().size(), q);
            System.out.println("[Agent] " + response.getTextContent());
            System.out.println("---");
        }
    }
}