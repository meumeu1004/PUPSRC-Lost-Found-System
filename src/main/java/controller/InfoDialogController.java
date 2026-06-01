package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class InfoDialogController {

    @FXML private Label titleLabel;
    @FXML private Label contentLabel;

    public void setContent(String title, String content) {
        titleLabel.setText(title);
        contentLabel.setText(content);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}