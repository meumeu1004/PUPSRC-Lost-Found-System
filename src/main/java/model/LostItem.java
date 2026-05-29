package model;

public class LostItem extends Item{
    private String itemStatus;      // Matched, Unmatched
    private String ownerName;
    private String ownerContactNum;
    private String ownerContactEmail;
    private String dateLost;

    public LostItem (int id, String itemName, String category, String description,
                     String color, String dateReported, String imagePath, String recordStatus,
                     String itemStatus, String ownerName, String ownerContactNum,
                     String ownerContactEmail, String dateLost) {

        super(id, itemName, category, description, color, dateReported, imagePath, recordStatus);
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

    public String getDateLost() {
        return dateLost; }

    public String getItemStatus() {
        return itemStatus; }

    @Override
    public String getType() {
        return "Lost"; }

}
