package com.smartestate.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Optional Claude-backed generation for the FR-8 chatbot. Disabled by default; the RAG
 * answer is produced by the local extractive engine. When app.ai.claude.enabled=true and
 * ANTHROPIC_API_KEY is set, the retrieved bylaw context is sent to Claude for a fluent,
 * grounded answer. Any failure falls back silently to the local engine.
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    @Value("${app.ai.claude.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.claude.model:claude-opus-4-8}")
    private String model;

    /** 1024 is tight once several bylaws must be synthesised into one answer. */
    @Value("${app.ai.claude.max-tokens:1500}")
    private long maxTokens;

    @Value("${app.ai.claude.history-turns:6}")
    private int historyTurns;

    private static final int MAX_TURN_CHARS = 2000;
    private static final int MAX_HISTORY_CHARS = 4000;

    private volatile AnthropicClient client;

    public boolean isAvailable() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        return enabled && key != null && !key.isBlank();
    }

    public String modelName() {
        return model;
    }

    /** Single-turn generation. */
    public Optional<String> generate(String instructions, String question, String context) {
        return generate(instructions, List.of(), question, context);
    }

    /**
     * Returns Claude's grounded answer, or empty if unavailable / on any error.
     *
     * <p>The retrieved context rides on the <em>final</em> user message only, never in the system
     * prompt, so context retrieved for an earlier question cannot leak forward into this answer.
     */
    public Optional<String> generate(String instructions, List<ChatTurn> history,
                                     String question, String context) {
        if (!isAvailable()) return Optional.empty();
        try {
            AnthropicClient c = client();
            MessageCreateParams.Builder params = MessageCreateParams.builder()
                    .model(resolveModel())
                    .maxTokens(maxTokens)
                    .system(instructions);

            for (ChatTurn t : normalise(history)) {
                if (t.isUser()) params.addUserMessage(t.text());
                else params.addAssistantMessage(t.text());
            }
            params.addUserMessage("Resident question:\n" + question
                    + "\n\nContext (cite facts as [doc-k]):\n" + context);

            Message response = c.messages().create(params.build());
            StringBuilder sb = new StringBuilder();
            response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(t -> sb.append(t.text()));
            String text = sb.toString().trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            log.warn("Claude generation failed, falling back to local engine: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Makes untrusted client history safe to replay: the Messages API requires the first message to
     * be from the user and roles to strictly alternate, neither of which a client array guarantees.
     * Also bounds turn count and total size so history cannot be used to blow up the request.
     */
    private List<ChatTurn> normalise(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) return List.of();

        List<ChatTurn> clean = new ArrayList<>();
        for (ChatTurn t : history) {
            if (t == null || t.text() == null || t.text().isBlank()) continue;
            String text = t.text().length() > MAX_TURN_CHARS
                    ? t.text().substring(0, MAX_TURN_CHARS) : t.text();
            // Collapse consecutive same-role turns; roles must alternate.
            if (!clean.isEmpty() && clean.get(clean.size() - 1).role() == t.role()) {
                ChatTurn prev = clean.remove(clean.size() - 1);
                clean.add(new ChatTurn(t.role(), prev.text() + "\n\n" + text));
            } else {
                clean.add(new ChatTurn(t.role(), text));
            }
        }

        // Keep the most recent turns within budget, then drop a leading assistant turn.
        if (clean.size() > historyTurns) clean = new ArrayList<>(clean.subList(clean.size() - historyTurns, clean.size()));
        int budget = MAX_HISTORY_CHARS;
        int from = clean.size();
        for (int i = clean.size() - 1; i >= 0; i--) {
            budget -= clean.get(i).text().length();
            if (budget < 0) break;
            from = i;
        }
        clean = new ArrayList<>(clean.subList(from, clean.size()));
        while (!clean.isEmpty() && !clean.get(0).isUser()) clean.remove(0);
        return clean;
    }

    private Model resolveModel() {
        // Model.of() passes the configured id straight to the API, so it works for model
        // names newer than this SDK build's typed constants (e.g. claude-opus-4-8).
        return Model.of(model);
    }

    private AnthropicClient client() {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    c = AnthropicOkHttpClient.fromEnv();
                    client = c;
                }
            }
        }
        return c;
    }
}
