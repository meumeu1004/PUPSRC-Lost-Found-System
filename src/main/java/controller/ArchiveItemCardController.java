package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import model.FoundItem;
import model.LostItem;
import util.DateUtil;

import java.io.File;

public class ArchiveItemCardController {

    @FXML private Label     iconLabel;
    @FXML private ImageView itemImage;
    @FXML private StackPane imageContainer;

    @FXML private Label itemName;
    @FXML private Label itemDate;
    @FXML private Label statusLabel;
    @FXML private Label typeLabel;

    @FXML private Label archiveReasonLabel;
    @FXML private Label archivedAtLabel;

    // =========================================================
    // SET LOST ITEM
    // =========================================================
    public void setLostItem(LostItem item) {
        itemName.setText(item.getItemName() != null ? item.getItemName() : "");
        itemDate.setText(item.getDateLost() != null
                ? DateUtil.format(item.getDateLost()) : "");
        statusLabel.setText(item.getItemStatus() != null ? item.getItemStatus() : "");
        typeLabel.setText("Lost");

        archiveReasonLabel.setText(
                item.getArchivedReason() != null ? item.getArchivedReason() : "—");
        archivedAtLabel.setText(
                item.getArchivedAt() != null ? DateUtil.format(item.getArchivedAt()) : "—");

        loadImage(item.getImagePath(), "L");
    }

    // =========================================================
    // SET FOUND ITEM
    // =========================================================
    public void setFoundItem(FoundItem item) {
        itemName.setText(item.getItemName() != null ? item.getItemName() : "");
        itemDate.setText(item.getDateFound() != null
                ? DateUtil.format(item.getDateFound()) : "");
        statusLabel.setText(item.getItemStatus() != null ? item.getItemStatus() : "");
        typeLabel.setText("Found");

        archiveReasonLabel.setText(
                item.getArchivedReason() != null ? item.getArchivedReason() : "—");
        archivedAtLabel.setText(
                item.getArchivedAt() != null ? DateUtil.format(item.getArchivedAt()) : "—");

        loadImage(item.getImagePath(), "F");
    }

    // =========================================================
    // HELPER
    // =========================================================
    private void loadImage(String imagePath, String fallbackIcon) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    itemImage.setImage(new Image(file.toURI().toString()));
                    itemImage.setVisible(true);
                    iconLabel.setVisible(false);
                    return;
                }
            } catch (Exception ignored) {}
        }
        iconLabel.setText(fallbackIcon);
        iconLabel.setVisible(true);
        itemImage.setVisible(false);
    }
}
