package com.example.chatbot.api.controller;

import com.example.chatbot.api.config.ChatbotProperties;
import com.example.chatbot.api.data.dto.ChatRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatbotApiController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotApiController.class);
    static final String CONVERSATION_ID_HEADER = "X-Conversation-Id";

    private final ChatClient chatClient;
    private final String systemPromptTemplate;

    public ChatbotApiController(
            ChatClient.Builder chatClientBuilder, ChatbotProperties properties, ChatMemory chatMemory) {
        this.systemPromptTemplate = readText(properties.systemPrompt());
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        log.info("System prompt loaded:\n{}", systemPromptTemplate);
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendPrompt(@RequestBody @Valid ChatRequest chatRequest, HttpServletResponse httpResponse) {
        String conversationId = StringUtils.hasText(chatRequest.conversationId())
                ? chatRequest.conversationId()
                : UUID.randomUUID().toString();
        httpResponse.setHeader(CONVERSATION_ID_HEADER, conversationId);

        log.info("Chat request  [{}] user prompt: {}", conversationId, chatRequest.message());

        StringBuilder reply = new StringBuilder();
        return chatClient.prompt()
                .system(spec -> spec.text(systemPromptTemplate).param("current_date", LocalDate.now()))
                .user(chatRequest.message())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse()
                .doOnNext(response -> logUsage(conversationId, response))
                .mapNotNull(response -> response.getResult() != null
                        ? response.getResult().getOutput().getText()
                        : null)
                .filter(text -> !text.isEmpty())
                .doOnNext(reply::append)
                .doOnComplete(() -> log.debug("Chat reply    [{}] assistant: {}", conversationId, reply))
                .doOnError(e -> log.error("Chat failed   [{}]", conversationId, e));
    }

    private void logUsage(String conversationId, ChatResponse response) {
        Usage usage = response.getMetadata().getUsage();
        if (usage.getTotalTokens() > 0) {
            log.info("Chat response [{}] tokens: prompt={} completion={} total={}",
                    conversationId, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }
    }

    private static String readText(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read system prompt resource", e);
        }
    }

}
