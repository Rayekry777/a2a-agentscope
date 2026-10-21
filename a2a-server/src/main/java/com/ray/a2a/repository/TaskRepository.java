package com.ray.a2a.repository;

import com.ray.a2a.model.Task;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务仓库：用 ConcurrentHashMap 做内存级任务存储。
 * 
 * 生产环境应换成 Redis / 数据库；这里单机演示用。
 * 
 * Key: task.id()  Value: 完整 Task 对象（含 status、history、artifacts）
 */
@Component
public class TaskRepository {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    /** 保存或覆盖任务（按 task.id() 去重） */
    public void save(Task task) {
        tasks.put(task.id(), task);
    }

    /** 按 ID 查询，不存在返回 null */
    public Task findById(String taskId) {
        return tasks.get(taskId);
    }
}
