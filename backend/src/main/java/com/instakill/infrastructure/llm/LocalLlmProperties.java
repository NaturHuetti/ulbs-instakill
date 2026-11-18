package com.instakill.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instakill.llm")
public record LocalLlmProperties(
        String url,
        String model,
        int timeoutSeconds
) {
}
