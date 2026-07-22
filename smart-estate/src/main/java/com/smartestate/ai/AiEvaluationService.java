package com.smartestate.ai;

import com.smartestate.model.Issue;
import com.smartestate.model.IssueCategory;
import com.smartestate.repo.IssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Offline evaluation of the AI module (rp.pdf §5): k-NN classification accuracy on a held-out
 * split, and retrieval Recall@K / MRR over a fixed bilingual question set. Numbers are
 * indicative on a small test set (the caveat rp.pdf §6 states).
 */
@Service
public class AiEvaluationService {

    private final IssueRepository issueRepository;
    private final RagService ragService;

    @Value("${app.ai.knn.k:5}")
    private int k;

    public AiEvaluationService(IssueRepository issueRepository, RagService ragService) {
        this.issueRepository = issueRepository;
        this.ragService = ragService;
    }

    // ---- classification: train/test split, k-NN accuracy --------------------

    public ClassificationEval evaluateClassification() {
        List<Issue> labeled = new ArrayList<>();
        for (Issue i : issueRepository.findAll()) {
            if (i.getCategory() != null && i.getDescription() != null && !i.getDescription().isBlank()) {
                labeled.add(i);
            }
        }
        labeled.sort(Comparator.comparing(Issue::getId));

        List<Issue> trainSet = new ArrayList<>();
        List<Issue> testSet = new ArrayList<>();
        for (int idx = 0; idx < labeled.size(); idx++) {
            (idx % 4 == 0 ? testSet : trainSet).add(labeled.get(idx)); // deterministic 25% test
        }

        Tfidf index = new Tfidf(false); // TF-only space, matching the classifier
        List<String> corpus = new ArrayList<>();
        for (Issue t : trainSet) corpus.add(t.getDescription());
        index.fit(corpus);
        List<Map<String, Double>> trainVecs = new ArrayList<>();
        for (int i = 0; i < trainSet.size(); i++) trainVecs.add(index.docVector(i));

        ClassificationEval eval = new ClassificationEval();
        eval.k = Math.min(k, Math.max(1, trainSet.size()));
        eval.trainSize = trainSet.size();
        Map<IssueCategory, int[]> perCat = new EnumMap<>(IssueCategory.class); // [total, correct]

        for (Issue test : testSet) {
            IssueCategory predicted = knnPredict(index, trainVecs, trainSet, test.getDescription(), eval.k);
            boolean correct = predicted == test.getCategory();
            if (correct) eval.correct++;
            eval.n++;
            perCat.computeIfAbsent(test.getCategory(), c -> new int[2]);
            perCat.get(test.getCategory())[0]++;
            if (correct) perCat.get(test.getCategory())[1]++;

            Example ex = new Example();
            ex.refNumber = test.getRefNumber();
            ex.actual = test.getCategory();
            ex.predicted = predicted;
            ex.correct = correct;
            eval.examples.add(ex);
        }
        eval.accuracyPct = eval.n == 0 ? 0 : Math.round(eval.correct * 100f / eval.n);
        for (Map.Entry<IssueCategory, int[]> e : perCat.entrySet()) {
            PerCategory pc = new PerCategory();
            pc.category = e.getKey();
            pc.total = e.getValue()[0];
            pc.correct = e.getValue()[1];
            pc.accuracyPct = pc.total == 0 ? 0 : Math.round(pc.correct * 100f / pc.total);
            eval.perCategory.add(pc);
        }
        return eval;
    }

    private IssueCategory knnPredict(Tfidf index, List<Map<String, Double>> trainVecs,
                                     List<Issue> trainSet, String description, int kk) {
        Map<String, Double> qv = index.vectorize(description);
        Integer[] order = new Integer[trainVecs.size()];
        double[] scores = new double[trainVecs.size()];
        for (int i = 0; i < trainVecs.size(); i++) { scores[i] = Tfidf.cosine(qv, trainVecs.get(i)); order[i] = i; }
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));
        Map<IssueCategory, Double> votes = new EnumMap<>(IssueCategory.class);
        for (int n = 0; n < Math.min(kk, order.length); n++) {
            int i = order[n];
            votes.merge(trainSet.get(i).getCategory(), Math.max(scores[i], 1e-6), Double::sum);
        }
        IssueCategory best = null; double bv = -1;
        for (Map.Entry<IssueCategory, Double> e : votes.entrySet()) {
            if (e.getValue() > bv) { bv = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    // ---- retrieval: Recall@K + MRR over a fixed question set ----------------

    private static final String[][] RETRIEVAL_SET = {
            {"How do I pay my management fee and what happens if it is late?", "BL-FEES"},
            {"管理費逾期繳交會有甚麼後果？", "BL-FEES"},
            {"What are the quiet hours, when can I not make noise?", "BL-NOISE"},
            {"裝修工程可以在甚麼時間進行？", "BL-RENO"},
            {"Am I allowed to keep a dog in my flat?", "BL-PET"},
            {"Why must the corridor be kept clear of belongings?", "BL-FIRE"},
            {"火警發生時我應該怎麼做？", "BL-FIRE"},
            {"How do I book the lift to move house?", "FAQ-MOVE"},
            {"Water is seeping from the flat above me, what should I do?", "FAQ-LEAK"},
            {"How is an Owners Corporation general meeting called?", "BL-MEETING"},
    };

    public RetrievalEval evaluateRetrieval() {
        RetrievalEval eval = new RetrievalEval();
        int r1 = 0, r3 = 0, r5 = 0;
        double mrrSum = 0.0;
        for (String[] qa : RETRIEVAL_SET) {
            String question = qa[0];
            String expected = qa[1];
            List<String> ranked = ragService.rankedDocCodes(question);
            int rank = ranked.indexOf(expected) + 1; // 0 -> not found -> rank 0
            Row row = new Row();
            row.question = question;
            row.expected = expected;
            row.rank = rank == 0 ? "—" : String.valueOf(rank);
            row.topDocs = String.join(", ", ranked.subList(0, Math.min(3, ranked.size())));
            eval.rows.add(row);
            if (rank == 1) r1++;
            if (rank >= 1 && rank <= 3) r3++;
            if (rank >= 1 && rank <= 5) r5++;
            if (rank >= 1) mrrSum += 1.0 / rank;
        }
        eval.n = RETRIEVAL_SET.length;
        eval.recall1Pct = Math.round(r1 * 100f / eval.n);
        eval.recall3Pct = Math.round(r3 * 100f / eval.n);
        eval.recall5Pct = Math.round(r5 * 100f / eval.n);
        eval.mrr = Math.round(mrrSum / eval.n * 1000) / 1000.0;

        // Distinct documents actually served, averaged over the same question set. Measured through
        // servedDocCodes (threshold + top-K + per-document cap applied) rather than rankedDocCodes,
        // which dedupes by document and applies neither, so it cannot see the diversification.
        double distinct = 0;
        for (String[] qa : RETRIEVAL_SET) distinct += ragService.servedDocCodes(qa[0], List.of()).size();
        eval.avgServedDocs = Math.round(distinct / eval.n * 100) / 100.0;
        return eval;
    }

    // ---- follow-up handling: rank of the expected doc with vs. without conversation history ----

    /** {prior question, follow-up that cannot stand alone, document the follow-up is really about} */
    private static final String[][] FOLLOWUP_SET = {
            {"What are the quiet hours?", "What about on Sundays?", "BL-NOISE"},
            {"How do I pay my management fee?", "And if I pay it late?", "BL-FEES"},
            {"Can I keep a dog in my flat?", "What about a cat?", "BL-PET"},
            {"裝修工程可以在甚麼時間進行？", "那星期日呢？", "BL-RENO"},
            {"我想搬屋，要預約升降機嗎？", "那甚麼時間可以搬？", "FAQ-MOVE"},
            {"樓上單位滴水到我的冷氣機平台。", "那誰負責維修？", "BL-AC"},
    };

    // ---- bilingual parity: the same question asked in English and in 繁體中文 -------------------

    /** {English question, 中文 question with the same meaning, document both should find} */
    private static final String[][] BILINGUAL_SET = {
            {"What happens if my management fee is overdue?", "管理費逾期未繳會有甚麼後果？", "BL-FEES"},
            {"What are the quiet hours in the estate?", "屋苑的安寧時段是甚麼時間？", "BL-NOISE"},
            {"When may renovation work be carried out?", "裝修工程可以在甚麼時間進行？", "BL-RENO"},
            {"Am I allowed to keep a dog?", "我可以養狗嗎？", "BL-PET"},
            {"Why must corridors be kept clear?", "為甚麼走廊必須保持暢通？", "BL-FIRE"},
            {"How do I book the lift for moving house?", "搬屋要怎樣預約升降機？", "FAQ-MOVE"},
            {"The flat above refuses to let anyone inspect the seepage.", "樓上單位拒絕檢查滲水，我可以怎樣做？", "BL-SEEPAGE-JO"},
            {"Who pays the management fee, the owner or the tenant?", "管理費應由業主還是租客繳付？", "BL-SUBLET-FEES"},
            {"Can I install an electric vehicle charger?", "我可以安裝電動車充電器嗎？", "BL-EV"},
            {"What are my rights to inspect the accounts?", "業主有權查閱法團的帳目嗎？", "BL-OWNER-RIGHTS"},
    };

    /**
     * Measures whether the corpus serves both languages equally. A bilingual estate system can look
     * fine in English while answering Chinese questions from a thin translated footnote, and no
     * single-language metric would reveal it. This asks the SAME question twice and compares.
     */
    public BilingualEval evaluateBilingual() {
        BilingualEval eval = new BilingualEval();
        for (String[] row : BILINGUAL_SET) {
            String en = row[0], zh = row[1], expected = row[2];
            int rankEn = ragService.rankedDocCodes(en).indexOf(expected) + 1;
            int rankZh = ragService.rankedDocCodes(zh).indexOf(expected) + 1;

            BilingualRow r = new BilingualRow();
            r.english = en;
            r.chinese = zh;
            r.expected = expected;
            r.rankEn = rankEn == 0 ? "—" : String.valueOf(rankEn);
            r.rankZh = rankZh == 0 ? "—" : String.valueOf(rankZh);
            r.bothTop1 = rankEn == 1 && rankZh == 1;
            eval.rows.add(r);
            if (rankEn == 1) eval.hitsEn++;
            if (rankZh == 1) eval.hitsZh++;
            if (r.bothTop1) eval.bothTop1++;
        }
        eval.n = BILINGUAL_SET.length;
        return eval;
    }

    /**
     * FR-8.1 follow-up evaluation. Recall@K over standalone questions cannot see conversational
     * ability at all, so this measures it directly: the same follow-up is ranked with and without
     * the preceding turn, which isolates the contribution of the history fold-in.
     */
    public FollowupEval evaluateFollowups() {
        FollowupEval eval = new FollowupEval();
        for (String[] row : FOLLOWUP_SET) {
            String prior = row[0], followUp = row[1], expected = row[2];
            int without = ragService.rankedDocCodes(followUp).indexOf(expected) + 1;
            int with = ragService.rankedDocCodes(followUp, List.of(ChatTurn.user(prior))).indexOf(expected) + 1;

            FollowupRow r = new FollowupRow();
            r.prior = prior;
            r.followUp = followUp;
            r.expected = expected;
            r.rankWithout = without == 0 ? "—" : String.valueOf(without);
            r.rankWith = with == 0 ? "—" : String.valueOf(with);
            r.improved = with >= 1 && (without == 0 || with < without);
            eval.rows.add(r);
            if (without == 1) eval.hitsWithout++;
            if (with == 1) eval.hitsWith++;
        }
        eval.n = FOLLOWUP_SET.length;
        return eval;
    }

    // ---- value types ------------------------------------------------------

    public static class ClassificationEval {
        public int n, correct, accuracyPct, k, trainSize;
        public List<PerCategory> perCategory = new ArrayList<>();
        public List<Example> examples = new ArrayList<>();
        public int getN() { return n; }
        public int getCorrect() { return correct; }
        public int getAccuracyPct() { return accuracyPct; }
        public int getK() { return k; }
        public int getTrainSize() { return trainSize; }
        public List<PerCategory> getPerCategory() { return perCategory; }
        public List<Example> getExamples() { return examples; }
    }

    public static class PerCategory {
        public IssueCategory category;
        public int total, correct, accuracyPct;
        public IssueCategory getCategory() { return category; }
        public int getTotal() { return total; }
        public int getCorrect() { return correct; }
        public int getAccuracyPct() { return accuracyPct; }
    }

    public static class Example {
        public String refNumber;
        public IssueCategory actual, predicted;
        public boolean correct;
        public String getRefNumber() { return refNumber; }
        public IssueCategory getActual() { return actual; }
        public IssueCategory getPredicted() { return predicted; }
        public boolean isCorrect() { return correct; }
    }

    public static class RetrievalEval {
        public int n, recall1Pct, recall3Pct, recall5Pct;
        public double mrr, avgServedDocs;
        public List<Row> rows = new ArrayList<>();
        public int getN() { return n; }
        public int getRecall1Pct() { return recall1Pct; }
        public int getRecall3Pct() { return recall3Pct; }
        public int getRecall5Pct() { return recall5Pct; }
        public double getMrr() { return mrr; }
        public double getAvgServedDocs() { return avgServedDocs; }
        public List<Row> getRows() { return rows; }
    }

    public static class FollowupEval {
        public int n, hitsWith, hitsWithout;
        public List<FollowupRow> rows = new ArrayList<>();
        public int getN() { return n; }
        public int getHitsWith() { return hitsWith; }
        public int getHitsWithout() { return hitsWithout; }
        public List<FollowupRow> getRows() { return rows; }
    }

    public static class BilingualEval {
        public int n, hitsEn, hitsZh, bothTop1;
        public List<BilingualRow> rows = new ArrayList<>();
        public int getN() { return n; }
        public int getHitsEn() { return hitsEn; }
        public int getHitsZh() { return hitsZh; }
        public int getBothTop1() { return bothTop1; }
        public List<BilingualRow> getRows() { return rows; }
    }

    public static class BilingualRow {
        public String english, chinese, expected, rankEn, rankZh;
        public boolean bothTop1;
        public String getEnglish() { return english; }
        public String getChinese() { return chinese; }
        public String getExpected() { return expected; }
        public String getRankEn() { return rankEn; }
        public String getRankZh() { return rankZh; }
        public boolean isBothTop1() { return bothTop1; }
    }

    public static class FollowupRow {
        public String prior, followUp, expected, rankWithout, rankWith;
        public boolean improved;
        public String getPrior() { return prior; }
        public String getFollowUp() { return followUp; }
        public String getExpected() { return expected; }
        public String getRankWithout() { return rankWithout; }
        public String getRankWith() { return rankWith; }
        public boolean isImproved() { return improved; }
    }

    public static class Row {
        public String question, expected, topDocs, rank;
        public String getQuestion() { return question; }
        public String getExpected() { return expected; }
        public String getTopDocs() { return topDocs; }
        public String getRank() { return rank; }
    }
}
