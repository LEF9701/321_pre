package com.smartestate.ai;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tiny bilingual TF-IDF vector space, used to approximate the bge-m3 embedding
 * retrieval / k-NN classification of the AI research prototype (rp.pdf) fully offline.
 *
 * English is tokenised on word boundaries; Traditional/Simplified Chinese (which has no
 * spaces) is tokenised into character unigrams + bigrams so CJK queries can match CJK docs.
 */
public class Tfidf {

    private static final Pattern EN = Pattern.compile("[a-z0-9]{2,}");
    private static final Set<String> STOP = Set.of(
            "the", "a", "an", "of", "to", "and", "or", "is", "are", "be", "in", "on", "for", "at",
            "by", "with", "as", "that", "this", "it", "its", "your", "you", "will", "shall", "may",
            "must", "please", "any", "all", "from", "if", "not", "no", "do", "does", "can", "we",
            "our", "us", "their", "they", "he", "she", "his", "her", "i", "my", "me", "should");

    private final Map<String, Double> idf = new HashMap<>();
    private final List<Map<String, Double>> docVectors = new ArrayList<>();
    private final boolean useIdf;

    /** IDF-weighted by default (best for retrieval / ranking). */
    public Tfidf() { this(true); }

    /**
     * @param useIdf false for a plain normalised term-frequency space; better for k-NN
     *               classification, where shared category vocabulary is the discriminative
     *               signal that IDF would otherwise suppress.
     */
    public Tfidf(boolean useIdf) { this.useIdf = useIdf; }

    /** Build the index from a corpus; index i in the corpus maps to docVector i. */
    public void fit(List<String> corpus) {
        idf.clear();
        docVectors.clear();
        int n = corpus.size();
        Map<String, Integer> df = new HashMap<>();
        List<List<String>> tokenised = new ArrayList<>();
        for (String doc : corpus) {
            List<String> toks = tokenize(doc);
            tokenised.add(toks);
            for (String t : new HashSet<>(toks)) df.merge(t, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idf.put(e.getKey(), useIdf ? Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0 : 1.0);
        }
        for (List<String> toks : tokenised) docVectors.add(vector(toks));
    }

    public Map<String, Double> vectorize(String text) {
        return vector(tokenize(text));
    }

    public Map<String, Double> docVector(int i) {
        return docVectors.get(i);
    }

    public int size() {
        return docVectors.size();
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Map<String, Double> small = a.size() < b.size() ? a : b;
        Map<String, Double> big = small == a ? b : a;
        double dot = 0.0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double v = big.get(e.getKey());
            if (v != null) dot += e.getValue() * v;
        }
        return dot; // vectors are L2-normalised in vector()
    }

    // ---- internals --------------------------------------------------------

    private Map<String, Double> vector(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) tf.merge(t, 1, Integer::sum);
        Map<String, Double> vec = new HashMap<>();
        double norm = 0.0;
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double w = e.getValue() * idf.getOrDefault(e.getKey(), 1.0);
            if (w == 0.0) continue;
            vec.put(e.getKey(), w);
            norm += w * w;
        }
        if (norm > 0) {
            double inv = 1.0 / Math.sqrt(norm);
            vec.replaceAll((k, v) -> v * inv);
        }
        return vec;
    }

    /**
     * Conservative plural folding, so a question about a "dog" matches a bylaw about "dogs".
     * Without it the corpus and the query only match on exact surface forms, which is a common
     * cause of a relevant document ranking far down for an otherwise obvious question.
     *
     * <p>Plurals only, no -ing/-ed stemming, which over-conflates more than it helps at this
     * corpus size. The guards keep words like "gas", "class", "premises" and "analysis" intact.
     */
    static String singular(String w) {
        int n = w.length();
        if (n <= 3 || !w.endsWith("s")) return w;
        if (w.endsWith("ss") || w.endsWith("us") || w.endsWith("is")) return w;   // class, bonus, basis
        if (w.endsWith("ies") && n > 4) return w.substring(0, n - 3) + "y";       // policies -> policy
        if (w.endsWith("ses") || w.endsWith("xes") || w.endsWith("zes")
                || w.endsWith("ches") || w.endsWith("shes")) return w.substring(0, n - 2); // boxes -> box
        return w.substring(0, n - 1);                                             // dogs -> dog
    }

    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String lower = text.toLowerCase();
        Matcher m = EN.matcher(lower);
        while (m.find()) {
            String w = m.group();
            if (!STOP.contains(w)) out.add(singular(w));
        }
        // CJK: unigrams + bigrams of consecutive CJK characters
        StringBuilder run = new StringBuilder();
        for (int i = 0; i <= lower.length(); i++) {
            char c = i < lower.length() ? lower.charAt(i) : 0;
            if (i < lower.length() && isCjk(c)) {
                run.append(c);
            } else if (run.length() > 0) {
                String s = run.toString();
                for (int j = 0; j < s.length(); j++) {
                    out.add(String.valueOf(s.charAt(j)));
                    if (j + 1 < s.length()) out.add(s.substring(j, j + 2));
                }
                run.setLength(0);
            }
        }
        return out;
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF);
    }
}
