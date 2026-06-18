# chatbot-api

Spring Boot backend for a **domain-agnostic streaming chatbot**. It wraps the Anthropic
Claude API with token streaming, per-conversation memory, a configurable system prompt,
and an LLM-as-judge evaluation harness. The domain is set by configuration — this repo
ships it configured as **TaxInfoBot** (general U.S. federal tax Q&A) as a worked example.

## Stack

- Java 21, Spring Boot 4.1
- [Spring AI](https://docs.spring.io/spring-ai/reference/) 2.0 (`spring-ai-starter-model-anthropic`)
- Gradle

## API

A single streaming endpoint:

```
POST /api
Content-Type: application/json

{ "message": "What is a W-2 form?", "conversationId": "<optional>" }
```

- Responds with `text/event-stream` (Server-Sent Events), one `data:` frame per token.
- The server owns the conversation id. Omit `conversationId` on the first turn; the
  response returns a generated one in the **`X-Conversation-Id`** header, which the
  client echoes back on later turns to continue the same memory thread.

## Configuration

Settings live in [`application.yaml`](src/main/resources/application.yaml):

| Key | Purpose |
| --- | --- |
| `spring.ai.anthropic.api-key` | Reads `ANTHROPIC_API_KEY` from the environment |
| `spring.ai.anthropic.chat.options.model` | Claude model id |
| `chatbot.system-prompt` | Classpath resource for the system prompt |
| `chatbot.max-messages` | Sliding window size for chat memory |

**Retargeting to another domain** is a matter of pointing `chatbot.system-prompt` at a
different prompt and updating the evaluation dataset — no code changes.

The system prompt is a [StringTemplate](src/main/resources/prompts/tax-system-prompt.st)
(`.st`) file. Today's date is injected into it per-request as `{current_date}`, so a
time-sensitive assistant can reason about "now" instead of guessing.

`ANTHROPIC_API_KEY` must be set in the environment before running.

## Run

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

## Tests

```bash
./gradlew test       # unit/slice tests — mocked, no API calls
./gradlew evalTest   # prompt evaluation suite — calls the real model (costs tokens)
```

### Prompt evaluation harness

`evalTest` is tagged `eval` and excluded from the normal `test` task. For each case in
[`dataset.json`](src/test/resources/evals/dataset.json), the real model answers under
the production system prompt and an **LLM-as-judge** scores the answer against the
case's criterion. The suite passes only if the average score clears a threshold **and**
no single case falls below a floor — so a guardrail violation fails loudly rather than
being averaged away. Swap the dataset to evaluate your own domain's prompt.

## Disclaimer

Demo for testing and educational purposes only — not for commercial use. In its
TaxInfoBot configuration it does not provide professional tax advice.
