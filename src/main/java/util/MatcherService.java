package util;

import dao.FoundItemDAO;
import dao.LostItemDAO;
import model.FoundItem;
import model.LostItem;
import model.MatchResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatcherService {

    private static final int THRESHOLD = 25; // minimum score % to show

    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();

    // ── Called when viewing a Lost Item → search found_items ──
    public List<MatchResult> findForLost(LostItem lost) {
        // Pre-filter: active + unclaimed, optional category/color match
        List<FoundItem> candidates = foundDAO.findCandidatesFor(lost);

        List<MatchResult> results = new ArrayList<>();
        for (FoundItem found : candidates) {
            int score = score(lost, found);
            if (score >= THRESHOLD) {
                results.add(new MatchResult(found, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    // ── Called when viewing a Found Item → search lost_items ──
    public List<MatchResult> findForFound(FoundItem found) {
        List<LostItem> candidates = lostDAO.findCandidatesFor(found);

        List<MatchResult> results = new ArrayList<>();
        for (LostItem lost : candidates) {
            int score = score(lost, found);
            if (score >= THRESHOLD) {
                results.add(new MatchResult(lost, score));
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed());
        return results;
    }

    // =========================================================
    // SCORING ENGINE  (max 100)
    // =========================================================
    private int score(LostItem lost, FoundItem found) {
        int total = 0;

        // Category exact match — +30
        if (lost.getCategory() != null && lost.getCategory().equalsIgnoreCase(found.getCategory()))
            total += 30;

        // Item Name — +30
        if (lost.getItemName() != null && found.getItemName() != null) {
            double sim = jaccardSimilarity(
                    normalizePunctuation(lost.getItemName()),
                    normalizePunctuation(found.getItemName()));
            if (sim >= 0.8)      total += 30;
            else if (sim >= 0.5) total += 20;
            else if (sim >= 0.3) total += 12;
        }

        // Color — +15
        if (lost.getColor() != null && found.getColor() != null) {
            String lc = normalizeColor(normalizePunctuation(lost.getColor()));
            String fc = normalizeColor(normalizePunctuation(found.getColor()));
            if (lc.equals(fc)) {
                total += 15;
            } else {
                double sim = jaccardSimilarity(lc, fc);
                if (sim >= 0.5) total += 9;
                else if (sim >= 0.3) total += 4;
            }
        }

        // Description — +15
        if (lost.getDescription() != null && found.getDescription() != null) {
            double sim = jaccardSimilarity(
                    normalizePunctuation(lost.getDescription()),
                    normalizePunctuation(found.getDescription()));
            if (sim >= 0.5)      total += 15;
            else if (sim >= 0.3) total += 10;
            else if (sim >= 0.1) total += 5;
        }

        // Date proximity — +10
        if (lost.getDateLost() != null && found.getDateFound() != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(lost.getDateLost(), found.getDateFound()));
            if (days <= 7)       total += 10;
            else if (days <= 30) total += 5;
        }

        return Math.min(total, 100);
    }

    // - HELPERS -------------------------

    // STRING COMPARISON HELPER
    private double jaccardSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;

        Set<String> setA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\W+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\W+")));

        // remove short stop words
        setA.removeIf(w -> w.length() <= 1);
        setB.removeIf(w -> w.length() <= 1);

        if (setA.isEmpty() || setB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }


    private String normalizePunctuation(String input) {
        if (input == null) return null;
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")  // remove punctuation
                .replaceAll("\\s+", " ")            // collapse multiple spaces
                .trim();
    }

    private String normalizeColor(String input) {
        if (input == null) return null;
        String s = input.toLowerCase().trim();
        return COLOR_FAMILY.getOrDefault(s, s);
    }

    private static final Map<String, String> COLOR_FAMILY = Map.ofEntries(
            // Blues
            Map.entry("navy", "blue"),
            Map.entry("navy blue", "blue"),
            Map.entry("light blue", "blue"),
            Map.entry("sky blue", "blue"),
            Map.entry("royal blue", "blue"),
            Map.entry("cobalt", "blue"),
            Map.entry("indigo", "blue"),
            Map.entry("denim", "blue"),
            Map.entry("aqua", "blue"),
            Map.entry("cyan", "blue"),
            Map.entry("turquoise", "blue"),
            Map.entry("cerulean", "blue"),
            // Reds
            Map.entry("maroon", "red"),
            Map.entry("crimson", "red"),
            Map.entry("scarlet", "red"),
            Map.entry("burgundy", "red"),
            Map.entry("wine", "red"),
            Map.entry("coral", "red"),
            Map.entry("rust", "red"),
            Map.entry("ruby", "red"),
            Map.entry("brick red", "red"),
            // Greens
            Map.entry("olive", "green"),
            Map.entry("mint", "green"),
            Map.entry("teal", "green"),
            Map.entry("lime", "green"),
            Map.entry("emerald", "green"),
            Map.entry("sage", "green"),
            Map.entry("forest green", "green"),
            Map.entry("army green", "green"),
            Map.entry("hunter green", "green"),
            // Yellows
            Map.entry("gold", "yellow"),
            Map.entry("mustard", "yellow"),
            Map.entry("amber", "yellow"),
            Map.entry("lemon", "yellow"),
            Map.entry("golden", "yellow"),
            // Oranges
            Map.entry("peach", "orange"),
            Map.entry("tangerine", "orange"),
            Map.entry("burnt orange", "orange"),
            Map.entry("copper", "orange"),
            Map.entry("apricot", "orange"),
            // Purples
            Map.entry("violet", "purple"),
            Map.entry("lavender", "purple"),
            Map.entry("lilac", "purple"),
            Map.entry("mauve", "purple"),
            Map.entry("plum", "purple"),
            Map.entry("magenta", "purple"),
            Map.entry("fuchsia", "purple"),
            Map.entry("grape", "purple"),
            Map.entry("eggplant", "purple"),
            // Pinks
            Map.entry("hot pink", "pink"),
            Map.entry("blush", "pink"),
            Map.entry("baby pink", "pink"),
            Map.entry("dusty pink", "pink"),
            Map.entry("rose pink", "pink"),
            Map.entry("salmon pink", "pink"),
            // Browns
            Map.entry("tan", "brown"),
            Map.entry("khaki", "brown"),
            Map.entry("chocolate", "brown"),
            Map.entry("coffee", "brown"),
            Map.entry("caramel", "brown"),
            Map.entry("mocha", "brown"),
            Map.entry("chestnut", "brown"),
            Map.entry("mahogany", "brown"),
            Map.entry("taupe", "brown"),
            Map.entry("sienna", "brown"),
            // Whites
            Map.entry("ivory", "white"),
            Map.entry("cream", "white"),
            Map.entry("beige", "white"),
            Map.entry("off white", "white"),
            Map.entry("off-white", "white"),
            Map.entry("pearl", "white"),
            Map.entry("eggshell", "white"),
            // Grays
            Map.entry("grey", "gray"),
            Map.entry("charcoal", "gray"),
            Map.entry("silver", "gray"),
            Map.entry("slate", "gray"),
            Map.entry("ash", "gray"),
            Map.entry("graphite", "gray"),
            Map.entry("smoke", "gray"),
            Map.entry("dark gray", "gray"),
            Map.entry("dark grey", "gray"),
            Map.entry("light gray", "gray"),
            Map.entry("light grey", "gray"),
            // Blacks
            Map.entry("jet black", "black"),
            Map.entry("matte black", "black"),
            Map.entry("onyx", "black"),
            Map.entry("ebony", "black")
    );

}