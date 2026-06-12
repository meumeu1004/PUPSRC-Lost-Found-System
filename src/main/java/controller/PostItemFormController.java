package controller;

import dao.AuditLogDAO;
import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.FoundItem;
import model.LostItem;
import database.DBConnection;
import controller.PasswordManager;

import java.io.File;
import java.time.LocalDate;

public class PostItemFormController {

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
    @FXML private Button             removeImageButton;
    @FXML private Button             saveButton;

    // ── State ─────────────────────────────────────────────────
    private String          mode;           // new_lost | new_found | edit_lost | edit_found
    private LostItem        existingLost;
    private FoundItem       existingFound;
    private AdminController adminController;
    private String          imagePath;

    // ── DAOs ──────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        categoryPicker.getItems().addAll(
                "Electronics", "Clothing", "Accessories",
                "Books", "ID/Documents", "Keys", "Bag", "Others"
        );
    }

    // =========================================================
    // setMode()
    // =========================================================
    public void setMode(String mode, Object existing, AdminController adminCtrl) {
        this.mode            = mode;
        this.adminController = adminCtrl;

        if (existing instanceof LostItem  l) this.existingLost  = l;
        if (existing instanceof FoundItem f) this.existingFound = f;

        switch (mode) {

            case "new_lost" -> {
                titleLabel.setText("POST LOST ITEM");
                itemTypeLabel.setText("Lost");
                itemDateLabel.setText("Date Lost");
                itemDatePicker.setPromptText("Select date item was lost");
                saveButton.setText("SUBMIT REPORT");
                itemDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                    @Override public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isAfter(LocalDate.now()));
                    }
                });
            }

            case "new_found" -> {
                titleLabel.setText("POST FOUND ITEM");
                itemTypeLabel.setText("Found");
                itemDateLabel.setText("Date Found");
                itemDatePicker.setPromptText("Select date item was found");
                saveButton.setText("SUBMIT REPORT");
                itemDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                    @Override public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isAfter(LocalDate.now()));
                    }
                });
            }

            case "edit_lost" -> {
                titleLabel.setText("EDIT LOST ITEM");
                itemTypeLabel.setText("Lost");
                itemDateLabel.setText("Date Lost");
                saveButton.setText("SAVE CHANGES");
                populateFromLost(existingLost);
                itemDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                    @Override public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isAfter(LocalDate.now()));
                    }
                });
            }

            case "edit_found" -> {
                titleLabel.setText("EDIT FOUND ITEM");
                itemTypeLabel.setText("Found");
                itemDateLabel.setText("Date Found");
                saveButton.setText("SAVE CHANGES");
                populateFromFound(existingFound);
                itemDatePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
                    @Override public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isAfter(LocalDate.now()));
                    }
                });
            }
        }
    }

    // =========================================================
    // SAVE
    // =========================================================
    @FXML
    private void handleSave() {
        if (!validateFields()) return;

        LocalDate pickedDate = itemDatePicker.getValue() != null
                ? itemDatePicker.getValue()
                : LocalDate.now();

        try {

            switch (mode) {

                case "new_lost" -> {
                    LostItem item = new LostItem(
                            0,
                            itemNameField.getText().trim(),
                            categoryPicker.getValue(),
                            descArea.getText().trim(),
                            colorField.getText().trim(),
                            imagePath,
                            "Active",
                            null,
                            null,
                            null,
                            null,
                            "Unresolved",
                            reporterNameField.getText().trim(),
                            contactNumberField.getText().trim(),
                            emailField.getText().trim(),
                            pickedDate
                    );
                    boolean ok = lostDAO.insert(item);
                    if (ok) auditDAO.insertLog(0, "Lost", "Created Report",
                            "user", null,
                            "{\"item_name\": \"" + item.getItemName() + "\"}");
                    handleResult(ok, "Lost item report submitted.");
                }

                case "new_found" -> {
                    FoundItem item = new FoundItem(
                            0,
                            itemNameField.getText().trim(),
                            categoryPicker.getValue(),
                            descArea.getText().trim(),
                            colorField.getText().trim(),
                            imagePath,
                            "Active",
                            "Unclaimed",
                            null,
                            null,
                            null,
                            null,
                            reporterNameField.getText().trim(),
                            contactNumberField.getText().trim(),
                            emailField.getText().trim(),
                            pickedDate
                    );
                    boolean ok = foundDAO.insert(item);
                    if (ok) auditDAO.insertLog(0, "Found", "Created Report",
                            "user", null,
                            "{\"item_name\": \"" + item.getItemName() + "\"}");
                    handleResult(ok, "Found item report submitted.");
                }

                case "edit_lost" -> {
                    LostItem updated = new LostItem(
                            existingLost.getId(),
                            itemNameField.getText().trim(),
                            categoryPicker.getValue(),
                            descArea.getText().trim(),
                            colorField.getText().trim(),
                            imagePath != null ? imagePath : existingLost.getImagePath(),
                            existingLost.getRecordStatus(),
                            existingLost.getCreatedAt(),
                            existingLost.getUpdatedAt(),
                            existingLost.getArchivedReason(),
                            existingLost.getArchivedAt(),
                            existingLost.getItemStatus(),
                            reporterNameField.getText().trim(),
                            contactNumberField.getText().trim(),
                            emailField.getText().trim(),
                            pickedDate
                    );
                    boolean ok = lostDAO.update(updated);
                    if (ok) auditDAO.insertLog(updated.getId(), "Lost",
                            "Updated Item Details", "admin",
                            buildLostJson(existingLost),
                            buildLostJson(updated));
                    handleResult(ok, "Lost item updated.");
                }

                case "edit_found" -> {
                    FoundItem updated = new FoundItem(
                            existingFound.getId(),
                            itemNameField.getText().trim(),
                            categoryPicker.getValue(),
                            descArea.getText().trim(),
                            colorField.getText().trim(),
                            imagePath != null ? imagePath : existingFound.getImagePath(),
                            existingFound.getRecordStatus(),
                            existingFound.getItemStatus(),
                            existingFound.getCreatedAt(),
                            existingFound.getUpdatedAt(),
                            existingFound.getArchivedReason(),
                            existingFound.getArchivedAt(),
                            reporterNameField.getText().trim(),
                            contactNumberField.getText().trim(),
                            emailField.getText().trim(),
                            pickedDate
                    );
                    boolean ok = foundDAO.update(updated);
                    if (ok) auditDAO.insertLog(updated.getId(), "Found",
                            "Updated Item Details", "admin",
                            buildFoundJson(existingFound),
                            buildFoundJson(updated));
                    handleResult(ok, "Found item updated.");
                }
            }

        } catch (DBConnection.NoConnectionException e) {
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");

        } catch (Exception e) {
            PasswordManager.showAlert("Error",
                    "Something went wrong. Please try again.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // IMAGE UPLOAD
    // =========================================================
    @FXML
    private void handleUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Item Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(imagePlaceholderLabel.getScene().getWindow());
        if (file != null) {
            imagePath = file.getAbsolutePath();
            previewImageView.setImage(new Image(file.toURI().toString()));
            previewImageView.setVisible(true);
            imagePlaceholderLabel.setVisible(false);
            removeImageButton.setVisible(true);
            removeImageButton.setManaged(true);
        }
    }

    @FXML
    private void handleRemoveImage() {
        imagePath = null;
        previewImageView.setImage(null);
        previewImageView.setVisible(false);
        imagePlaceholderLabel.setVisible(true);
        removeImageButton.setVisible(false);
        removeImageButton.setManaged(false);
    }

    // =========================================================
    // CLOSE
    // =========================================================
    @FXML
    private void handleClose() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================
    private void populateFromLost(LostItem item) {
        itemNameField.setText(item.getItemName());
        categoryPicker.setValue(item.getCategory());
        colorField.setText(item.getColor());
        reporterNameField.setText(item.getOwnerName());
        contactNumberField.setText(item.getOwnerContactNum());
        emailField.setText(item.getOwnerContactEmail());
        descArea.setText(item.getDescription());
        if (item.getDateLost() != null) itemDatePicker.setValue(item.getDateLost());
        if (item.getImagePath() != null) {
            imagePath = item.getImagePath();
            previewImageView.setImage(new Image("file:" + imagePath));
            previewImageView.setVisible(true);
            imagePlaceholderLabel.setVisible(false);
        }
    }

    private void populateFromFound(FoundItem item) {
        itemNameField.setText(item.getItemName());
        categoryPicker.setValue(item.getCategory());
        colorField.setText(item.getColor());
        reporterNameField.setText(item.getFinderName());
        contactNumberField.setText(item.getFinderContactNum());
        emailField.setText(item.getFinderContactEmail());
        descArea.setText(item.getDescription());
        if (item.getDateFound() != null) itemDatePicker.setValue(item.getDateFound());
        if (item.getImagePath() != null) {
            imagePath = item.getImagePath();
            previewImageView.setImage(new Image("file:" + imagePath));
            previewImageView.setVisible(true);
            imagePlaceholderLabel.setVisible(false);
        }
    }

    private boolean validateFields() {
        if (itemNameField.getText().isBlank()) {
            showAlert("Validation Error", "Item name is required.");
            return false;
        }
        if ((mode.equals("new_found") || mode.equals("edit_found")) && imagePath == null) {
            showAlert("Validation Error", "Image is required for found item reports.");
            return false;
        }
        if (categoryPicker.getValue() == null) {
            showAlert("Validation Error", "Category is required.");
            return false;
        }
        if (colorField.getText().isBlank()) {
            showAlert("Validation Error", "Color is required.");
            return false;
        }
        if (!colorField.getText().trim().matches("^[a-zA-Z\\s]+$")) {
            showAlert("Validation Error", "Color must contain letters only.");
            return false;
        }
        if (!reporterNameField.getText().trim().matches("^[a-zA-Z\\s.,-]+$")) {
            showAlert("Validation Error", "Reporter name must contain letters only.");
            return false;
        }
        if (descArea.getText().isBlank() || descArea.getText().trim().length() < 10) {
            showAlert("Validation Error", "Description must be at least 10 characters.");
            return false;
        }
        if (reporterNameField.getText().isBlank()) {
            showAlert("Validation Error", "Reporter name is required.");
            return false;
        }
        String contactNum = contactNumberField.getText().trim();
        String email      = emailField.getText().trim();
        if (contactNum.isBlank() && email.isBlank()) {
            showAlert("Validation Error",
                    "Please provide at least a contact number or an email address.");
            return false;
        }
        if (!email.isBlank() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showAlert("Validation Error", "Invalid email format.");
            return false;
        }
        if (!contactNum.isBlank() && !contactNum.matches("^09\\d{9}$")) {
            showAlert("Validation Error",
                    "Invalid contact number format. Use 09XXXXXXXXX.");
            return false;
        }
        return true;
    }

    private void handleResult(boolean ok, String successMessage) {
        if (ok) {
            showAlert("Success", successMessage);
            if (adminController != null) adminController.refreshDashboard();
            handleClose();
        } else {
            showAlert("Error", "Database operation failed. Please try again.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================
    // AUDIT LOG HELPERS
    // =========================================================
    private String buildLostJson(LostItem item) {
        return String.format(
                "{\"item_name\": \"%s\", \"category\": \"%s\", \"color\": \"%s\", " +
                        "\"description\": \"%s\", \"owner_name\": \"%s\", " +
                        "\"contact\": \"%s\", \"email\": \"%s\", \"date_lost\": \"%s\"}",
                orEmpty(item.getItemName()),
                orEmpty(item.getCategory()),
                orEmpty(item.getColor()),
                orEmpty(item.getDescription()).replace("\"", "'"),
                orEmpty(item.getOwnerName()),
                orEmpty(item.getOwnerContactNum()),
                orEmpty(item.getOwnerContactEmail()),
                item.getDateLost() != null ? item.getDateLost().toString() : ""
        );
    }

    private String buildFoundJson(FoundItem item) {
        return String.format(
                "{\"item_name\": \"%s\", \"category\": \"%s\", \"color\": \"%s\", " +
                        "\"description\": \"%s\", \"finder_name\": \"%s\", " +
                        "\"contact\": \"%s\", \"email\": \"%s\", \"date_found\": \"%s\"}",
                orEmpty(item.getItemName()),
                orEmpty(item.getCategory()),
                orEmpty(item.getColor()),
                orEmpty(item.getDescription()).replace("\"", "'"),
                orEmpty(item.getFinderName()),
                orEmpty(item.getFinderContactNum()),
                orEmpty(item.getFinderContactEmail()),
                item.getDateFound() != null ? item.getDateFound().toString() : ""
        );
    }

    private String orEmpty(String value) {
        return (value != null) ? value : "";
    }

}