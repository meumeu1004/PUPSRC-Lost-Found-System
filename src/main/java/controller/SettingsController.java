package controller;

import dao.AdminDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Admin;
import database.DBConnection;
import controller.PasswordManager;

public class SettingsController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button        saveButton;

    private final AdminDAO adminDAO = new AdminDAO();

    @FXML
    private void handleSave() {

        String currentPw = currentPasswordField.getText();
        String newPw      = newPasswordField.getText();
        String confirmPw  = confirmPasswordField.getText();

        // ── 1. Basic validation ──────────────────────────────
        if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            showAlert("Validation Error", "All fields are required.");
            return;
        }

        if (!newPw.equals(confirmPw)) {
            showAlert("Validation Error", "New password and confirmation do not match.");
            newPasswordField.clear();
            confirmPasswordField.clear();
            return;
        }

        if (newPw.length() < 6) {
            showAlert("Validation Error", "New password must be at least 6 characters.");
            return;
        }

        boolean hasLetter = newPw.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = newPw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            showAlert("Validation Error", "Password must contain at least one letter and one number.");
            return;
        }

        // ── 2. Fetch admin and verify current password ───────
    try{
        Admin admin = adminDAO.getByUsername("admin");

        if (admin == null) {
            showAlert("Error", "Admin account not found.");
            return;
        }

        boolean currentOk = adminDAO.verifyPassword(currentPw, admin);

        if (!currentOk) {
            showAlert("Incorrect Password", "Current password is incorrect.");
            currentPasswordField.clear();
            return;
        }

        // ── 3. Hash the new password with bcrypt (cost 12) ───
        String newHash =
                org.mindrot.jbcrypt.BCrypt.hashpw(
                        newPw,
                        org.mindrot.jbcrypt.BCrypt.gensalt()
                );

        // ── 4. Save ──────────────────────────────────────────
        boolean saved = adminDAO.updatePassword(admin.getAdminId(), newHash);

        if (saved) {
            showAlert("Success", "Password changed successfully.");
            handleClose();
        } else {
            showAlert("Error", "Failed to update password. Please try again.");
        }

    } catch (DBConnection.NoConnectionException e) {

        showAlert("No Internet",
                "Please connect to the Internet and try again.");

    } catch (Exception e) {

        showAlert("Error",
                "Something went wrong. Please try again.");

        e.printStackTrace();
    }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}