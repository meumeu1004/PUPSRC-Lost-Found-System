package controller;

import dao.AdminDAO;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import model.Admin;

public class PasswordManager {

    private static final AdminDAO adminDAO = new AdminDAO();

    // =========================================================
    // VERIFY — now checks against the DB via bcrypt
    // Replaces the old hardcoded string comparison.
    // =========================================================
    public static boolean verifyPassword(String enteredPassword) {
        if (enteredPassword == null || enteredPassword.isBlank()) return false;

        Admin admin = adminDAO.getByUsername("admin");
        if (admin == null) return false;

        return adminDAO.verifyPassword(enteredPassword, admin);
    }

    // =========================================================
    // UPDATE — hashes the new password and saves it to the DB.
    // Old password is verified first before updating.
    // =========================================================
    public static boolean updatePassword(String oldPassword, String newPassword) {
        if (!verifyPassword(oldPassword)) return false;

        String newHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                newPassword,
                org.mindrot.jbcrypt.BCrypt.gensalt()
        );

        Admin admin = adminDAO.getByUsername("admin");
        if (admin == null) return false;

        return adminDAO.updatePassword(admin.getAdminId(), newHash);
    }

    // =========================================================
    // ALERT HELPERS — unchanged
    // =========================================================
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");
        }

        alert.showAndWait();
    }

    public static void showWarningAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");
        }

        alert.showAndWait();
    }
}