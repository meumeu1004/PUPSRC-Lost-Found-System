package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class AdminItemCardController {
    @FXML private Label iconLabel;
    @FXML private Label itemName;
    @FXML private Label itemDate;
    @FXML private ImageView itemImage;
    @FXML private StackPane imageContainer;

    public void setItemData(String name, String date, String icon, String imageBase64) {
        itemName.setText(name);
        itemDate.setText(date);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                itemImage.setImage(image);
                itemImage.setVisible(true);
                iconLabel.setVisible(false);
            } catch (Exception e) {
                iconLabel.setText(icon);
                iconLabel.setVisible(true);
                itemImage.setVisible(false);
            }
        } else {
            iconLabel.setText(icon);
            iconLabel.setVisible(true);
            itemImage.setVisible(false);
        }
    }
}