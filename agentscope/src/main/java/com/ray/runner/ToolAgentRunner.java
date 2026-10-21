package com.ray.runner;


import com.ray.hook.ToolMonitorHook;
import com.ray.tool.DateTimeTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;


public class ToolAgentRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final DateTimeTool dateTimeTool;

    public ToolAgentRunner(DashScopeChatModel model, DateTimeTool dateTimeTool) {
        this.model = model;
        this.dateTimeTool = dateTimeTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(dateTimeTool);

        ReActAgent agent = ReActAgent.builder()
                .name("时间助手")
                .sysPrompt("你是一个助手，可以查询当前时间。")
                .model(model)
                .toolkit(toolkit)
                .hooks(List.of(new ToolMonitorHook()))  // 用 hooks() 传 List
                .build();

        Msg response = agent.call(
                Msg.builder().textContent("现在几点了？").build()
        ).block();

        System.out.println(response.getTextContent());
    }
}