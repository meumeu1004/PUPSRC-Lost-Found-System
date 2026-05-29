package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class PostItemController {

    @FXML private Label titleLabel;
    @FXML private TextField itemNameField;
    @FXML private ComboBox<String> categoryPicker;
    @FXML private TextField colorField;
    @FXML private ComboBox<String> statusPicker;
    @FXML private TextField reporterNameField;
    @FXML private TextField contactNumberField;
    @FXML private TextField emailField;
    @FXML private DatePicker reportedDatePicker;
    @FXML private TextArea descArea;
    @FXML private VBox dialogRoot;

    @FXML private Button editButton;
    @FXML private Label editNoticeLabel;
    @FXML private Button claimButton;
    @FXML private Button markFoundButton;
    @FXML private Button saveButton;
    @FXML private Button archiveButton;
    @FXML private Button restoreButton;
    @FXML private Button manualArchiveButton;
    @FXML private VBox imageUploadBox;
    @FXML private ImageView previewImageView;
    @FXML private Label imagePlaceholderLabel;
    @FXML private Button removeImageButton;

    private boolean isEditMode = false;
    private boolean isExistingItem = false;
    private boolean isArchiveView = false;
    private String originalStatus = "";
    private String currentImageBase64 = null;
    private boolean isLostMode = true;
    private String currentItemName = "";
    private String currentCategory = "";
    private String currentColor = "";
    private String currentReporterName = "";
    private String currentContactNumber = "";
    private String currentEmail = "";
    private String currentDescription = "";

    // Callback for communicating with AdminController
    private ArchiveCallback archiveCallback;

    // Define the ArchiveCallback interface
    public interface ArchiveCallback {
        void moveItemToArchive(AdminController.ItemData item, String reason);
        void restoreItemFromArchive(AdminController.ItemData item);
        void updateItem(AdminController.ItemData item);
        void addNewItem(AdminController.ItemData item);
    }

    public void setArchiveCallback(ArchiveCallback callback) {
        this.archiveCallback = callback;
    }

    @FXML
    public void initialize() {
        categoryPicker.getItems().addAll("Electronics", "Clothing", "Documents", "Jewelry", "Other");
        statusPicker.getItems().addAll("Lost", "Found", "Claimed", "Unresolved");

        setNewItemMode();
    }

    public void setLostMode(boolean lost) {
        this.isLostMode = lost;
        if (lost) {
            titleLabel.setText("REPORT LOST ITEM");
            statusPicker.setValue("Lost");
            applyLostFormTheme();
        } else {
            titleLabel.setText("REPORT FOUND ITEM");
            statusPicker.setValue("Found");
            applyFoundFormTheme();
        }
        originalStatus = statusPicker.getValue();
    }

    private void applyLostFormTheme() {
        dialogRoot.setStyle("-fx-background-color: white; -fx-border-color: #c28d39; -fx-border-width: 3; -fx-background-radius: 5; -fx-border-radius: 5;");
        titleLabel.setTextFill(javafx.scene.paint.Color.web("#c28d39"));
        styleButtonsForTheme("#c28d39");
    }

    private void applyFoundFormTheme() {
        dialogRoot.setStyle("-fx-background-color: white; -fx-border-color: #153240; -fx-border-width: 3; -fx-background-radius: 5; -fx-border-radius: 5;");
        titleLabel.setTextFill(javafx.scene.paint.Color.web("#153240"));
        styleButtonsForTheme("#153240");
    }

    private void styleButtonsForTheme(String themeColor) {
        if (saveButton != null) {
            saveButton.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5; -fx-cursor: hand;");
        }
        if (editButton != null) {
            editButton.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5; -fx-cursor: hand;");
        }
    }

    public void setExistingItemMode(boolean isExisting, String currentStatus, boolean isArchive) {
        this.isExistingItem = isExisting;
        this.originalStatus = currentStatus;
        this.isArchiveView = isArchive;

        if (currentStatus.equals("Lost")) {
            applyLostFormTheme();
            isLostMode = true;
        } else if (currentStatus.equals("Found")) {
            applyFoundFormTheme();
            isLostMode = false;
        }

        if (isExistingItem) {
            titleLabel.setText("ITEM DETAILS");
            statusPicker.setValue(currentStatus);
            setFieldsEditable(false);

            hideContactInfo();

            // Show the notice next to edit button
            if (editNoticeLabel != null) {
                editNoticeLabel.setVisible(true);
                editNoticeLabel.setManaged(true);
            }

            editButton.setVisible(true);
            editButton.setManaged(true);
            saveButton.setVisible(false);
            saveButton.setManaged(false);

            if (isArchive) {
                restoreButton.setVisible(true);
                restoreButton.setManaged(true);
                editButton.setVisible(false);
                claimButton.setVisible(false);
                claimButton.setManaged(false);
                markFoundButton.setVisible(false);
                markFoundButton.setManaged(false);
                archiveButton.setVisible(false);
                archiveButton.setManaged(false);
                manualArchiveButton.setVisible(false);
                manualArchiveButton.setManaged(false);
                // Hide notice since edit is not available in archive
                if (editNoticeLabel != null) {
                    editNoticeLabel.setVisible(false);
                    editNoticeLabel.setManaged(false);
                }
            } else {
                restoreButton.setVisible(false);
                restoreButton.setManaged(false);

                // Show appropriate button based on status
                if (currentStatus.equals("Lost")) {
                    // Lost items show "Mark as Found" button
                    markFoundButton.setVisible(true);
                    markFoundButton.setManaged(true);
                    claimButton.setVisible(false);
                    claimButton.setManaged(false);
                } else if (currentStatus.equals("Found")) {
                    // Found items show "Claim" button
                    markFoundButton.setVisible(false);
                    markFoundButton.setManaged(false);
                    claimButton.setVisible(true);
                    claimButton.setManaged(true);
                }

                // Show Manual Archive button for Lost or Found items (not already archived)
                boolean canBeArchived = currentStatus.equals("Lost") || currentStatus.equals("Found");
                manualArchiveButton.setVisible(canBeArchived);
                manualArchiveButton.setManaged(canBeArchived);

                archiveButton.setVisible(false);
                archiveButton.setManaged(false);
            }
        } else {
            setNewItemMode();
        }
    }

    public void setItemDataForEditing(String name, String category, String color, String status,
                                      String reporterName, String contactNumber, String email,
                                      String description, String imageBase64) {
        this.currentItemName = name;
        this.currentCategory = category;
        this.currentColor = color;
        this.currentReporterName = reporterName;
        this.currentContactNumber = contactNumber;
        this.currentEmail = email;
        this.currentDescription = description;

        itemNameField.setText(name);
        categoryPicker.setValue(category);
        colorField.setText(color);
        statusPicker.setValue(status);
        reporterNameField.setText(reporterName);
        contactNumberField.setText(contactNumber);
        emailField.setText(email);
        descArea.setText(description);
        currentImageBase64 = imageBase64;

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                if (previewImageView != null) {
                    previewImageView.setImage(image);
                    previewImageView.setVisible(true);
                    if (imagePlaceholderLabel != null) {
                        imagePlaceholderLabel.setVisible(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hideContactInfo() {
        if (reporterNameField != null) {
            reporterNameField.setManaged(false);
            reporterNameField.setVisible(false);
        }
        if (contactNumberField != null) {
            contactNumberField.setManaged(false);
            contactNumberField.setVisible(false);
        }
        if (emailField != null) {
            emailField.setManaged(false);
            emailField.setVisible(false);
        }
    }

    private void showContactInfo() {
        if (reporterNameField != null) {
            reporterNameField.setManaged(true);
            reporterNameField.setVisible(true);
        }
        if (contactNumberField != null) {
            contactNumberField.setManaged(true);
            contactNumberField.setVisible(true);
        }
        if (emailField != null) {
            emailField.setManaged(true);
            emailField.setVisible(true);
        }
    }

    private void setNewItemMode() {
        setFieldsEditable(true);
        statusPicker.setDisable(true);
        editButton.setVisible(false);
        editButton.setManaged(false);
        saveButton.setVisible(true);
        saveButton.setManaged(true);
        saveButton.setText("SUBMIT REPORT");
        claimButton.setVisible(false);
        claimButton.setManaged(false);
        archiveButton.setVisible(false);
        archiveButton.setManaged(false);
        restoreButton.setVisible(false);
        restoreButton.setManaged(false);
        manualArchiveButton.setVisible(false);
        manualArchiveButton.setManaged(false);
        showContactInfo();

        // Hide the edit notice when creating new item
        if (editNoticeLabel != null) {
            editNoticeLabel.setVisible(false);
            editNoticeLabel.setManaged(false);
        }

        if (isLostMode) {
            applyLostFormTheme();
        } else {
            applyFoundFormTheme();
        }
    }

    private boolean verifyPasswordForAction(String action) {
        // Restore doesn't need password verification since archive access already requires it
        if (action.equals("Restore")) {
            proceedWithAction(action);
            return true;
        }

        Dialog<ButtonType> authDialog = new Dialog<>();
        authDialog.setTitle("NEED AUTHORIZATION");
        authDialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("ADMIN PASSWORD");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #710912; -fx-font-size: 14px;");
        grid.add(titleLabel, 0, 0);

        grid.add(new Label("Password:"), 0, 1);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        grid.add(passwordField, 1, 1);

        authDialog.getDialogPane().setContent(grid);
        authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        authDialog.getDialogPane().setStyle("-fx-background-color: white;");

        Button okBtn = (Button) authDialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #710912; -fx-text-fill: white;");

        final boolean[] passwordCorrect = {false};

        authDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String enteredPassword = passwordField.getText();
                if (PasswordManager.verifyPassword(enteredPassword)) {
                    passwordCorrect[0] = true;
                    proceedWithAction(action);
                } else {
                    PasswordManager.showAlert("Incorrect password!", action + " access denied.");
                }
            }
        });
        return passwordCorrect[0];
    }

    private void proceedWithAction(String action) {
        if (action.equals("Edit")) {
            isEditMode = true;
            setFieldsEditable(true);
            showContactInfo();
            statusPicker.setDisable(false);
            editButton.setDisable(true);
            saveButton.setVisible(true);
            saveButton.setManaged(true);
            saveButton.setText("SAVE CHANGES");
            claimButton.setVisible(false);
            claimButton.setManaged(false);
            manualArchiveButton.setVisible(false);
            manualArchiveButton.setManaged(false);

            // Hide the notice once edit is clicked
            if (editNoticeLabel != null) {
                editNoticeLabel.setVisible(false);
                editNoticeLabel.setManaged(false);
            }

            PasswordManager.showAlert("Edit mode enabled.", "You can now modify all fields including Status.");
        } else if (action.equals("Archive")) {
            showArchiveConfirmation();
        } else if (action.equals("Restore")) {
            performRestore();
        } else if (action.equals("ManualArchive")) {
            performManualArchive();
        }
    }

    @FXML
    private void handleEditMode() {
        if (!isExistingItem) return;
        verifyPasswordForAction("Edit");
    }

    @FXML
    private void handleArchive() {
        if (!isExistingItem) return;
        verifyPasswordForAction("Archive");
    }

    @FXML
    private void handleManualArchive() {
        if (!isExistingItem) return;
        verifyPasswordForAction("ManualArchive");
    }

    @FXML
    private void handleRestore() {
        if (!isExistingItem || !isArchiveView) return;
        verifyPasswordForAction("Restore");
    }

    private void performManualArchive() {
        String originalStatus = statusPicker.getValue(); // "Lost" or "Found"
        String newStatus = originalStatus + " - Unresolved";

        // Create item data from current fields with new status format
        AdminController.ItemData itemToArchive = new AdminController.ItemData(
                itemNameField.getText(),
                reportedDatePicker.getValue() != null ? "Reported: " + reportedDatePicker.getValue().toString() : "No date",
                getIconForCategory(categoryPicker.getValue()),
                categoryPicker.getValue(),
                newStatus,  // e.g., "Lost - Unresolved" or "Found - Unresolved"
                reporterNameField.getText(),
                emailField.getText(),
                contactNumberField.getText(),
                descArea.getText(),
                colorField.getText(),
                currentImageBase64
        );

        if (archiveCallback != null) {
            archiveCallback.moveItemToArchive(itemToArchive, "Manually archived by admin");
            PasswordManager.showAlert("Item Archived", "Item has been manually moved to archive with status '" + newStatus + "'.");
            handleClose();
        }
    }

    private void performRestore() {
        System.out.println("=== ITEM RESTORED FROM ARCHIVE ===");
        System.out.println("Item: " + currentItemName);
        System.out.println("Category: " + currentCategory);
        System.out.println("Color: " + currentColor);
        System.out.println("Description: " + currentDescription);
        System.out.println("Original Status: " + originalStatus);

        // When restoring, set status back to original (Lost or Found)
        String restoredStatus = originalStatus.contains(" - Unresolved") ? originalStatus.split(" - ")[0] : originalStatus;

        // Create restored item
        AdminController.ItemData restoredItem = new AdminController.ItemData(
                currentItemName,
                "Reported: " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()),
                getIconForCategory(currentCategory),
                currentCategory,
                restoredStatus,
                currentReporterName,
                currentEmail,
                currentContactNumber,
                currentDescription,
                currentColor,
                currentImageBase64
        );

        if (archiveCallback != null) {
            archiveCallback.restoreItemFromArchive(restoredItem);
        }

        PasswordManager.showAlert("Item Restored", "Item has been restored to the main dashboard with status '" + restoredStatus + "'.");
        handleClose();
    }

    private boolean validateRequiredFields() {
        StringBuilder missingFields = new StringBuilder();

        if (itemNameField.getText() == null || itemNameField.getText().trim().isEmpty()) {
            missingFields.append("• Item Name\n");
            itemNameField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            itemNameField.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (categoryPicker.getValue() == null) {
            missingFields.append("• Category\n");
            categoryPicker.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            categoryPicker.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (reporterNameField.getText() == null || reporterNameField.getText().trim().isEmpty()) {
            missingFields.append("• Reporter Name\n");
            reporterNameField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            reporterNameField.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (contactNumberField.getText() == null || contactNumberField.getText().trim().isEmpty()) {
            missingFields.append("• Contact Number\n");
            contactNumberField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            contactNumberField.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            missingFields.append("• Email\n");
            emailField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            emailField.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (reportedDatePicker.getValue() == null) {
            missingFields.append("• Reported Date\n");
            reportedDatePicker.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            reportedDatePicker.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (descArea.getText() == null || descArea.getText().trim().isEmpty()) {
            missingFields.append("• Description\n");
            descArea.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        } else {
            descArea.setStyle("-fx-border-color: #D3D3D3; -fx-border-radius: 5;");
        }

        if (missingFields.length() > 0) {
            PasswordManager.showAlert("Missing Required Fields",
                    "Please fill in the following required fields:\n\n" + missingFields.toString() +
                            "\nNote: Photo upload is optional.");
            return false;
        }

        return true;
    }

    @FXML
    private void handleSave() {
        if (isExistingItem && !isEditMode) return;

        // Validate all required fields (photo is optional)
        if (!validateRequiredFields()) {
            return;
        }

        String itemName = itemNameField.getText();
        String category = categoryPicker.getValue();
        String newStatus = statusPicker.getValue();
        boolean shouldArchive = false;
        String archiveReason = "";

        if (isExistingItem && !newStatus.equals(originalStatus)) {
            if (newStatus.equals("Claimed")) {
                shouldArchive = true;
                archiveReason = "Item was claimed by owner";
            } else if (newStatus.equals("Unresolved")) {
                shouldArchive = true;
                archiveReason = "Item marked as unresolved (too long unclaimed)";
            }
            // Note: Changing from Lost to Found does NOT trigger archiving
        }

        // Create updated item data (image can be null - optional)
        AdminController.ItemData updatedItem = new AdminController.ItemData(
                itemName,
                reportedDatePicker.getValue() != null ? "Reported: " + reportedDatePicker.getValue().toString() : "No date",
                getIconForCategory(category),
                category,
                newStatus,
                reporterNameField.getText(),
                emailField.getText(),
                contactNumberField.getText(),
                descArea.getText(),
                colorField.getText() != null ? colorField.getText() : "",
                currentImageBase64
        );

        System.out.println("=== " + (isExistingItem ? "UPDATING ITEM" : "NEW ITEM REPORT") + " ===");
        System.out.println("Item Name: " + itemName);
        System.out.println("Category: " + category);
        System.out.println("Color: " + colorField.getText());
        System.out.println("Status: " + newStatus);
        System.out.println("Reporter: " + reporterNameField.getText());
        System.out.println("Contact: " + contactNumberField.getText());
        System.out.println("Email: " + emailField.getText());
        if (reportedDatePicker.getValue() != null) {
            System.out.println("Date: " + reportedDatePicker.getValue().toString());
        }
        System.out.println("Description: " + descArea.getText());
        System.out.println("Image: " + (currentImageBase64 != null ? "Uploaded" : "No image (optional)"));

        if (shouldArchive && archiveCallback != null) {
            archiveCallback.moveItemToArchive(updatedItem, archiveReason);
            PasswordManager.showAlert("Item Archived", "Item saved and moved to archive.\nReason: " + archiveReason);
            handleClose();
        } else if (isExistingItem && archiveCallback != null) {
            archiveCallback.updateItem(updatedItem);
            PasswordManager.showAlert("Success", "Changes saved successfully!");
            // Only close if it's being archived, otherwise just refresh
            if (!newStatus.equals("Claimed") && !newStatus.equals("Unresolved")) {
                // Don't close - just update the UI
                // Update the theme if status changed from Lost to Found
                if (newStatus.equals("Found") && !originalStatus.equals("Found")) {
                    applyFoundFormTheme();
                    isLostMode = false;
                    originalStatus = "Found";
                }
            } else {
                handleClose();
            }
        } else if (!isExistingItem && archiveCallback != null) {
            archiveCallback.addNewItem(updatedItem);
            PasswordManager.showAlert("Success", "Item reported successfully!");
            handleClose();
        }
    }

    private String getIconForCategory(String category) {
        if (category == null) return "📦";
        switch(category) {
            case "Electronics": return "💻";
            case "Clothing": return "👕";
            case "Documents": return "📄";
            case "Jewelry": return "💍";
            default: return "📦";
        }
    }

    @FXML
    private void handleClaim() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ClaimDialog.fxml"));
            VBox root = loader.load();

            ClaimController claimController = loader.getController();
            claimController.setItemDetails(
                    itemNameField.getText(),
                    categoryPicker.getValue(),
                    colorField.getText(),
                    this
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Claim Item");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            PasswordManager.showAlert("Error", "Could not open claim dialog: " + e.getMessage());
        }
    }

    public void onClaimConfirmed() {
        statusPicker.setValue("Claimed");
        PasswordManager.showAlert("Claim Processed", "Item status updated to Claimed.");
        // Trigger save to archive the claimed item
        handleSave();
    }

    @FXML
    private void handleMarkAsFound() {
        if (!isExistingItem) return;

        // Password verification for marking as found
        Dialog<ButtonType> authDialog = new Dialog<>();
        authDialog.setTitle("ADMIN AUTHENTICATION REQUIRED");
        authDialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("MARK ITEM AS FOUND");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #710912; -fx-font-size: 14px;");
        grid.add(titleLabel, 0, 0);

        Label infoLabel = new Label("This will change the item status from LOST to FOUND.\nThe item will remain in the main dashboard.");
        infoLabel.setStyle("-fx-text-fill: #5b5b5b; -fx-font-size: 12px;");
        infoLabel.setWrapText(true);
        grid.add(infoLabel, 0, 1, 2, 1);

        grid.add(new Label("Password:"), 0, 2);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        grid.add(passwordField, 1, 2);

        authDialog.getDialogPane().setContent(grid);
        authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        authDialog.getDialogPane().setStyle("-fx-background-color: white;");

        Button okBtn = (Button) authDialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20 8 20;");

        Button cancelBtn = (Button) authDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color: #A5A5A5; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20 8 20;");

        authDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String enteredPassword = passwordField.getText();
                if (PasswordManager.verifyPassword(enteredPassword)) {
                    performMarkAsFound();
                } else {
                    PasswordManager.showAlert("Access Denied", "Incorrect admin password.\nCannot mark item as found.");
                }
            }
        });
    }

    private void performMarkAsFound() {
        // Change the status in the UI
        statusPicker.setValue("Found");

        // Update the item in the backend
        String itemName = itemNameField.getText();
        String category = categoryPicker.getValue();

        // Create updated item data
        AdminController.ItemData updatedItem = new AdminController.ItemData(
                itemName,
                reportedDatePicker.getValue() != null ? "Reported: " + reportedDatePicker.getValue().toString() : "No date",
                getIconForCategory(category),
                category,
                "Found",
                reporterNameField.getText(),
                emailField.getText(),
                contactNumberField.getText(),
                descArea.getText(),
                colorField.getText() != null ? colorField.getText() : "",
                currentImageBase64
        );

        // Update in the backend
        if (archiveCallback != null) {
            archiveCallback.updateItem(updatedItem);
        }

        // Update the UI to reflect the change
        applyFoundFormTheme();
        isLostMode = false;

        // Swap buttons: hide Mark as Found, show Claim button
        markFoundButton.setVisible(false);
        markFoundButton.setManaged(false);
        claimButton.setVisible(true);
        claimButton.setManaged(true);

        // Update the status picker value
        originalStatus = "Found";

        PasswordManager.showAlert("Success", "Item has been marked as FOUND!\nThe item status has been updated.");
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Item Image (Optional)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString(), 200, 200, true, true);
                previewImageView.setImage(image);
                previewImageView.setVisible(true);
                if (imagePlaceholderLabel != null) {
                    imagePlaceholderLabel.setVisible(false);
                }
                if (removeImageButton != null) {
                    removeImageButton.setVisible(true);
                    removeImageButton.setManaged(true);
                }
                currentImageBase64 = encodeImageToBase64(file);
            } catch (Exception e) {
                e.printStackTrace();
                PasswordManager.showAlert("Error", "Failed to load image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemoveImage() {
        currentImageBase64 = null;
        previewImageView.setImage(null);
        previewImageView.setVisible(false);
        if (imagePlaceholderLabel != null) {
            imagePlaceholderLabel.setVisible(true);
        }
        if (removeImageButton != null) {
            removeImageButton.setVisible(false);
            removeImageButton.setManaged(false);
        }
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

    @FXML
    private void handleClose() {
        Stage stage = (Stage) itemNameField.getScene().getWindow();
        stage.close();
    }

    private void setFieldsEditable(boolean editable) {
        itemNameField.setEditable(editable);
        categoryPicker.setDisable(!editable);
        colorField.setEditable(editable);
        reporterNameField.setEditable(editable);
        contactNumberField.setEditable(editable);
        emailField.setEditable(editable);
        reportedDatePicker.setDisable(!editable);
        descArea.setEditable(editable);
    }

    private void showArchiveConfirmation() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("ITEM ARCHIVED");
        confirm.setHeaderText(null);
        confirm.setContentText("Item has been moved to archive.");
        confirm.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        confirm.getDialogPane().setStyle("-fx-background-color: white;");

        Button okButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-min-width: 100px;");

        confirm.showAndWait();
        System.out.println("Item archived!");
        handleClose();
    }
}