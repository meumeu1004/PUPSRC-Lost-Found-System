package util;

import controller.PasswordManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/**
 * PasswordGuard — reusable admin password prompt.
 * Delegates verification to PasswordManager (which checks the DB).
 *
 * Usage in any controller:
 *   if (!PasswordGuard.verify(anyNode.getScene().getWindow())) return;
 *   // ... proceed with the protected action
 */
public class PasswordGuard {

    // =========================================================
    // DEFAULT — generic title/message
    // =========================================================
    public static boolean verify(Window owner) {
        return verify(owner, "Admin Verification", "Enter the admin password to continue:");
    }

    // =========================================================
    // OVERLOAD — custom title and message per action
    // =========================================================
    public static boolean verify(Window owner, String title, String message) {

        // Build dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(owner);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: white;");

        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Password");
        pwField.setStyle(
                "-fx-background-color: #f5f5f5;" +
                        "-fx-border-color: #ddd;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 8;"
        );

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #710912; -fx-font-size: 11px;");
        errorLabel.setVisible(false);

        VBox content = new VBox(8, msgLabel, pwField, errorLabel);
        content.setPadding(new Insets(16, 20, 8, 20));
        pane.setContent(content);

        ButtonType confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType  = new ButtonType("Cancel",  ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(confirmType, cancelType);

        Button confirmBtn = (Button) pane.lookupButton(confirmType);
        confirmBtn.setStyle(
                "-fx-background-color: #710912;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 5;"
        );
        ((Button) pane.lookupButton(cancelType)).setStyle("-fx-cursor: hand;");

        // Focus password field on open
        dialog.setOnShown(e -> pwField.requestFocus());

        // Block close if field is empty
        confirmBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (pwField.getText().isBlank()) {
                errorLabel.setText("Password cannot be empty.");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> btn == confirmType ? pwField.getText() : null);

        // Show dialog and verify via PasswordManager
        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() == null) return false;

        boolean ok = PasswordManager.verifyPassword(result.get());

        if (!ok) {
            PasswordManager.showAlert("Access Denied", "Incorrect password. Action cancelled.");
        }

        return ok;
    }
}