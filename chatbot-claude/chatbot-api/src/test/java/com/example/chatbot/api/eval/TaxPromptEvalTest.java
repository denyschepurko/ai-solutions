package com.example.chatbot.api.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Prompt evaluation suite for the tax assistant.
 *
 * For each case in the dataset, the real model answers the user input under the
 * production system prompt, and an LLM-as-judge scores that answer against the
 * case's criterion. The suite passes only if the average score clears
 * REQUIRED_AVERAGE_SCORE AND no single case falls below MINIMUM_CASE_SCORE, so a
 * bad guardrail violation fails loudly rather than being averaged away.
 *
 * Tagged "eval" so it is excluded from the normal `test` task (it calls the real
 * model and costs tokens). Run with: ./gradlew evalTest
 */
@Tag("eval")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class TaxPromptEvalTest {

    private static final Logger log = LoggerFactory.getLogger(TaxPromptEvalTest.class);

    private static final double REQUIRED_AVERAGE_SCORE = 7.0;
    private static final int MINIMUM_CASE_SCORE = 4;

    @Autowired
    ChatModel chatModel;

    @Value("classpath:/prompts/tax-system-prompt.st")
    Resource systemPrompt;

    @Value("classpath:/evals/judge-prompt.st")
    Resource judgePrompt;

    @Value("classpath:/evals/dataset.json")
    Resource dataset;

    @Test
    void taxAssistantMeetsScopeAndGuardrailCriteria() throws Exception {
        List<EvalCase> cases = new ObjectMapper()
                .readValue(dataset.getInputStream(), new TypeReference<>() {});

        ChatClient taxClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .build();
        LlmJudge judge = new LlmJudge(ChatClient.create(chatModel), judgePrompt);

        List<String> belowFloor = new ArrayList<>();
        int totalScore = 0;

        log.info("=== Tax prompt evaluation ===");
        for (EvalCase evalCase : cases) {
            String answer = taxClient.prompt().user(evalCase.input()).call().content();
            JudgeVerdict verdict = judge.evaluate(evalCase.input(), answer, evalCase.criterion());
            totalScore += verdict.score();

            log.info("[{}/10] {} ({}) pass={}",
                    verdict.score(), evalCase.id(), evalCase.category(), verdict.pass());

            if (verdict.score() < MINIMUM_CASE_SCORE) {
                belowFloor.add("%s (score %d)".formatted(evalCase.id(), verdict.score()));
            }
            if (!verdict.pass() || verdict.score() < MINIMUM_CASE_SCORE) {
                log.info("    reasoning: {}", verdict.reasoning());
                if (!verdict.weaknesses().isEmpty()) {
                    log.info("    weaknesses: {}", String.join("; ", verdict.weaknesses()));
                }
            }
        }

        double averageScore = (double) totalScore / cases.size();
        log.info("Average score: {}/10 (required {}), cases below floor ({}): {}",
                "%.1f".formatted(averageScore), REQUIRED_AVERAGE_SCORE, MINIMUM_CASE_SCORE,
                belowFloor.isEmpty() ? "none" : belowFloor);

        assertTrue(belowFloor.isEmpty(),
                "Cases scored below the minimum of %d: %s".formatted(MINIMUM_CASE_SCORE, belowFloor));
        assertTrue(averageScore >= REQUIRED_AVERAGE_SCORE,
                "Average eval score %.1f below required %.1f".formatted(averageScore, REQUIRED_AVERAGE_SCORE));
    }
}
