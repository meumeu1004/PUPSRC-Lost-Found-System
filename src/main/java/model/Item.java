// Moved to separate models: FoundItem, LostItem

package model;

import java.time.LocalDateTime;

public abstract class Item {
    private int id;
    private String itemName;
    private String category;
    private String description;
    private String color;
    private String imagePath;
    private String recordStatus;
    private LocalDateTime createdAt;   // maps from created_at — serves as dateReported
    private LocalDateTime updatedAt;   // maps from updated_at
    private String archivedReason;
    private LocalDateTime archivedAt;


    public Item(int id, String itemName, String category, String description,
                String color, String imagePath, String recordStatus,
                LocalDateTime createdAt, LocalDateTime updatedAt, String archivedReason, LocalDateTime archivedAt) {
        this.id = id;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.color = color;
        this.imagePath = imagePath;
        this.recordStatus = recordStatus;   // Active, Archived, Deleted
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedReason = archivedReason;
        this.archivedAt = archivedAt;
    }


    public int getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public abstract String getType();

    public String getArchivedReason() { return archivedReason; }

    public void setArchivedReason(String archivedReason) { this.archivedReason = archivedReason; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
}