package controller;

import dao.AuditLogDAO;
import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FoundItem;
import model.LostItem;
import util.PasswordGuard;
import database.DBConnection;
import controller.PasswordManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PostItemViewController {

    // ── FXML fields ──────────────────────────────────────────
    @FXML private Label              titleLabel;
    @FXML private Label              itemTypeLabel;
    @FXML private Label              itemDateLabel;
    @FXML private TextField          itemNameField;
    @FXML private ComboBox<String>   categoryPicker;
    @FXML private TextField          colorField;
    @FXML private TextField          reporterNameField;
    @FXML private TextField          contactNumberField;
    @FXML private TextField          emailField;
    @FXML private DatePicker         itemDatePicker;
    @FXML private TextArea           descArea;
    @FXML private Label              imagePlaceholderLabel;
    @FXML private ImageView          previewImageView;

    // ── Admin action buttons ──────────────────────────────────
    @FXML private Button             editButton;
    @FXML private Button             markFoundButton;
    @FXML private Button             claimButton;
    @FXML private Button             archiveButton;
    @FXML private Button             restoreButton;
    @FXML private Button             deleteButton;

    // ── State ─────────────────────────────────────────────────
    private LostItem        existingLost;
    private FoundItem       existingFound;
    private AdminController adminController;

    // ── DAOs ──────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // =========================================================
    // setItem() — called from AdminController
    // =========================================================
    public void setItem(Object item, AdminController adminCtrl) {
        this.adminController = adminCtrl;

        if (item instanceof LostItem lost) {
            this.existingLost = lost;
            populateFromLost(lost);
            configureButtons("Lost", lost.getItemStatus(), lost.getRecordStatus());
        } else if (item instanceof FoundItem found) {
            this.existingFound = found;
            populateFromFound(found);
            configureButtons("Found", found.getItemStatus(), found.getRecordStatus());
        }
    }

    // =========================================================
    // EDIT — opens PostItemForm pre-filled
    // =========================================================


    @FXML
    private void handleEdit() {

        // ── GUARD ─────────────────────────────────────────────
        if (!PasswordGuard.verify(
                editButton.getScene().getWindow(),
                "Edit Item",
                "Enter admin password to edit this item:")) return;
        // ── END GUARD ─────────────────────────────────────────

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PostItemForm.fxml"));
            Parent root = loader.load();

            PostItemFormController ctrl = loader.getController();

            if (existingLost != null) {
                ctrl.setMode("edit_lost", existingLost, adminController);
            } else {
                ctrl.setMode("edit_found", existingFound, adminController);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            handleClose(); // close view after edit completes
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MARK AS FOUND
    // =========================================================
    @FXML
    private void handleMarkAsFound() {
        if (existingLost == null) return;

        try {
            boolean ok = lostDAO.markFound(existingLost.getId());
            if (ok) {
                auditDAO.insertLog(existingLost.getId(), "Lost",
                        "Marked Found", "admin",
                        "{\"item_status\": \"Unresolved\"}",
                        "{\"item_status\": \"Found\"}");
                if (adminController != null) adminController.refreshDashboard();
                handleClose();
            } else {
                showAlert("Error", "Failed to update item status.");
            }
        } catch (DBConnection.NoConnectionException e) {
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");
        } catch (Exception e) {
            PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // CLAIM — opens ClaimDialog
    // =========================================================
    @FXML
    private void handleClaim() {
        if (existingFound == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ClaimDialog.fxml"));
            Parent root = loader.load();

            ClaimController ctrl = loader.getController();
            ctrl.setTargetItem(existingFound);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (adminController != null) adminController.refreshDashboard();
            handleClose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // ARCHIVE
    // =========================================================
    @FXML
    private void handleArchive() {
        if (!PasswordGuard.verify(
                archiveButton.getScene().getWindow(),
                "Archive Item",
                "Enter admin password to archive this item:")) return;

        // outer try — handles FXML loading
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ArchiveReasonDialog.fxml"));
            Parent root = loader.load();

            ArchiveReasonController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Archive Reason");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            String reason = ctrl.getSelectedReason();
            if (reason == null) return;

            // inner try — handles DAO calls
            try {
                if (existingLost != null) {
                    lostDAO.archive(existingLost.getId(), reason);
                    auditDAO.insertLog(existingLost.getId(), "Lost",
                            "Archived", "admin",
                            "{\"record_status\": \"Active\"}",
                            "{\"record_status\": \"Archived\", \"reason\": \"" + reason + "\"}");
                } else if (existingFound != null) {
                    foundDAO.archive(existingFound.getId(), reason);
                    auditDAO.insertLog(existingFound.getId(), "Found",
                            "Archived", "admin",
                            "{\"record_status\": \"Active\"}",
                            "{\"record_status\": \"Archived\", \"reason\": \"" + reason + "\"}");
                }
                if (adminController != null) adminController.refreshDashboard();
                handleClose();

            } catch (DBConnection.NoConnectionException e) {
                PasswordManager.showAlert("No Internet",
                        "Please connect to the Internet and try again.");
            } catch (Exception e) {
                PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace(); // FXML failed to load
        }
    }
  
    // =========================================================
    // RESTORE
    // =========================================================
    @FXML
    private void handleRestore() {
        if (!PasswordGuard.verify(
                restoreButton.getScene().getWindow(),
                "Restore Item",
                "Enter admin password to restore this item:")) return;

        try {
            if (existingLost != null) {
                lostDAO.restore(existingLost.getId());
                auditDAO.insertLog(existingLost.getId(), "Lost",
                        "Restored", "admin",
                        "{\"record_status\": \"Archived\"}",
                        "{\"record_status\": \"Active\"}");
            } else if (existingFound != null) {
                foundDAO.restore(existingFound.getId());
                auditDAO.insertLog(existingFound.getId(), "Found",
                        "Restored", "admin",
                        "{\"record_status\": \"Archived\"}",
                        "{\"record_status\": \"Active\"}");
            }
            if (adminController != null) adminController.refreshDashboard();
            handleClose();

        } catch (DBConnection.NoConnectionException e) {
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");
        } catch (Exception e) {
            PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // RESTORE
    // =========================================================
    @FXML
    private void handleRestore() {
        if (existingLost != null) {
            lostDAO.restore(existingLost.getId());
            auditDAO.insertLog(existingLost.getId(), "Lost",
                    "Restored", "admin",
                    "{\"record_status\": \"Archived\"}",
                    "{\"record_status\": \"Active\"}");
        } else if (existingFound != null) {
            foundDAO.restore(existingFound.getId());
            auditDAO.insertLog(existingFound.getId(), "Found",
                    "Restored", "admin",
                    "{\"record_status\": \"Archived\"}",
                    "{\"record_status\": \"Active\"}");
        }
        if (adminController != null) adminController.refreshDashboard();
        handleClose();
    }

    // =========================================================
    // DELETE (soft delete → Deleted)
    // =========================================================
    @FXML
    private void handleDelete() {

        // ── GUARD ─────────────────────────────────────────────
        if (!PasswordGuard.verify(
                deleteButton.getScene().getWindow(),
                "Delete Item",
                "Enter admin password to permanently delete this item:")) return;
        // ── END GUARD ─────────────────────────────────────────

        // Extra confirmation — deletion is harder to undo than archive
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Move this item to the Recycle Bin? It can be restored later.");
        confirm.getDialogPane().setStyle("-fx-background-color: white;");

        Button okBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            try {

                if (existingLost != null) {
                    lostDAO.delete(existingLost.getId());
                    auditDAO.insertLog(existingLost.getId(), "Lost",
                            "Deleted", "admin",
                            "{\"record_status\": \"Active\"}",
                            "{\"record_status\": \"Deleted\"}");

                } else if (existingFound != null) {
                    foundDAO.delete(existingFound.getId());
                    auditDAO.insertLog(existingFound.getId(), "Found",
                            "Deleted", "admin",
                            "{\"record_status\": \"Active\"}",
                            "{\"record_status\": \"Deleted\"}");
                }

                if (adminController != null) {
                    adminController.refreshDashboard();
                }

                handleClose();

            } catch (DBConnection.NoConnectionException e) {

                PasswordManager.showAlert(
                        "No Internet",
                        "Please connect to the Internet and try again."
                );

            } catch (Exception e) {

                PasswordManager.showAlert(
                        "Error",
                        "Something went wrong. Please try again."
                );

                e.printStackTrace();
            }
        });
    }



    // =========================================================
    // CLOSE
    // =========================================================
    @FXML
    private void handleClose() {
        Stage stage = (Stage) editButton.getScene().getWindow();
        stage.close();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Decides which buttons to show based on type, itemStatus,
     * and recordStatus. No gaps — hidden buttons use managed=false.
     */
    private void configureButtons(String type, String itemStatus, String recordStatus) {

        // Edit is always visible
        editButton.setVisible(true);
        editButton.setManaged(true);

        boolean isActive   = "Active".equals(recordStatus);
        boolean isArchived = "Archived".equals(recordStatus);

        // Mark as Found — lost items only, when Unresolved
        boolean showMarkFound = "Lost".equals(type)
                && "Unresolved".equals(itemStatus)
                && isActive;
        markFoundButton.setVisible(showMarkFound);
        markFoundButton.setManaged(showMarkFound);

        // Claim — found items only, when Unclaimed
        boolean showClaim = "Found".equals(type)
                && "Unclaimed".equals(itemStatus)
                && isActive;
        claimButton.setVisible(showClaim);
        claimButton.setManaged(showClaim);

        // Archive — only when Active
        archiveButton.setVisible(isActive);
        archiveButton.setManaged(isActive);

        // Restore — only when Archived
        restoreButton.setVisible(isArchived);
        restoreButton.setManaged(isArchived);

        // Delete — only when Archived
        deleteButton.setVisible(isArchived);
        deleteButton.setManaged(isArchived);
    }

    private void populateFromLost(LostItem item) {
        titleLabel.setText("LOST ITEM DETAILS");
        itemTypeLabel.setText("Lost");
        itemDateLabel.setText("Date Lost");

        itemNameField.setText(item.getItemName());
        categoryPicker.setValue(item.getCategory());
        colorField.setText(item.getColor());
        reporterNameField.setText(item.getOwnerName());
        contactNumberField.setText(item.getOwnerContactNum());
        emailField.setText(item.getOwnerContactEmail());
        descArea.setText(item.getDescription());

        if (item.getDateLost() != null) {
            itemDatePicker.setValue(item.getDateLost());
        }
        loadImage(item.getImagePath());
    }

    private void populateFromFound(FoundItem item) {
        titleLabel.setText("FOUND ITEM DETAILS");
        itemTypeLabel.setText("Found");
        itemDateLabel.setText("Date Found");

        itemNameField.setText(item.getItemName());
        categoryPicker.setValue(item.getCategory());
        colorField.setText(item.getColor());
        reporterNameField.setText(item.getFinderName());
        contactNumberField.setText(item.getFinderContactNum());
        emailField.setText(item.getFinderContactEmail());
        descArea.setText(item.getDescription());

        if (item.getDateFound() != null) {
            itemDatePicker.setValue(item.getDateFound());
        }
        loadImage(item.getImagePath());
    }

    private void loadImage(String path) {
        if (path != null && !path.isBlank()) {
            previewImageView.setImage(new Image("file:" + path));
            previewImageView.setVisible(true);
            imagePlaceholderLabel.setVisible(false);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
