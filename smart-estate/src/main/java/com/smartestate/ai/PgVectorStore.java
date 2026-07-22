package com.smartestate.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * pgvector-backed {@link VectorStore}. Owns its own schema (created idempotently on
 * startup, independent of Hibernate's entity tables) so the retrieval upgrade adds a
 * table without disturbing the rest of the app. Cosine distance ({@code <=>}) with an
 * HNSW index gives approximate nearest-neighbour search; similarity is returned as
 * {@code 1 - distance}.
 *
 * <p>Only present under the {@code pgvector} profile.
 */
@Component
@Profile("pgvector")
public class PgVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;
    private final int dim;
    private volatile boolean ready = false;

    public PgVectorStore(JdbcTemplate jdbc, @Value("${app.ai.embedding.dim:384}") int dim) {
        this.jdbc = jdbc;
        this.dim = dim;
    }

    @PostConstruct
    void init() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS bylaw_chunk (
                chunk_index INT PRIMARY KEY,
                doc_code    TEXT NOT NULL,
                lang        VARCHAR(2) NOT NULL,
                embedding   vector(%d) NOT NULL
            )""".formatted(dim));
        // Cosine ANN index; matches the <=> operator used at query time.
        jdbc.execute("""
            CREATE INDEX IF NOT EXISTS bylaw_chunk_hnsw
            ON bylaw_chunk USING hnsw (embedding vector_cosine_ops)
            WITH (m = 16, ef_construction = 64)""");
        ready = true;
    }

    @Override
    public boolean isReady() { return ready; }

    @Override
    public void replaceAll(List<ChunkVector> vectors) {
        jdbc.update("TRUNCATE bylaw_chunk");
        if (vectors.isEmpty()) return;
        jdbc.batchUpdate(
                "INSERT INTO bylaw_chunk (chunk_index, doc_code, lang, embedding) "
                        + "VALUES (?, ?, ?, CAST(? AS vector))",
                vectors, vectors.size(),
                (ps, v) -> {
                    ps.setInt(1, v.chunkIndex());
                    ps.setString(2, v.docCode());
                    ps.setString(3, v.language());
                    ps.setString(4, toVectorLiteral(v.embedding()));
                });
    }

    @Override
    public List<Hit> search(float[] queryVector, int k) {
        String q = toVectorLiteral(queryVector);
        // 1 - cosine distance = cosine similarity; order by distance ascending (nearest first).
        return jdbc.query(
                "SELECT chunk_index, 1 - (embedding <=> CAST(? AS vector)) AS sim "
                        + "FROM bylaw_chunk ORDER BY embedding <=> CAST(? AS vector) LIMIT ?",
                (rs, i) -> new Hit(rs.getInt("chunk_index"), rs.getDouble("sim")),
                q, q, k);
    }

    /** pgvector accepts a vector as the text literal {@code [f1,f2,...]}. */
    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
