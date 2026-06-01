package model.enums;

public enum ArchiveReason {

    AUTO_LOST_UNRESOLVED("Auto Archived (Unresolved)"),
    AUTO_FOUND_UNCLAIMED("Auto Archived (Unclaimed)"),
    MANUAL("Manually Archived");

    private final String label;

    ArchiveReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
