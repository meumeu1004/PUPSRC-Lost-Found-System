package model;

public abstract class Item {
    private int id;
    private String itemName;
    private String category;
    private String description;
    private String color;
    private String imagePath;
    private String dateReported;
    private String recordStatus;


    public Item(int id, String itemName, String category, String description,
                String color, String dateReported, String imagePath, String recordStatus) {
        this.id = id;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.color = color;
        this.dateReported = dateReported;
        this.imagePath = imagePath;
        this.recordStatus = recordStatus;   // Active or Archived
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

    public String getDateReported() {
        return dateReported;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public abstract String getType();

}