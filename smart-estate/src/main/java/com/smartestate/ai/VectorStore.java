package com.smartestate.ai;

import java.util.List;

/**
 * A dense-vector index over the bylaw corpus, used by the dense/hybrid retriever.
 * Chunks are addressed by their index in {@link RagService}'s current chunk list;
 * the store is fully replaced on each corpus rebuild so ids always line up.
 *
 * <p>Implemented by {@link PgVectorStore} (Postgres + pgvector) under the
 * {@code pgvector} profile; absent in the default profile, where retrieval stays
 * on the offline TF-IDF path.
 */
public interface VectorStore {

    /** Replace the whole index with the given chunk vectors (clear + bulk insert). */
    void replaceAll(List<ChunkVector> vectors);

    /** Approximate nearest neighbours: the {@code k} most similar chunks to {@code queryVector}. */
    List<Hit> search(float[] queryVector, int k);

    /** True once the backing schema is ready to accept vectors. */
    boolean isReady();

    /** One corpus chunk and its embedding. */
    record ChunkVector(int chunkIndex, String docCode, String language, float[] embedding) {}

    /** A search result: which chunk, and its cosine similarity in [0,1]. */
    record Hit(int chunkIndex, double similarity) {}
}
