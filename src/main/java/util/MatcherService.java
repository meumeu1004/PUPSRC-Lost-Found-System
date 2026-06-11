package util;

import dao.FoundItemDAO;
import dao.LostItemDAO;
import model.FoundItem;
import model.LostItem;
import model.MatchResult;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatcherService {

    private static final int THRESHOLD = 40; // minimum score % to show

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

        // Color exact match — +20
        if (lost.getColor() != null && lost.getColor().equalsIgnoreCase(found.getColor()))
            total += 20;

        // Item name — +25 (exact), +12 (partial)
        if (lost.getItemName() != null && found.getItemName() != null) {
            String ln = lost.getItemName().toLowerCase();
            String fn = found.getItemName().toLowerCase();
            if (ln.equals(fn))                          total += 25;
            else if (ln.contains(fn) || fn.contains(ln)) total += 12;
        }

        // Description keyword match — +15
        total += descriptionScore(lost.getDescription(), found.getDescription());

        // Date proximity — +10 (within 30 days), +5 (within 90)
        if (lost.getDateLost() != null && found.getDateFound() != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(lost.getDateLost(), found.getDateFound()));
            if (days <= 30)      total += 10;
            else if (days <= 90) total += 5;
        }

        return Math.min(total, 100);
    }

    // Keyword overlap between two description strings
    private int descriptionScore(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0;

        String[] wordsA = a.toLowerCase().split("\\W+");
        String[] wordsB = b.toLowerCase().split("\\W+");

        // skip short stop-words
        java.util.Set<String> setA = new java.util.HashSet<>();
        for (String w : wordsA) if (w.length() > 3) setA.add(w);

        int matches = 0;
        for (String w : wordsB) if (w.length() > 3 && setA.contains(w)) matches++;

        if (matches >= 3) return 15;
        if (matches == 2) return 10;
        if (matches == 1) return 5;
        return 0;
    }
}