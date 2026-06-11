package model;

public class MatchResult {
    private final Object item;        // LostItem or FoundItem
    private final int score;          // 0–100

    public MatchResult(Object item, int score) {
        this.item = item;
        this.score = score;
    }

    public Object getItem() { return item; }
    public int getScore()   { return score; }
}
