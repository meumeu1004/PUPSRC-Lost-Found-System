package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class ClaimController {

    @FXML private VBox claimDialogRoot;
    @FXML private Label itemNameLabel;
    @FXML private Label itemCategoryLabel;
    @FXML private Label itemColorLabel;
    @FXML private TextField claimantNameField;
    @FXML private TextField claimantIdField;
    @FXML private TextField claimantContactField;
    @FXML private TextField claimantEmailField;
    @FXML private PasswordField adminPasswordField;
    @FXML private ImageView proofPreview;
    @FXML private Label proofPlaceholder;
    @FXML private Button removeProofButton;
    @FXML private Button confirmButton;

    private String currentProofImageBase64 = null;
    private String itemName;
    private String itemCategory;
    private String itemColor;
    private PostItemController parentController;

    public void setItemDetails(String name, String category, String color, PostItemController controller) {
        this.itemName = name;
        this.itemCategory = category;
        this.itemColor = color;
        this.parentController = controller;

        itemNameLabel.setText(name);
        itemCategoryLabel.setText(category);
        itemColorLabel.setText(color != null && !color.isEmpty() ? color : "Not specified");
    }

    @FXML
    private void handleUploadProof() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Proof of Ownership Image (REQUIRED)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString(), 200, 200, true, true);
                proofPreview.setImage(image);
                proofPreview.setVisible(true);
                proofPlaceholder.setVisible(false);
                removeProofButton.setVisible(true);
                removeProofButton.setManaged(true);
                currentProofImageBase64 = encodeImageToBase64(file);

                // Remove red border if it was showing
                proofPlaceholder.setStyle("");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Failed to load image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemoveProof() {
        currentProofImageBase64 = null;
        proofPreview.setImage(null);
        proofPreview.setVisible(false);
        proofPlaceholder.setVisible(true);
        removeProofButton.setVisible(false);
        removeProofButton.setManaged(false);
    }

    private String encodeImageToBase64(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean validateRequiredFields() {
        StringBuilder missingFields = new StringBuilder();

        if (claimantNameField.getText() == null || claimantNameField.getText().trim().isEmpty()) {
            missingFields.append("• Claimant Full Name\n");
            claimantNameField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            claimantNameField.setStyle("");
        }

        if (claimantIdField.getText() == null || claimantIdField.getText().trim().isEmpty()) {
            missingFields.append("• ID Number\n");
            claimantIdField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            claimantIdField.setStyle("");
        }

        if (currentProofImageBase64 == null) {
            missingFields.append("• Proof of Ownership Image (REQUIRED)\n");
            proofPlaceholder.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 8;");
        } else {
            proofPlaceholder.setStyle("");
        }

        if (missingFields.length() > 0) {
            showAlert("Missing Required Fields!\n\nPlease provide:\n" + missingFields.toString());
            return false;
        }

        return true;
    }

    @FXML
    private void handleConfirmClaim() {
        String enteredPassword = adminPasswordField.getText();

        // Check if password is wrong
        if (!PasswordManager.verifyPassword(enteredPassword)) {
            showAlert("Incorrect admin password!");
            adminPasswordField.clear();
            adminPasswordField.requestFocus();
            return;
        }

        // Validate all required fields (including proof of ownership)
        if (!validateRequiredFields()) {
            return;
        }

        // Process the claim
        System.out.println("=== CLAIM PROCESSED BY ADMIN ===");
        System.out.println("Item: " + itemName);
        System.out.println("Claimant Name: " + claimantNameField.getText());
        System.out.println("ID Number: " + claimantIdField.getText());
        System.out.println("Contact: " + claimantContactField.getText());
        System.out.println("Email: " + claimantEmailField.getText());
        System.out.println("Proof of Ownership: " + (currentProofImageBase64 != null ? "Uploaded ✓" : "MISSING!"));

        if (parentController != null) {
            parentController.onClaimConfirmed();
        }

        showAlert("Claim has been processed for " + claimantNameField.getText());
        handleClose();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) claimDialogRoot.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");

        alert.showAndWait();
    }
}