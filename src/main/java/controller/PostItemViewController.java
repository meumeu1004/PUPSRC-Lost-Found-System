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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FoundItem;
import model.LostItem;
import util.PasswordGuard;
import database.DBConnection;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PostItemViewController {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label     titleLabel;

    // ── Item Information ──────────────────────────────────────
    @FXML private Label     itemNameValue;
    @FXML private Label     categoryValue;
    @FXML private Label     colorValue;
    @FXML private Label     itemTypeValue;
    @FXML private Label     itemDateLabel;       // "Date Lost" or "Date Found"
    @FXML private Label     itemDateValue;
    @FXML private Label     itemStatusValue;
    @FXML private Label     descriptionValue;

    // ── Reporter / Finder Information ─────────────────────────
    @FXML private Label     reporterSectionLabel; // "Reporter Information" / "Finder Information"
    @FXML private Label     reporterNameValue;
    @FXML private Label     contactValue;
    @FXML private Label     emailValue;

    // ── Record Information ────────────────────────────────────
    @FXML private Label     dateReportedValue;
    @FXML private Label     lastUpdatedValue;

    // ── Image ─────────────────────────────────────────────────
    @FXML private Label     imagePlaceholderLabel;
    @FXML private ImageView previewImageView;

    // ── Find Possible Matches row ─────────────────────────────
    @FXML private HBox      findMatchesRow;
    @FXML private Button    findMatchesButton;

    // ── Action Buttons ────────────────────────────────────────
    @FXML private Button    editButton;
    @FXML private Button    markFoundButton;
    @FXML private Button    claimButton;
    @FXML private Button    archiveButton;

    // ── State ─────────────────────────────────────────────────
    private LostItem        existingLost;
    private FoundItem       existingFound;
    private AdminController adminController;

    // ── DAOs ──────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a");

    // =========================================================
    // setItem() — called from AdminController or card controllers
    // =========================================================
    public void setItem(Object item, AdminController adminCtrl) {
        this.adminController = adminCtrl;

        if (item instanceof LostItem lost) {
            this.existingLost = lost;
            populateFromLost(lost);
            configureButtons("Lost", lost.getItemStatus());
        } else if (item instanceof FoundItem found) {
            this.existingFound = found;
            populateFromFound(found);
            configureButtons("Found", found.getItemStatus());
        }
    }

    // =========================================================
    // POPULATE
    // =========================================================
    private void populateFromLost(LostItem item) {
        titleLabel.setText("LOST ITEM DETAILS");

        itemNameValue.setText(orDash(item.getItemName()));
        categoryValue.setText(orDash(item.getCategory()));
        colorValue.setText(orDash(item.getColor()));
        itemTypeValue.setText("Lost");
        itemDateLabel.setText("Date Lost");
        itemDateValue.setText(item.getDateLost() != null
                ? item.getDateLost().format(DATE_FMT) : "—");
        itemStatusValue.setText(orDash(item.getItemStatus()));
        descriptionValue.setText(orDash(item.getDescription()));

        reporterSectionLabel.setText("Reporter Information");
        reporterNameValue.setText(orDash(item.getOwnerName()));
        contactValue.setText(orDash(item.getOwnerContactNum()));
        emailValue.setText(orDash(item.getOwnerContactEmail()));

        dateReportedValue.setText(item.getCreatedAt() != null
                ? item.getCreatedAt().format(DATETIME_FMT) : "—");
        lastUpdatedValue.setText(item.getUpdatedAt() != null
                ? item.getUpdatedAt().format(DATETIME_FMT) : "—");

        loadImage(item.getImagePath());
    }

    private void populateFromFound(FoundItem item) {
        titleLabel.setText("FOUND ITEM DETAILS");

        itemNameValue.setText(orDash(item.getItemName()));
        categoryValue.setText(orDash(item.getCategory()));
        colorValue.setText(orDash(item.getColor()));
        itemTypeValue.setText("Found");
        itemDateLabel.setText("Date Found");
        itemDateValue.setText(item.getDateFound() != null
                ? item.getDateFound().format(DATE_FMT) : "—");
        itemStatusValue.setText(orDash(item.getItemStatus()));
        descriptionValue.setText(orDash(item.getDescription()));

        reporterSectionLabel.setText("Finder Information");
        reporterNameValue.setText(orDash(item.getFinderName()));
        contactValue.setText(orDash(item.getFinderContactNum()));
        emailValue.setText(orDash(item.getFinderContactEmail()));

        dateReportedValue.setText(item.getCreatedAt() != null
                ? item.getCreatedAt().format(DATETIME_FMT) : "—");
        lastUpdatedValue.setText(item.getUpdatedAt() != null
                ? item.getUpdatedAt().format(DATETIME_FMT) : "—");

        loadImage(item.getImagePath());
    }

    // =========================================================
    // CONFIGURE BUTTONS
    // =========================================================
    private void configureButtons(String type, String itemStatus) {

        // Find Possible Matches — always shown for active items
        findMatchesRow.setVisible(true);
        findMatchesRow.setManaged(true);

        // Mark as Found — lost + unresolved only
        boolean showMarkFound = "Lost".equals(type) && "Unresolved".equals(itemStatus);
        markFoundButton.setVisible(showMarkFound);
        markFoundButton.setManaged(showMarkFound);

        // Claim — found + unclaimed only
        boolean showClaim = "Found".equals(type) && "Unclaimed".equals(itemStatus);
        claimButton.setVisible(showClaim);
        claimButton.setManaged(showClaim);

        // Archive always shown for active items (this view is active-only)
        archiveButton.setVisible(true);
        archiveButton.setManaged(true);
    }

    // =========================================================
    // FIND POSSIBLE MATCHES
    // =========================================================
    @FXML
    private void handleFindMatches() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PossibleMatchesDialog.fxml"));
            Parent root = loader.load();

            PossibleMatchesController ctrl = loader.getController();
            Object source = (existingLost != null) ? existingLost : existingFound;
            ctrl.init(source, adminController);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Find Possible Matches");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // EDIT
    // =========================================================
    @FXML
    private void handleEdit() {
        if (!PasswordGuard.verify(
                editButton.getScene().getWindow(),
                "Edit Item",
                "Enter admin password to edit this item:")) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PostItemForm.fxml"));
            Parent root = loader.load();

            PostItemFormController ctrl = loader.getController();
            if (existingLost != null)
                ctrl.setMode("edit_lost", existingLost, adminController);
            else
                ctrl.setMode("edit_found", existingFound, adminController);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            handleClose();
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
    // CLAIM
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
            e.printStackTrace();
        }
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
    // HELPERS
    // =========================================================
    private void loadImage(String path) {
        if (path != null && !path.isBlank()) {
            try {
                previewImageView.setImage(new Image("file:" + path));
                previewImageView.setVisible(true);
                imagePlaceholderLabel.setVisible(false);
            } catch (Exception e) {
                // image file missing or unreadable — show placeholder
            }
        }
    }

    private String orDash(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}