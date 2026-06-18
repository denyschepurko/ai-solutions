package com.example.chatbot.api.data.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message, String conversationId) {}
