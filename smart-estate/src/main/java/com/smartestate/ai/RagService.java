package com.smartestate.ai;

import com.smartestate.model.BylawDoc;
import com.smartestate.model.Language;
import com.smartestate.repo.BylawDocRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Bilingual RAG over the bylaw/FAQ corpus (FR-8.1). TF-IDF retrieval, top-K by similarity,
 * [doc-k] citations (rp.pdf §2). Claude (when enabled) always answers; the offline engine
 * cites passages when found or shows a help message.
 */
@Service
public class RagService {

    /** Citation code for the signed-in resident's own record, as opposed to a bylaw document. */
    static final String ACCOUNT_DOC_CODE = "ACCOUNT";

    private final BylawDocRepository bylawRepo;
    private final ClaudeClient claudeClient;
    private final AccountContextService accountContext;

    /** Dense-retrieval collaborators. {@code vectorStore} is null unless the pgvector profile is
     *  active; when either is unavailable, retrieval stays on the offline TF-IDF path. */
    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;
    private volatile boolean denseReady = false;

    private enum Mode { LEXICAL, DENSE, HYBRID }

    @Value("${app.ai.rag.top-k:4}")
    private int topK;

    @Value("${app.ai.rag.threshold:0.06}")
    private double threshold;

    /** Chunks admitted per document, so top-K spans several bylaws instead of one. */
    @Value("${app.ai.rag.max-chunks-per-doc:1}")
    private int maxChunksPerDoc;

    /** Prior user turns folded into retrieval for a follow-up question. */
    @Value("${app.ai.rag.history-turns:2}")
    private int historyTurns;

    /** Retrieval strategy: lexical (TF-IDF baseline) | dense (embeddings) | hybrid (RRF fusion). */
    @Value("${app.ai.retrieval.mode:lexical}")
    private String retrievalMode;

    /** Dense candidates pulled before fusion / packaging. */
    @Value("${app.ai.retrieval.dense-candidates:20}")
    private int denseCandidates;

    /** Reciprocal Rank Fusion constant (Cormack et al., 2009). */
    @Value("${app.ai.retrieval.rrf-k:60}")
    private int rrfK;

    private static final List<String> CONTINUATION_CUES = List.of(
            "what about", "how about", "and ", "also ", "then ", "what if", "why not",
            "那", "呢", "咁", "還有", "那么", "那麼");

    /** Cues that a question asks for judgement, not a lookup. */
    private static final List<String> JUDGEMENT_CUES = List.of(
            "should i", "what if", "can i still", "is it ok", "am i allowed", "what are my rights",
            "who is responsible", "who pays", "what happens if", "how do i deal",
            "應該", "是否可以", "可唔可以", "怎麼辦", "怎办", "點算", "有咩權", "誰負責", "誰付",
            "甚麼權利", "什麼權利", "有權", "如何處理", "怎樣處理", "可以投訴", "誰要負責", "邊個負責");

    private final Tfidf index = new Tfidf();
    private final List<Chunk> chunks = new ArrayList<>();
    private volatile boolean built = false;

    private static final String CLAUDE_INSTRUCTIONS =
            "You are the friendly assistant for Evergreen Bay Estate, a Hong Kong residential estate, inside the "
            + "Smart Estate building-management system. Talk to residents the way a warm, capable estate concierge "
            + "would — natural, conversational and reassuring, never robotic or form-letter. Help with anything about "
            + "estate rules, fees, maintenance, facilities and daily living in the estate. Guidelines:\n"
            + "- Sound like a real person in a chat: lead with a direct, plain-language answer in a sentence or two, "
            + "then add only the detail that matters. Contractions and a friendly opener are good; walls of text and "
            + "bureaucratic phrasing are not.\n"
            + "- When the provided context is relevant, base your answer on it and cite the passages you use as [doc-k], "
            + "but weave the citation into the sentence — don't just paste quotes and bracket numbers.\n"
            + "- When the context does not fully cover the question, still give a helpful, sensible answer using general "
            + "knowledge of Hong Kong residential estate management, and note that the resident can confirm official "
            + "matters with the management office (Evergreen Property Management Ltd., 3000 0000).\n"
            + "- If the resident just greets you or makes small talk, reply warmly and invite their question.\n"
            + "- If the question is genuinely ambiguous, ask one short clarifying question instead of guessing — that is "
            + "part of sounding conversational. Otherwise just answer.\n"
            + "- End when it helps by offering the natural next step (\"want me to check your balance?\"), so the "
            + "conversation keeps flowing rather than dead-ending.\n"
            + "- Reply in the resident's language (English or 中文), matching their register, and keep it concise.\n"
            + "- Only decline if the request is clearly unrelated to the estate or inappropriate.\n"
            + "- This is a conversation: resolve pronouns and ellipsis (\"what about on Sundays?\") against "
            + "what was already asked, and don't repeat context the resident already has.\n"
            + "- When a question spans several bylaws, synthesise across them into one answer and cite each "
            + "passage you actually used — do not simply recite the single closest one.\n"
            + "- For situational questions (\"what should I do if…\"), answer in three beats: the rule that "
            + "applies, the action you recommend, then the escalation path if that doesn't resolve it.\n"
            + "- A context passage labelled \"Your account\" is the signed-in resident's own live record. "
            + "Use its exact figures when they are relevant, cite it as [doc-k] like any other source, and "
            + "never guess at balances, dates or case numbers that it does not state.";

    public RagService(BylawDocRepository bylawRepo, ClaudeClient claudeClient,
                      AccountContextService accountContext,
                      ObjectProvider<VectorStore> vectorStoreProvider, EmbeddingClient embeddingClient) {
        this.bylawRepo = bylawRepo;
        this.claudeClient = claudeClient;
        this.accountContext = accountContext;
        this.vectorStore = vectorStoreProvider.getIfAvailable();   // present only under the pgvector profile
        this.embeddingClient = embeddingClient;
    }

    private Mode configuredMode() {
        if (retrievalMode == null) return Mode.LEXICAL;
        return switch (retrievalMode.trim().toLowerCase(Locale.ROOT)) {
            case "dense"  -> Mode.DENSE;
            case "hybrid" -> Mode.HYBRID;
            default        -> Mode.LEXICAL;
        };
    }

    /** The mode that will actually run: falls back to lexical unless the dense index is loaded. */
    private Mode effectiveMode() {
        Mode m = configuredMode();
        return (m != Mode.LEXICAL && denseReady && vectorStore != null) ? m : Mode.LEXICAL;
    }

    /** Build the index once the corpus is seeded (after CommandLineRunners have run). */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        rebuild();
    }

    /** Rebuild the index after a new document is ingested (FR-8.2). */
    public synchronized void rebuild() {
        chunks.clear();
        for (BylawDoc doc : bylawRepo.findAllByOrderByCodeAsc()) {
            for (String piece : chunk(doc.getContent())) {
                // Language is detected per CHUNK, not inherited from the document: every bylaw is
                // now written in both English and 繁體中文, so a document-level tag would mislabel
                // half its passages. RetrievedChunk.language is then the language of the passage
                // actually cited.
                chunks.add(new Chunk(doc.getCode(), doc.getTitle(), languageOf(piece), piece));
            }
        }
        List<String> corpus = new ArrayList<>();
        for (Chunk c : chunks) corpus.add(c.title + ". " + c.text);
        index.fit(corpus);
        built = true;
        buildDenseIndex();
    }

    /**
     * When the pgvector profile and the embedding service are both available, embed every chunk and
     * load the vector store so the dense/hybrid retriever can run. Fail-soft: any problem leaves
     * {@code denseReady=false} and retrieval stays on the TF-IDF path, so a missing embedding service
     * never breaks the assistant.
     */
    private synchronized void buildDenseIndex() {
        denseReady = false;
        if (vectorStore == null || configuredMode() == Mode.LEXICAL || !embeddingClient.isAvailable()) return;
        try {
            List<String> texts = new ArrayList<>(chunks.size());
            for (Chunk c : chunks) texts.add(c.text());
            List<float[]> vecs = embeddingClient.embedPassages(texts);
            if (vecs.size() != chunks.size()) return;
            List<VectorStore.ChunkVector> rows = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Chunk c = chunks.get(i);
                rows.add(new VectorStore.ChunkVector(
                        i, c.docCode(), c.language() == Language.ZH ? "ZH" : "EN", vecs.get(i)));
            }
            vectorStore.replaceAll(rows);
            denseReady = true;
        } catch (Exception e) {
            denseReady = false;   // on any failure, stay on TF-IDF
        }
    }

    private void ensureBuilt() {
        if (!built || chunks.isEmpty()) rebuild();
    }

    /** Single-turn ask, kept for the plain form post and any caller without a conversation. */
    public RagAnswer ask(String query) {
        return ask(query, List.of());
    }

    public RagAnswer ask(String query, List<ChatTurn> history) {
        ensureBuilt();
        RagAnswer answer = new RagAnswer();
        answer.query = query;   // always the RAW question; the UI echoes this back
        if (query == null || query.isBlank()) {
            answer.answer = helpMessage();
            return answer;
        }

        // Retrieve citable context (top-K above threshold); may be empty for general questions.
        String retrievalText = retrievalText(query, history);
        List<RetrievedChunk> sources = retrieveSources(retrievalText);

        // Ground in the resident's own record when the question is about it. Prepended and NOT
        // counted against top-K, so personalisation never evicts a bylaw source. Labels are
        // reassigned afterwards so the [doc-k] chips stay contiguous.
        String accountFacts = accountContext.isRelevant(retrievalText) ? accountContext.forCurrentUser() : null;
        if (accountFacts != null) {
            RetrievedChunk acc = new RetrievedChunk();
            acc.docCode = ACCOUNT_DOC_CODE;
            acc.title = accountContext.sourceTitle();
            acc.snippet = accountFacts;
            acc.score = 1.0;    // an exact record, not a similarity match
            acc.language = languageOf(query);   // tag it like any other cited passage
            sources.add(0, acc);
            relabel(sources);
        }
        answer.sources = sources;
        answer.retrieval = effectiveMode().name().toLowerCase(Locale.ROOT);

        // Claude mode: always answer helpfully rather than gating on retrieval.
        if (claudeClient.isAvailable()) {
            String context = sources.isEmpty()
                    ? "(No specific estate bylaw passage closely matched this question — use general knowledge.)"
                    : buildContext(sources);
            Optional<String> generated = claudeClient.generate(CLAUDE_INSTRUCTIONS, history, query, context);
            if (generated.isPresent()) {
                answer.answer = generated.get();
                answer.mode = "Claude (" + claudeClient.modelName() + ")";
                return answer;
            }
            // Claude errored; fall through to the local engine.
        }

        // Local (offline) mode. The extractive engine can only surface passages, so when the question
        // needs reasoning it says so rather than implying a synthesis it never performed.
        boolean limited = beyondExtractive(query, history, sources, accountFacts != null);
        answer.mode = limited ? "Local (extractive TF-IDF) · limited" : "Local (extractive TF-IDF)";
        if (sources.isEmpty()) {
            answer.answer = limited ? limitationNotice(query) + "\n\n" + helpMessage() : helpMessage();
        } else {
            answer.answer = limited ? limitedAnswer(query, sources) : extractiveAnswer(query, sources);
        }
        return answer;
    }

    /**
     * True when the question needs reasoning the extractive engine cannot do. Leaves `refused`
     * alone (the UI's error channel); the limitation is carried in the answer prose and the
     * `mode` string instead.
     */
    private boolean beyondExtractive(String query, List<ChatTurn> history,
                                     List<RetrievedChunk> sources, boolean hasAccount) {
        if (history != null && !history.isEmpty()) return true;   // no notion of a thread
        if (hasAccount) return true;                              // cannot combine a rule with a balance
        // Not keyed on how many documents matched: per-document diversification makes almost every
        // answer span several, so document count would flag plain lookups as "limited". What needs
        // synthesis is the shape of the QUESTION.
        String q = query.toLowerCase(Locale.ROOT);
        for (String cue : JUDGEMENT_CUES) {
            if (q.contains(cue)) return true;
        }
        long parts = query.chars().filter(ch -> ch == '?' || ch == '？').count();
        return parts >= 2;
    }

    private boolean isChinese(String text) {
        return text != null && text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private String limitationNotice(String query) {
        return isChinese(query)
                ? "我盡量幫你。現時我在離線模式，未能替你綜合分析或結合帳戶情況作判斷，"
                  + "不過我先把最相關的條文找出來給你參考："
                : "Happy to help with this. I'm in offline mode just now, so rather than piecing the rules "
                + "together for you, let me pull up the passages that speak to it most directly:";
    }

    /** Passages plus an explicit statement of what was NOT done, and a route to a human. */
    private String limitedAnswer(String query, List<RetrievedChunk> sources) {
        boolean zh = isChinese(query);
        StringBuilder sb = new StringBuilder(limitationNotice(query));

        // The account record is a fact, not a bylaw passage, so state it as one. Printing it under
        // "here is what the estate documents say" would read as though the resident's own balance
        // were an excerpt from a rulebook.
        sources.stream().filter(r -> ACCOUNT_DOC_CODE.equals(r.docCode)).findFirst().ifPresent(acc ->
                sb.append(zh ? "\n\n您的帳戶紀錄 [" : "\n\nYour account record [")
                        .append(acc.label).append("]:\n").append(acc.snippet).append("\n"));

        List<RetrievedChunk> bylaws = sources.stream()
                .filter(r -> !ACCOUNT_DOC_CODE.equals(r.docCode))
                .toList();
        if (!bylaws.isEmpty()) {
            sb.append(zh ? "\n屋苑文件的相關條文如下：\n\n" : "\nHere is what the estate documents say:\n\n");
            for (RetrievedChunk r : bylaws) {
                sb.append("[").append(r.label).append("] ").append(r.snippet).append("\n\n");
            }
        }
        sb.append(zh
                ? "如需就此作出決定，請聯絡管理處（常青灣物業管理有限公司，3000 0000）。"
                : "For a decision on this, please contact the management office "
                + "(Evergreen Property Management Ltd., 3000 0000).");
        return sb.toString();
    }

    // ---- conversational retrieval ------------------------------------------

    /**
     * The text vectorised for retrieval. A short follow-up ("What about on Sundays?") carries little
     * signal alone, so recent user turns are folded in; {@link Tfidf} being bag-of-words,
     * concatenating them approximates blending their vectors. Folded only when the query looks
     * context-dependent, else a well-formed question is diluted toward the threshold. Only
     * <em>user</em> turns are used: offline, the assistant text is a verbatim copy of retrieved
     * passages, so feeding it back would lock retrieval onto what it already found.
     */
    private String retrievalText(String query, List<ChatTurn> history) {
        if (history == null || history.isEmpty() || !looksContextDependent(query)) return query;

        List<String> recent = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && recent.size() < historyTurns; i--) {
            ChatTurn t = history.get(i);
            if (t != null && t.isUser() && t.text() != null && !t.text().isBlank()) {
                recent.add(t.text());
            }
        }
        if (recent.isEmpty()) return query;
        Collections.reverse(recent);
        return String.join(" ", recent) + " " + query;
    }

    /** Continuation cues and very short queries mark a question that cannot stand on its own. */
    private boolean looksContextDependent(String query) {
        if (Tfidf.tokenize(query).size() <= 6) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (String cue : CONTINUATION_CUES) {
            if (q.startsWith(cue)) return true;
        }
        return false;
    }

    /**
     * Retrieve citable context using the active strategy (lexical / dense / hybrid). Dense and
     * hybrid fall back to lexical whenever the embedding service or a query embedding is
     * unavailable, so this method always returns a usable ranking.
     */
    private List<RetrievedChunk> retrieveSources(String query) {
        if (query == null || query.isBlank() || chunks.isEmpty()) return List.of();
        Mode mode = effectiveMode();
        if (mode == Mode.LEXICAL) return lexicalSources(query);

        float[] qv = embeddingClient.embedQuery(query);
        if (qv == null) return lexicalSources(query);
        List<VectorStore.Hit> hits = vectorStore.search(qv, Math.max(denseCandidates, topK));
        if (hits.isEmpty()) return lexicalSources(query);

        Map<Integer, Double> simByIdx = new HashMap<>();
        List<Integer> denseOrder = new ArrayList<>();
        for (VectorStore.Hit h : hits) {
            if (h.chunkIndex() >= 0 && h.chunkIndex() < chunks.size()) {
                simByIdx.put(h.chunkIndex(), h.similarity());
                denseOrder.add(h.chunkIndex());   // already sorted nearest-first
            }
        }
        if (denseOrder.isEmpty()) return lexicalSources(query);

        List<Integer> ranked;
        Map<Integer, Double> displayScore;
        if (mode == Mode.DENSE) {
            ranked = denseOrder;
            displayScore = simByIdx;
        } else {
            // HYBRID: Reciprocal Rank Fusion of the lexical and dense rankings.
            double[] lex = lexicalScores(query);
            Integer[] lexOrder = new Integer[chunks.size()];
            for (int i = 0; i < lexOrder.length; i++) lexOrder[i] = i;
            Arrays.sort(lexOrder, (a, b) -> Double.compare(lex[b], lex[a]));

            Map<Integer, Integer> lexRank = new HashMap<>();
            for (int r = 0; r < Math.min(lexOrder.length, denseCandidates); r++) lexRank.put(lexOrder[r], r + 1);
            Map<Integer, Integer> denseRank = new HashMap<>();
            for (int r = 0; r < denseOrder.size(); r++) denseRank.put(denseOrder.get(r), r + 1);

            Set<Integer> cand = new LinkedHashSet<>();
            cand.addAll(denseRank.keySet());
            cand.addAll(lexRank.keySet());
            Map<Integer, Double> rrf = new HashMap<>();
            for (int idx : cand) {
                double s = 0;
                if (lexRank.containsKey(idx))   s += 1.0 / (rrfK + lexRank.get(idx));
                if (denseRank.containsKey(idx)) s += 1.0 / (rrfK + denseRank.get(idx));
                rrf.put(idx, s);
            }
            ranked = new ArrayList<>(cand);
            ranked.sort((a, b) -> Double.compare(rrf.get(b), rrf.get(a)));
            // Show the semantic similarity where we have it (else the lexical cosine): a meaningful
            // 0-1 value for the UI's confidence bar; the raw RRF score is not on that scale.
            displayScore = new HashMap<>();
            for (int idx : ranked) displayScore.put(idx, simByIdx.getOrDefault(idx, Math.min(1.0, lex[idx])));
        }
        return packageRanked(ranked, displayScore);
    }

    /** The TF-IDF baseline retriever (top-K above the similarity threshold, capped per document). */
    private List<RetrievedChunk> lexicalSources(String query) {
        if (query == null || query.isBlank() || chunks.isEmpty()) return List.of();
        double[] scores = lexicalScores(query);
        Integer[] order = new Integer[chunks.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        // Pass 1: best chunks, capped per document so one doc cannot consume the whole budget.
        // Cross-document synthesis needs breadth, not several chunks of the same bylaw.
        List<RetrievedChunk> sources = new ArrayList<>();
        Map<String, Integer> perDoc = new HashMap<>();
        List<Integer> deferred = new ArrayList<>();
        for (int rank = 0; rank < order.length && sources.size() < topK; rank++) {
            int i = order[rank];
            if (scores[i] < threshold) break;
            Chunk c = chunks.get(i);
            if (perDoc.getOrDefault(c.docCode(), 0) >= maxChunksPerDoc) {
                deferred.add(i);
                continue;
            }
            perDoc.merge(c.docCode(), 1, Integer::sum);
            sources.add(toRetrieved(c, scores[i]));
        }
        // Pass 2: a narrow single-document question shouldn't be starved of its other passages.
        // `deferred` was filled in rank order and every entry already cleared the threshold.
        for (int i : deferred) {
            if (sources.size() >= topK) break;
            sources.add(toRetrieved(chunks.get(i), scores[i]));
        }

        relabel(sources);
        return sources;
    }

    /** Cosine similarity of the query against every chunk's TF-IDF vector. */
    private double[] lexicalScores(String query) {
        Map<String, Double> qv = index.vectorize(query);
        double[] scores = new double[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) scores[i] = Tfidf.cosine(qv, index.docVector(i));
        return scores;
    }

    /** Apply the per-document cap + top-K budget to a ranked chunk list, then relabel [doc-k]. */
    private List<RetrievedChunk> packageRanked(List<Integer> orderedIdx, Map<Integer, Double> displayScore) {
        List<RetrievedChunk> sources = new ArrayList<>();
        Map<String, Integer> perDoc = new HashMap<>();
        List<Integer> deferred = new ArrayList<>();
        for (int i : orderedIdx) {
            if (sources.size() >= topK) break;
            Chunk c = chunks.get(i);
            if (perDoc.getOrDefault(c.docCode(), 0) >= maxChunksPerDoc) {
                deferred.add(i);
                continue;
            }
            perDoc.merge(c.docCode(), 1, Integer::sum);
            sources.add(toRetrieved(c, displayScore.getOrDefault(i, 0.0)));
        }
        for (int i : deferred) {
            if (sources.size() >= topK) break;
            sources.add(toRetrieved(chunks.get(i), displayScore.getOrDefault(i, 0.0)));
        }
        relabel(sources);
        return sources;
    }

    private RetrievedChunk toRetrieved(Chunk c, double score) {
        RetrievedChunk rc = new RetrievedChunk();
        rc.docCode = c.docCode();
        rc.title = c.title();
        rc.snippet = c.text();
        rc.language = c.language();
        rc.score = Math.round(score * 1000) / 1000.0;
        return rc;
    }

    /**
     * Numbers the citations in one pass, after the list is fully assembled. Labels must be assigned
     * here rather than during construction: the account chunk is prepended later, and the frontend
     * resolves inline [doc-k] chips by exact label match, so a stale number renders a chip that
     * highlights nothing.
     */
    private void relabel(List<RetrievedChunk> sources) {
        for (int i = 0; i < sources.size(); i++) {
            sources.get(i).label = "doc-" + (i + 1);
        }
    }

    /** Friendly fallback that points the resident at what they CAN ask, instead of a flat refusal. */
    private String helpMessage() {
        Set<String> cats = new LinkedHashSet<>();
        for (BylawDoc d : bylawRepo.findAllByOrderByCodeAsc()) {
            if (d.getCategory() != null && !d.getCategory().isBlank()) cats.add(d.getCategory());
        }
        String topics = cats.isEmpty() ? "estate rules and FAQs" : String.join(", ", cats);
        return "Hi! I'm the Evergreen Bay Estate assistant. I can help with questions about: " + topics + ".\n\n"
                + "For example, try: \"What are the quiet hours?\", \"What happens if my management fee is late?\", "
                + "or \"Can I keep a dog?\".\n\n"
                + "For anything official, please contact the management office (Evergreen Property Management Ltd., 3000 0000).\n\n"
                + "———\n\n"
                + "您好！我是常青灣屋苑的智能助理，可以解答有關屋苑規則、管理費、裝修、噪音、消防安全及會議等問題。\n\n"
                + "例如可以問我：「安寧時段是幾點？」、「管理費逾期會怎樣？」或「我可以養狗嗎？」。\n\n"
                + "如涉及正式事宜，請聯絡管理處（常青灣物業管理有限公司，3000 0000）。";
    }

    /**
     * Doc codes actually placed in {@code sources[]} for this question (threshold, top-K and
     * per-document cap applied). Distinct from {@link #rankedDocCodes}, which dedupes by document
     * and applies neither, so it never sees the per-document diversification.
     */
    public List<String> servedDocCodes(String query, List<ChatTurn> history) {
        ensureBuilt();
        return retrieveSources(retrievalText(query, history)).stream()
                .map(RetrievedChunk::getDocCode)
                .distinct()
                .toList();
    }

    /** Distinct doc codes in descending similarity order (no threshold); for retrieval evaluation. */
    public List<String> rankedDocCodes(String query) {
        return rankedDocCodes(query, List.of());
    }

    /** Ranking for a follow-up question, with prior turns folded into the retrieval text. */
    public List<String> rankedDocCodes(String query, List<ChatTurn> history) {
        ensureBuilt();
        query = retrievalText(query, history);
        if (query == null || query.isBlank() || chunks.isEmpty()) return List.of();
        Map<String, Double> qv = index.vectorize(query);
        double[] scores = new double[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) scores[i] = Tfidf.cosine(qv, index.docVector(i));
        Integer[] order = new Integer[chunks.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));
        List<String> docs = new ArrayList<>();
        for (int rank = 0; rank < order.length; rank++) {
            String code = chunks.get(order[rank]).docCode();
            if (!docs.contains(code)) docs.add(code);
        }
        return docs;
    }

    private String buildContext(List<RetrievedChunk> sources) {
        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk r : sources) {
            sb.append("[").append(r.label).append("] ").append(r.title).append(": ").append(r.snippet).append("\n");
        }
        return sb.toString();
    }

    /**
     * Answer the resident in the language they asked in. The offline engine only surfaces the
     * passages it retrieved and cannot paraphrase; the framing around them is written to read
     * naturally rather than as a raw dump.
     */
    private String extractiveAnswer(String query, List<RetrievedChunk> sources) {
        boolean zh = isChinese(query);
        boolean many = sources.size() > 1;
        StringBuilder sb = new StringBuilder(zh
                ? (many ? "好的，我幫你查一查——屋苑守則裡有幾段相關的內容：\n\n"
                        : "好的，我幫你查一查——屋苑守則裡是這樣說的：\n\n")
                : (many ? "Sure — I looked this up, and a few passages in the estate handbook are relevant:\n\n"
                        : "Sure — I looked this up. Here's what the estate handbook says:\n\n"));
        for (RetrievedChunk r : sources) {
            sb.append("[").append(r.label).append("] ").append(r.snippet).append("\n\n");
        }
        sb.append(zh
                ? "希望幫到你！如有其他疑問可以隨時追問；正式事宜亦可聯絡管理處"
                  + "（常青灣物業管理有限公司，3000 0000）。"
                : "Hope that helps! Feel free to ask a follow-up, or contact the management office "
                  + "(Evergreen Property Management Ltd., 3000 0000) for anything official.");
        return sb.toString();
    }

    /** A passage is Chinese when Han characters dominate its letter content. */
    private Language languageOf(String text) {
        long han = text.codePoints()
                .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN).count();
        long latin = text.codePoints().filter(Character::isLetter).count() - han;
        return han > latin ? Language.ZH : Language.EN;
    }

    /** Split text into ~400-char chunks on sentence-ish boundaries. */
    private List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        // Chinese writes no space after 。！？, so requiring trailing whitespace (as the original
        // pattern did) never split Chinese at all: a whole Chinese section became one oversized
        // chunk whose TF-IDF vector was diluted across too many terms. Split on CJK
        // terminators with or without following whitespace.
        String[] parts = text.split("(?<=[.!?])\\s+|(?<=[。！？])\\s*");
        StringBuilder cur = new StringBuilder();
        Language curLang = null;
        for (String p : parts) {
            if (p.isBlank()) continue;
            Language pLang = languageOf(p);
            // Break at a language boundary as well as at the size limit. Every bylaw is written in
            // both languages, so without this the last English sentence and the first Chinese one
            // merge into a mixed chunk, which blurs the language tag and puts both languages'
            // terms in one vector, making each language's passage harder to match.
            boolean langBreak = curLang != null && pLang != curLang;
            if (cur.length() > 0 && (langBreak || cur.length() + p.length() > 400)) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            }
            cur.append(p).append(" ");
            curLang = pLang;
        }
        if (cur.length() > 0) out.add(cur.toString().trim());
        return out;
    }

    // ---- value types ------------------------------------------------------

    private record Chunk(String docCode, String title, Language language, String text) {}

    public static class RagAnswer {
        public String query;
        public String answer;
        public boolean refused;
        public String mode = "Local (extractive TF-IDF)";
        /** Which retriever produced the sources: lexical | dense | hybrid. */
        public String retrieval = "lexical";
        public List<RetrievedChunk> sources = new ArrayList<>();

        public String getQuery() { return query; }
        public String getAnswer() { return answer; }
        public boolean isRefused() { return refused; }
        public String getMode() { return mode; }
        public String getRetrieval() { return retrieval; }
        public List<RetrievedChunk> getSources() { return sources; }
    }

    public static class RetrievedChunk {
        public String label;
        public String docCode;
        public String title;
        public String snippet;
        public double score;
        public Language language;

        public String getLabel() { return label; }
        public String getDocCode() { return docCode; }
        public String getTitle() { return title; }
        public String getSnippet() { return snippet; }
        public double getScore() { return score; }
        public Language getLanguage() { return language; }
    }
}
