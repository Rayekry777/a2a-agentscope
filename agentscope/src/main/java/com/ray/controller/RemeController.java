package com.ray.controller;

import com.ray.service.RemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reme-platform")
@RequiredArgsConstructor
public class RemeController {
    private final RemeService reme;

    @PostMapping("/chat")
    public Map<String, String> chat(
            @RequestParam String userId,
            @RequestParam String message) {
        String reply = reme.chat(userId, message);
        return Map.of("userId", userId, "reply", reply);
    }
}
