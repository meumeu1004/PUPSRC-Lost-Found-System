package controller;

import dao.AuditLogDAO;
import dao.ClaimDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.Claim;
import database.DBConnection;

public class EditClaimInfoController {

    // ── FXML fields ───────────────────────────────────────────
    @FXML private TextField     claimantNameField;
    @FXML private TextField     studentIdField;
    @FXML private TextField     contactField;
    @FXML private TextField     emailField;
    @FXML private DatePicker    claimDatePicker;
    @FXML private TextField     verifiedByField;
    @FXML private TextArea      remarksArea;
    @FXML private Label         proofPlaceholder;
    @FXML private ImageView     proofPreview;
    @FXML private Button        removeProofButton;
    private String              proofImagePath;

    // ── State ─────────────────────────────────────────────────
    private Claim   originalClaim;
    private Claim   updatedClaim;
    private boolean saved = false;

    // ── DAOs ──────────────────────────────────────────────────
    private final ClaimDAO    claimDAO = new ClaimDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();

    // =========================================================
    // setClaim() — called from PostItemViewArchiveController
    // =========================================================
    public void setClaim(Claim claim) {
        this.originalClaim = claim;

        claimantNameField.setText(orEmpty(claim.getClaimantName()));
        studentIdField.setText(orEmpty(claim.getStudentId()));
        contactField.setText(orEmpty(claim.getClaimantContactNum()));
        emailField.setText(orEmpty(claim.getClaimantContactEmail()));
        claimDatePicker.setValue(claim.getClaimDate());
        verifiedByField.setText(orEmpty(claim.getVerifiedBy()));
        remarksArea.setText(orEmpty(claim.getRemarks()));

        this.proofImagePath = claim.getProofImagePath();
        if (proofImagePath != null && !proofImagePath.isBlank()) {
            proofPreview.setImage(new javafx.scene.image.Image("file:" + proofImagePath));
            proofPreview.setVisible(true);
            proofPlaceholder.setVisible(false);
            removeProofButton.setVisible(true);
            removeProofButton.setManaged(true);
        }

    }

    // =========================================================
    // SAVE
    // =========================================================
    @FXML
    private void handleSave() {

        String name    = claimantNameField.getText().trim();
        String contact = contactField.getText().trim();
        String email   = emailField.getText().trim();

        if (name.isBlank()) {
            PasswordManager.showAlert("Validation Error", "Claimant name is required.");
            return;
        }
        if (verifiedByField.getText().trim().isBlank()) {
            PasswordManager.showAlert("Validation Error", "Verifier Identity is required.");
            return;
        }
        if (contact.isBlank() && email.isBlank()) {
            PasswordManager.showAlert("Validation Error",
                    "Please provide at least a contact number or an email address.");
            return;
        }
        if (!email.isBlank() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            PasswordManager.showAlert("Validation Error", "Invalid email format.");
            return;
        }
        if (!contact.isBlank() && !contact.matches("^09\\d{9}$")) {
            PasswordManager.showAlert("Validation Error",
                    "Invalid contact number format. Use 09XXXXXXXXX.");
            return;
        }

        if (proofImagePath == null || proofImagePath.isBlank()) {
            PasswordManager.showAlert("Validation Error", "Proof of ownership image is required.");
            return;
        }

        // Build updated Claim from fields
        Claim updated = new Claim(
                originalClaim.getClaimId(),
                originalClaim.getFoundItemId(),
                claimantNameField.getText().trim(),
                studentIdField.getText().trim(),
                contactField.getText().trim(),
                emailField.getText().trim(),
                proofImagePath,   // proof image unchanged
                claimDatePicker.getValue(),
                verifiedByField.getText().trim(),
                remarksArea.getText().trim(),
                originalClaim.getCreatedAt(),
                originalClaim.getUpdatedAt()
        );

        try {
            boolean ok = claimDAO.updateClaimInfo(updated);
            if (ok) {
                auditDAO.insertLog(
                        originalClaim.getFoundItemId(), "Found",
                        "Edited Claim Info", "admin",
                        buildJson(originalClaim),
                        buildJson(updated)
                );
                this.updatedClaim = updated;
                this.saved = true;
                closeStage();
            } else {
                PasswordManager.showAlert("Error", "Failed to save changes. Please try again.");
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
    // CANCEL
    // =========================================================
    @FXML
    private void handleCancel() {
        closeStage();
    }

    // =========================================================
    // IMAGE UPLOAD
    // =========================================================
    @FXML
    private void handleUploadProof() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select Proof of Ownership");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        java.io.File file = chooser.showOpenDialog(claimantNameField.getScene().getWindow());
        if (file != null) {
            proofImagePath = file.getAbsolutePath();
            proofPreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
            proofPreview.setVisible(true);
            proofPlaceholder.setVisible(false);
            removeProofButton.setVisible(true);
            removeProofButton.setManaged(true);
        }
    }

    @FXML
    private void handleRemoveProof() {
        proofImagePath = null;
        proofPreview.setImage(null);
        proofPreview.setVisible(false);
        proofPlaceholder.setVisible(true);
        removeProofButton.setVisible(false);
        removeProofButton.setManaged(false);
    }

    // =========================================================
    // RESULTS — read by PostItemViewArchiveController after close
    // =========================================================
    public boolean isSaved()          { return saved; }
    public Claim   getUpdatedClaim()  { return updatedClaim; }

    // =========================================================
    // HELPERS
    // =========================================================
    private void closeStage() {
        Stage stage = (Stage) claimantNameField.getScene().getWindow();
        stage.close();
    }

    private String orEmpty(String value) {
        return (value != null) ? value : "";
    }

    private String buildJson(Claim c) {
        return String.format(
                "{\"claimant_name\": \"%s\", \"student_id\": \"%s\", " +
                        "\"contact\": \"%s\", \"email\": \"%s\", " +
                        "\"claim_date\": \"%s\", \"verified_by\": \"%s\"}",
                orEmpty(c.getClaimantName()),
                orEmpty(c.getStudentId()),
                orEmpty(c.getClaimantContactNum()),
                orEmpty(c.getClaimantContactEmail()),
                c.getClaimDate() != null ? c.getClaimDate().toString() : "",
                orEmpty(c.getVerifiedBy())
        );
    }
}