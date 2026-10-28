package com.ray.controller;


import com.ray.service.PersistentChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final PersistentChatService chatService;

    /**
     * 发送消息
     * sessionId 可以是 userId，也可以是更细粒度的会话 ID
     */
    @PostMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> chat(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body
    ) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }

        String reply = chatService.chat(sessionId, message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    /**
     * 清空会话历史（开启新对话）
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        // 安全校验：JsonSession 直接拿 sessionId 当目录名，必须防路径穿越（../）
        if (!sessionId.matches("[a-zA-Z0-9_-]+")) {
            return ResponseEntity.badRequest().build();
        }
        Path sessionDir = Path.of("sessions", sessionId);
        try {
            FileSystemUtils.deleteRecursively(sessionDir);
        } catch (IOException e) {
            log.warn("删除会话目录失败：{}", sessionDir, e);
        }
        return ResponseEntity.noContent().build();
    }
}