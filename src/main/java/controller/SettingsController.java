package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class SettingsController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button saveButton;

    @FXML
    public void initialize() {
        // Add real-time validation with visual feedback
        currentPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateAndShowFeedback());
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateAndShowFeedback());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateAndShowFeedback());

        // Add Enter key handlers
        currentPasswordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSave();
            }
        });

        newPasswordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSave();
            }
        });

        confirmPasswordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSave();
            }
        });

        // Initially disable save button
        saveButton.setDisable(true);

        // Add tooltips
        currentPasswordField.setTooltip(new Tooltip("Enter your current password"));
        newPasswordField.setTooltip(new Tooltip("Password must be at least 6 characters with letters and numbers"));
        confirmPasswordField.setTooltip(new Tooltip("Re-enter your new password"));
    }

    private void validateAndShowFeedback() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean isCurrentPasswordCorrect = PasswordManager.verifyPassword(currentPassword);
        boolean isNewPasswordValid = isValidPassword(newPassword);
        boolean doPasswordsMatch = newPassword.equals(confirmPassword) && !newPassword.isEmpty();

        // Show real-time feedback
        if (!currentPassword.isEmpty() && !isCurrentPasswordCorrect) {
            currentPasswordField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            setTooltipText(currentPasswordField, "Current password is incorrect!");
        } else {
            currentPasswordField.setStyle("");
            if (isCurrentPasswordCorrect && !currentPassword.isEmpty()) {
                setTooltipText(currentPasswordField, "Current password correct ✓");
            } else {
                setTooltipText(currentPasswordField, "Enter your current password");
            }
        }

        if (!newPassword.isEmpty() && !isNewPasswordValid) {
            newPasswordField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            setTooltipText(newPasswordField, "Password must be 6+ chars with letters and numbers");
        } else if (!newPassword.isEmpty() && isNewPasswordValid) {
            newPasswordField.setStyle("-fx-border-color: green; -fx-border-radius: 5;");
            setTooltipText(newPasswordField, "Password valid ✓");
        } else {
            newPasswordField.setStyle("");
            setTooltipText(newPasswordField, "Enter new password (6+ chars, letters + numbers)");
        }

        if (!confirmPassword.isEmpty() && !doPasswordsMatch) {
            confirmPasswordField.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            setTooltipText(confirmPasswordField, "Passwords do not match!");
        } else if (!confirmPassword.isEmpty() && doPasswordsMatch) {
            confirmPasswordField.setStyle("-fx-border-color: green; -fx-border-radius: 5;");
            setTooltipText(confirmPasswordField, "Passwords match ✓");
        } else {
            confirmPasswordField.setStyle("");
            setTooltipText(confirmPasswordField, "Confirm your new password");
        }

        // Enable/disable save button
        saveButton.setDisable(!isCurrentPasswordCorrect || !isNewPasswordValid || !doPasswordsMatch);
    }

    private void setTooltipText(Control field, String text) {
        Tooltip tooltip = field.getTooltip();
        if (tooltip == null) {
            tooltip = new Tooltip(text);
            field.setTooltip(tooltip);
        } else {
            tooltip.setText(text);
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    @FXML
    private void handleSave() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Verify current password
        if (!PasswordManager.verifyPassword(currentPassword)) {
            PasswordManager.showWarningAlert(
                    "Invalid Current Password",
                    "The current password you entered is incorrect.",
                    "Please enter your correct current password to change it."
            );
            currentPasswordField.clear();
            currentPasswordField.requestFocus();
            return;
        }

        // Check if new password is empty
        if (newPassword.isEmpty()) {
            PasswordManager.showWarningAlert(
                    "Password Cannot Be Empty",
                    "New password cannot be empty.",
                    "Please enter a new password."
            );
            newPasswordField.requestFocus();
            return;
        }

        // Validate new password
        if (!isValidPassword(newPassword)) {
            PasswordManager.showWarningAlert(
                    "Weak Password",
                    "Password does not meet requirements.",
                    "Password must be at least 6 characters and contain both letters and numbers."
            );
            newPasswordField.clear();
            confirmPasswordField.clear();
            newPasswordField.requestFocus();
            return;
        }

        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            PasswordManager.showWarningAlert(
                    "Passwords Do Not Match",
                    "New password and confirmation password do not match.",
                    "Please make sure both passwords are identical."
            );
            confirmPasswordField.clear();
            confirmPasswordField.requestFocus();
            return;
        }

        // Check if new password is different from old
        if (newPassword.equals(PasswordManager.getCurrentPassword())) {
            PasswordManager.showWarningAlert(
                    "Same as Current Password",
                    "New password is the same as your current password.",
                    "Please choose a different password."
            );
            newPasswordField.clear();
            confirmPasswordField.clear();
            newPasswordField.requestFocus();
            return;
        }

        // Update password
        if (PasswordManager.updatePassword(currentPassword, newPassword)) {
            // Show success message
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Password Changed");
            successAlert.setHeaderText("✓ Password Changed Successfully");
            successAlert.setContentText("Your password has been updated.\nPlease use your new password for future logins.");
            successAlert.getDialogPane().setStyle("-fx-background-color: white;");

            Button okButton = (Button) successAlert.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");

            successAlert.showAndWait();

            // Close the dialog
            handleClose();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) currentPasswordField.getScene().getWindow();
        stage.close();
    }
}