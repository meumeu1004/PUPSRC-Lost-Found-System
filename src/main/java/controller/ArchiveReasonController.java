package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ArchiveReasonController {

    @FXML private ComboBox<String> reasonCombo;
    @FXML private Button           confirmButton;

    private String selectedReason = null;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        reasonCombo.getItems().addAll(
                "Manually Archived by Admin",
                "Duplicate Record",
                "Invalid / Incorrect Entry",
                "Claimed by Owner"
        );
        reasonCombo.setValue("Manually Archived by Admin");
    }

    // =========================================================
    // CONFIRM
    // =========================================================
    @FXML
    private void handleConfirm() {
        if (reasonCombo.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Please select a reason before confirming.");
            alert.showAndWait();
            return;
        }
        selectedReason = reasonCombo.getValue();
        close();
    }

    // =========================================================
    // CANCEL — selectedReason stays null, caller checks for null
    // =========================================================
    @FXML
    private void handleCancel() {
        selectedReason = null;
        close();
    }

    // =========================================================
    // GETTER — called by PostItemViewController after showAndWait
    // =========================================================
    public String getSelectedReason() {
        return selectedReason;
    }

    // =========================================================
    // HELPER
    // =========================================================
    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }
}
