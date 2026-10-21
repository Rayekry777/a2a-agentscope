package com.ray.a2a.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP 客户端配置。
 * 
 * A2A 任务是"同步等待结果"的长耗时调用（LLM 生成要几十秒），
 * 所以 readTimeout 给到 60s，避免 RestTemplate 默认 30s 直接断。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(60))   // A2A 任务可能耗时较长
                .build();
    }
}
