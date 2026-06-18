package com.example.chatbot.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(Resource systemPrompt, int maxMessages) {
}
