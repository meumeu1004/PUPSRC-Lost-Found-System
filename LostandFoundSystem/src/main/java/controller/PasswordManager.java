package controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class PasswordManager {
    private static String currentAdminPassword = "admin123";

    public static boolean verifyPassword(String enteredPassword) {
        return enteredPassword != null && enteredPassword.equals(currentAdminPassword);
    }

    public static boolean updatePassword(String oldPassword, String newPassword) {
        if (oldPassword != null && oldPassword.equals(currentAdminPassword)) {
            currentAdminPassword = newPassword;
            return true;
        }
        return false;
    }

    public static String getCurrentPassword() {
        return currentAdminPassword;
    }

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