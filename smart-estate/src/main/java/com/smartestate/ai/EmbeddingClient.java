package com.smartestate.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the local embedding service (embedding-service/, a multilingual
 * sentence-transformer on :8000) to turn text into dense vectors for the
 * dense/hybrid retriever. Thin and fail-soft: if the service is not configured
 * or not reachable, {@link #isAvailable()} is false and {@link RagService} stays
 * on the offline TF-IDF path. Embeddings are produced locally, with no external AI API.
 */
@Component
public class EmbeddingClient {

    private final String baseUrl;
    private final int dim;
    private final RestClient client;

    public EmbeddingClient(
            @Value("${app.ai.embedding.base-url:}") String baseUrl,
            @Value("${app.ai.embedding.dim:384}") int dim,
            @Value("${app.ai.embedding.timeout-ms:20000}") int timeoutMs) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.dim = dim;
        if (this.baseUrl.isEmpty()) {
            this.client = null;
        } else {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(3000);
            f.setReadTimeout(timeoutMs);
            this.client = RestClient.builder().baseUrl(this.baseUrl).requestFactory(f).build();
        }
    }

    public int dim() { return dim; }

    /** Reachable and loaded? The service only answers /health once its model is loaded. */
    public boolean isAvailable() {
        if (client == null) return false;
        try {
            client.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Embed a search query (e5 "query:" side). Returns null on any failure. */
    public float[] embedQuery(String text) {
        List<float[]> v = embed(List.of(text == null ? "" : text), "query");
        return v.isEmpty() ? null : v.get(0);
    }

    /** Embed corpus passages (e5 "passage:" side). Returns an empty list on failure. */
    public List<float[]> embedPassages(List<String> texts) {
        return embed(texts, "passage");
    }

    private List<float[]> embed(List<String> texts, String inputType) {
        if (client == null || texts == null || texts.isEmpty()) return List.of();
        try {
            EmbedResponse r = client.post()
                    .uri("/embed")
                    .body(new EmbedRequest(texts, inputType))
                    .retrieve()
                    .body(EmbedResponse.class);
            if (r == null || r.vectors() == null) return List.of();
            List<float[]> out = new ArrayList<>(r.vectors().size());
            for (List<Float> row : r.vectors()) {
                float[] a = new float[row.size()];
                for (int i = 0; i < a.length; i++) a[i] = row.get(i);
                out.add(a);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private record EmbedRequest(List<String> texts, String input_type) {}

    private record EmbedResponse(String model, int dim, List<List<Float>> vectors) {}
}
