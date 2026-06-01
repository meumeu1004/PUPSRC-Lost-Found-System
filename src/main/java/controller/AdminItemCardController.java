package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;

public class AdminItemCardController {

    @FXML private Label     iconLabel;
    @FXML private Label     itemName;
    @FXML private Label     itemDate;
    @FXML private Label     statusLabel;
    @FXML private Label     typeLabel;
    @FXML private ImageView itemImage;
    @FXML private StackPane imageContainer;

    public void setItem(String name, String date, String imagePath,
                        String status, String type, String category) {

        itemName.setText(name != null ? name : "—");
        itemDate.setText(date != null ? date : "—");

        if (statusLabel != null) statusLabel.setText(status != null ? status : "");
        if (typeLabel   != null) typeLabel.setText(type   != null ? type   : "");

        // Try to load image from local file path
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    itemImage.setImage(image);
                    itemImage.setVisible(true);
                    iconLabel.setVisible(false);
                    return;
                }
            } catch (Exception e) {
                // Fall through to show icon
            }
        }

        // No image - show emoji based on type and category
        showIcon(type, category);
    }

    private void showIcon(String type, String category) {
        String emoji = getEmoji(type, category);
        iconLabel.setText(emoji);
        iconLabel.setVisible(true);
        itemImage.setVisible(false);
    }

    private String getEmoji(String type, String category) {
        // Always show category emoji regardless of type
        return getCategoryEmoji(category);
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "❓";
        return switch (category) {
            case "Electronics" -> "📱";
            case "Clothing" -> "👕";
            case "Accessories" -> "💍";
            case "Books" -> "📚";
            case "ID/Documents" -> "🪪";
            case "Keys" -> "🔑";
            case "Bag" -> "🎒";
            case "Others" -> "📦";
            default -> "❓";
        };
    }
}