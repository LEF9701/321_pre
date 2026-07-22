package com.smartestate.ai;

import com.smartestate.model.Issue;
import com.smartestate.model.IssueCategory;
import com.smartestate.repo.IssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Issue auto-categorisation via embedding k-NN (rp.pdf §3, Method 1), implemented with the
 * local TF-IDF space. Training data is the set of historically-labelled maintenance issues.
 */
@Service
public class IssueClassifier {

    private final IssueRepository issueRepository;

    @Value("${app.ai.knn.k:5}")
    private int k;

    private final Tfidf index = new Tfidf(false); // TF-only space for classification
    private final List<Train> train = new ArrayList<>();
    private volatile boolean built = false;

    public IssueClassifier(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        rebuild();
    }

    public synchronized void rebuild() {
        train.clear();
        for (Issue i : issueRepository.findAll()) {
            if (i.getCategory() != null && i.getDescription() != null && !i.getDescription().isBlank()) {
                train.add(new Train(i.getRefNumber(), i.getCategory(), i.getDescription()));
            }
        }
        List<String> corpus = new ArrayList<>();
        for (Train t : train) corpus.add(t.description);
        index.fit(corpus);
        built = true;
    }

    private void ensureBuilt() {
        if (!built || train.isEmpty()) rebuild();
    }

    public Result classify(String description) {
        ensureBuilt();
        Result r = new Result();
        if (description == null || description.isBlank() || train.isEmpty()) return r;

        Map<String, Double> qv = index.vectorize(description);
        double[] scores = new double[train.size()];
        for (int i = 0; i < train.size(); i++) scores[i] = Tfidf.cosine(qv, index.docVector(i));
        Integer[] order = new Integer[train.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        int kk = Math.min(k, train.size());
        Map<IssueCategory, Double> votes = new EnumMap<>(IssueCategory.class);
        double total = 0.0;
        for (int n = 0; n < kk; n++) {
            int i = order[n];
            Train t = train.get(i);
            double w = Math.max(scores[i], 1e-6); // distance-weighted vote
            votes.merge(t.category, w, Double::sum);
            total += w;
            Neighbor nb = new Neighbor();
            nb.refNumber = t.refNumber;
            nb.category = t.category;
            nb.score = Math.round(scores[i] * 1000) / 1000.0;
            r.neighbors.add(nb);
        }

        IssueCategory best = null;
        double bestWeight = -1;
        for (Map.Entry<IssueCategory, Double> e : votes.entrySet()) {
            if (e.getValue() > bestWeight) { bestWeight = e.getValue(); best = e.getKey(); }
        }
        r.predicted = best;
        r.confidencePct = total <= 0 ? 0 : Math.round((float) (bestWeight / total * 100));
        return r;
    }

    private record Train(String refNumber, IssueCategory category, String description) {}

    public static class Result {
        public IssueCategory predicted;
        public int confidencePct;
        public List<Neighbor> neighbors = new ArrayList<>();

        public IssueCategory getPredicted() { return predicted; }
        public int getConfidencePct() { return confidencePct; }
        public List<Neighbor> getNeighbors() { return neighbors; }
        public boolean isHasPrediction() { return predicted != null; }
    }

    public static class Neighbor {
        public String refNumber;
        public IssueCategory category;
        public double score;
        public String getRefNumber() { return refNumber; }
        public IssueCategory getCategory() { return category; }
        public double getScore() { return score; }
    }
}
