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
                        String status, String type) {

        itemName.setText(name != null ? name : "—");
        itemDate.setText(date != null ? date : "—");

        if (statusLabel != null) statusLabel.setText(status != null ? status : "");
        if (typeLabel   != null) typeLabel.setText(type   != null ? type   : "");

        // Determine the icon character to show when no image is available
        String icon = switch (type != null ? type : "") {
            case "Lost"  -> "✕";
            case "Found" -> "✓";
            default      -> "?";
        };

        // Try to load image from local file path
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    itemImage.setImage(image);
                    itemImage.setVisible(true);
                    iconLabel.setVisible(false);
                } else {
                    showIcon(icon);
                }
            } catch (Exception e) {
                showIcon(icon);
            }
        } else {
            showIcon(icon);
        }
    }

    // =========================================================
    // HELPER
    // =========================================================
    private void showIcon(String icon) {
        iconLabel.setText(icon);
        iconLabel.setVisible(true);
        itemImage.setVisible(false);
    }
}