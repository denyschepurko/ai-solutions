package com.example.chatbot.api.controller;

import com.example.chatbot.api.config.ChatMemoryConfig;
import com.example.chatbot.api.config.ChatbotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotApiController.class)
@EnableConfigurationProperties(ChatbotProperties.class)
@Import(ChatMemoryConfig.class)
class ChatbotApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @TestConfiguration
    static class MockConfig {
        @Bean
        ChatClient.Builder chatClientBuilder() {
            return mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS);
        }
    }

    @Test
    void streamsAssistantResponseInChunks() throws Exception {
        ChatResponse first = new ChatResponse(List.of(new Generation(new AssistantMessage("Hello "))));
        ChatResponse second = new ChatResponse(List.of(new Generation(new AssistantMessage("from Claude"))));

        when(chatClientBuilder
                .defaultAdvisors(any(Advisor.class)).build()
                .prompt().system(any(Consumer.class)).user(anyString()).advisors(any(Consumer.class))
                .stream().chatResponse())
                .thenReturn(Flux.just(first, second));

        MvcResult result = mockMvc.perform(post("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Conversation-Id"))
                .andExpect(content().string(containsString("Hello ")))
                .andExpect(content().string(containsString("from Claude")));
    }

    @Test
    void returnsBadRequestForBlankMessage() throws Exception {
        mockMvc.perform(post("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
