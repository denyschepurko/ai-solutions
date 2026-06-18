package com.example.chatbot.api.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvalCase(String id, String category, String input, String criterion) {
}
