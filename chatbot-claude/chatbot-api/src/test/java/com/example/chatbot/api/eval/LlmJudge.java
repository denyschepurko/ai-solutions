package com.example.chatbot.api.eval;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;

import java.util.Map;

class LlmJudge {

    private final ChatClient judge;
    private final Resource promptTemplate;

    LlmJudge(ChatClient judge, Resource promptTemplate) {
        this.judge = judge;
        this.promptTemplate = promptTemplate;
    }

    JudgeVerdict evaluate(String question, String response, String criterion) {
        String prompt = new PromptTemplate(promptTemplate).render(Map.of(
                "question", question,
                "response", response,
                "criterion", criterion));
        return judge.prompt()
                .user(prompt)
                .call()
                .entity(JudgeVerdict.class);
    }
}
