package controller;

import dao.AuditLogDAO;
import dao.ClaimDAO;
import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Claim;
import model.FoundItem;
import model.LostItem;
import util.PasswordGuard;
import database.DBConnection;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostItemViewArchiveController {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label     titleLabel;

    // ── Item Information ──────────────────────────────────────
    @FXML private Label     itemNameValue;
    @FXML private Label     categoryValue;
    @FXML private Label     colorValue;
    @FXML private Label     itemTypeValue;
    @FXML private Label     itemDateLabel;
    @FXML private Label     itemDateValue;
    @FXML private Label     itemStatusValue;
    @FXML private Label     descriptionValue;

    // ── Reporter / Finder Information ─────────────────────────
    @FXML private Label     reporterSectionLabel;
    @FXML private Label     reporterNameValue;
    @FXML private Label     contactValue;
    @FXML private Label     emailValue;

    // ── Record Information ────────────────────────────────────
    @FXML private Label     dateReportedValue;
    @FXML private Label     lastUpdatedValue;

    // ── Archive Info ──────────────────────────────────────────
    @FXML private Label     archiveReasonValue;
    @FXML private Label     archivedAtValue;
    @FXML private Button    editArchiveInfoButton;

    // ── Claimant Info ─────────────────────────────────────────
    @FXML private VBox      claimantSection;
    @FXML private Label     claimantNameValue;
    @FXML private Label     studentIdValue;
    @FXML private Label     claimantContactValue;
    @FXML private Label     claimantEmailValue;
    @FXML private Label     claimDateValue;
    @FXML private Label     verifiedByValue;
    @FXML private Label     remarksValue;
    @FXML private Button    editClaimInfoButton;

    // ── Image ─────────────────────────────────────────────────
    @FXML private Label     imagePlaceholderLabel;
    @FXML private ImageView previewImageView;

    // ── Action Buttons ────────────────────────────────────────
    @FXML private Button    restoreButton;
    @FXML private Button    deleteButton;

    // ── State ─────────────────────────────────────────────────
    private LostItem        existingLost;
    private FoundItem       existingFound;
    private Claim           existingClaim;
    private AdminController adminController;

    // ── DAOs ──────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();
    private final ClaimDAO     claimDAO = new ClaimDAO();

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy  hh:mm a");

    // =========================================================
    // setItem() — called from AdminController
    // =========================================================
    public void setItem(Object item, AdminController adminCtrl) {
        this.adminController = adminCtrl;

        if (item instanceof LostItem lost) {
            this.existingLost = lost;
            populateFromLost(lost);
            populateArchiveInfo(lost.getArchivedReason(), lost.getArchivedAt() != null
                    ? lost.getArchivedAt().format(DATETIME_FMT) : "—");

            // Lost items never have claimant info — hide section entirely
            claimantSection.setVisible(false);
            claimantSection.setManaged(false);

        } else if (item instanceof FoundItem found) {
            this.existingFound = found;
            populateFromFound(found);
            populateArchiveInfo(found.getArchivedReason(), found.getArchivedAt() != null
                    ? found.getArchivedAt().format(DATETIME_FMT) : "—");
            populateClaimantSection(found);
        }
    }

    // =========================================================
    // POPULATE
    // =========================================================
    private void populateFromLost(LostItem item) {
        titleLabel.setText("LOST ITEM DETAILS (ARCHIVED)");

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
        titleLabel.setText("FOUND ITEM DETAILS (ARCHIVED)");

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

    private void populateArchiveInfo(String reason, String archivedAt) {
        archiveReasonValue.setText(orDash(reason));
        archivedAtValue.setText(orDash(archivedAt));
    }

    private void populateClaimantSection(FoundItem item) {
        // Always show the section for found items
        claimantSection.setVisible(true);
        claimantSection.setManaged(true);

        if ("Claimed".equals(item.getItemStatus())) {
            try {
                List<Claim> claims = claimDAO.getClaimsByFoundItem(item.getId());
                if (!claims.isEmpty()) {
                    Claim claim = claims.get(0);
                    this.existingClaim = claim;

                    claimantNameValue.setText(orDash(claim.getClaimantName()));
                    studentIdValue.setText(orDash(claim.getStudentId()));
                    claimantContactValue.setText(orDash(claim.getClaimantContactNum()));
                    claimantEmailValue.setText(orDash(claim.getClaimantContactEmail()));
                    claimDateValue.setText(claim.getClaimDate() != null
                            ? claim.getClaimDate().format(DATE_FMT)
                            : "—");
                    verifiedByValue.setText(orDash(claim.getVerifiedBy()));
                    remarksValue.setText(orDash(claim.getRemarks()));

                    // Show Edit button only when there's actual claim data
                    editClaimInfoButton.setVisible(true);
                    editClaimInfoButton.setManaged(true);
                } else {
                    setNoClaimantPlaceholder();
                }
            } catch (DBConnection.NoConnectionException e) {
                setNoClaimantPlaceholder();
            } catch (Exception e) {
                setNoClaimantPlaceholder();
                e.printStackTrace();
            }
        } else {
            setNoClaimantPlaceholder();
        }
    }

    private void setNoClaimantPlaceholder() {
        claimantNameValue.setText("No claimant info recorded.");
        studentIdValue.setText("—");
        claimantContactValue.setText("—");
        claimantEmailValue.setText("—");
        claimDateValue.setText("—");
        verifiedByValue.setText("—");
        remarksValue.setText("—");

        editClaimInfoButton.setVisible(false);
        editClaimInfoButton.setManaged(false);
    }

    // =========================================================
    // EDIT ARCHIVE INFO
    // =========================================================
    @FXML
    private void handleEditArchiveInfo() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ArchiveReasonDialog.fxml"));
            Parent root = loader.load();

            ArchiveReasonController ctrl = loader.getController();

            // Pre-select the current reason
            String currentReason = archiveReasonValue.getText();
            if (!"—".equals(currentReason)) ctrl.preselect(currentReason);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Archive Reason");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            String newReason = ctrl.getSelectedReason();
            if (newReason == null || newReason.equals(currentReason)) return;

            try {
                if (existingLost != null) {
                    lostDAO.updateArchiveReason(existingLost.getId(), newReason);
                    auditDAO.insertLog(existingLost.getId(), "Lost",
                            "Edited Archive Reason", "admin",
                            "{\"archived_reason\": \"" + currentReason + "\"}",
                            "{\"archived_reason\": \"" + newReason + "\"}");
                } else if (existingFound != null) {
                    foundDAO.updateArchiveReason(existingFound.getId(), newReason);
                    auditDAO.insertLog(existingFound.getId(), "Found",
                            "Edited Archive Reason", "admin",
                            "{\"archived_reason\": \"" + currentReason + "\"}",
                            "{\"archived_reason\": \"" + newReason + "\"}");
                }
                // Refresh the label in-place
                archiveReasonValue.setText(newReason);

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
    // EDIT CLAIM INFO
    // =========================================================
    @FXML
    private void handleEditClaimInfo() {
        if (existingClaim == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/EditClaimInfoDialog.fxml"));
            Parent root = loader.load();

            EditClaimInfoController ctrl = loader.getController();
            ctrl.setClaim(existingClaim);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Claim Info");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (ctrl.isSaved()) {
                // Refresh claimant labels from the updated claim
                Claim updated = ctrl.getUpdatedClaim();
                claimantNameValue.setText(orDash(updated.getClaimantName()));
                studentIdValue.setText(orDash(updated.getStudentId()));
                claimantContactValue.setText(orDash(updated.getClaimantContactNum()));
                claimantEmailValue.setText(orDash(updated.getClaimantContactEmail()));
                claimDateValue.setText(updated.getClaimDate() != null
                        ? updated.getClaimDate().format(DATE_FMT)
                        : "—");
                verifiedByValue.setText(orDash(updated.getVerifiedBy()));
                remarksValue.setText(orDash(updated.getRemarks()));
                this.existingClaim = updated;
            }

        } catch (IOException e) {
            e.printStackTrace();
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
    // DELETE
    // =========================================================
    @FXML
    private void handleDelete() {
        if (!PasswordGuard.verify(
                deleteButton.getScene().getWindow(),
                "Delete Item",
                "Enter admin password to permanently delete this item:")) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Move this item to the Recycle Bin? It can be restored later.");
        confirm.getDialogPane().setStyle("-fx-background-color: white;");
        Button okBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null)
            okBtn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand;");

        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;
            try {
                if (existingLost != null) {
                    lostDAO.delete(existingLost.getId());
                    auditDAO.insertLog(existingLost.getId(), "Lost",
                            "Deleted", "admin",
                            "{\"record_status\": \"Archived\"}",
                            "{\"record_status\": \"Deleted\"}");
                } else if (existingFound != null) {
                    foundDAO.delete(existingFound.getId());
                    auditDAO.insertLog(existingFound.getId(), "Found",
                            "Deleted", "admin",
                            "{\"record_status\": \"Archived\"}",
                            "{\"record_status\": \"Deleted\"}");
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
        });
    }

    // =========================================================
    // CLOSE
    // =========================================================
    @FXML
    private void handleClose() {
        Stage stage = (Stage) restoreButton.getScene().getWindow();
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
                // image missing or unreadable — show placeholder
            }
        }
    }

    private String orDash(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }
}