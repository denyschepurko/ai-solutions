package com.example.chatbot.api.eval;

import java.util.List;

public record JudgeVerdict(
        String reasoning,
        List<String> strengths,
        List<String> weaknesses,
        int score,
        boolean pass) {
}
