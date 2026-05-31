package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LostItem extends Item{
    private String itemStatus;      // Unresolved, Found
    private String ownerName;
    private String ownerContactNum;
    private String ownerContactEmail;
    private LocalDate dateLost;

    public LostItem (int id, String itemName, String category, String description,
                     String color, String imagePath, String recordStatus, LocalDateTime createdAt, LocalDateTime updatedAt,
                     String archivedReason, LocalDateTime archivedAt, String itemStatus, String ownerName, String ownerContactNum,
                     String ownerContactEmail, LocalDate dateLost) {

        super(id, itemName, category, description, color, imagePath, recordStatus, createdAt, updatedAt, archivedReason, archivedAt);
        this.itemStatus = itemStatus;
        this.ownerName = ownerName;
        this.ownerContactNum = ownerContactNum;
        this.ownerContactEmail = ownerContactEmail;
        this.dateLost = dateLost;
    }

    public String getOwnerName() {
        return ownerName; }

    public String getOwnerContactNum() {
        return ownerContactNum; }

    public String getOwnerContactEmail() {
        return ownerContactEmail; }

    public LocalDate getDateLost() {
        return dateLost; }

    public String getItemStatus() {
        return itemStatus; }

    @Override
    public String getType() {
        return "Lost"; }

}
