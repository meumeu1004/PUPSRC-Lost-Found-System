package controller;

import dao.AuditLogDAO;
import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import model.FoundItem;
import model.LostItem;
import util.PasswordGuard;
import database.DBConnection;
import controller.PasswordManager;
import controller.ArchiveItemCardController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class AdminController {

  // ── FXML fields ──────────────────────────────────────────
    @FXML private Label     dashboardTitleLabel;
    @FXML private Label     totalLostLabel;
    @FXML private Label     totalFoundLabel;
    @FXML private Label     totalUnresolvedLabel;
    @FXML private Label     totalLostLabelText;
    @FXML private Label     totalFoundLabelText;

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> typeCombo;

    @FXML private GridPane itemGrid;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label pageLabel;

    @FXML private Label aboutUsLink;
    @FXML private Label termsLink;
    @FXML private Label privacyLink;

    @FXML private StackPane normalPostLost;
    @FXML private StackPane normalPostFound;
    @FXML private StackPane normalArchive;
    @FXML private StackPane archiveBackToMain;
    @FXML private StackPane archiveRecentlyDeleted;

    @FXML private Button archiveButton;
    @FXML private Button backToMainButton;
    @FXML private Button recentlyDeletedButton;
 

    // ── DAOs ─────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();

    // ── Pagination state ──────────────────────────────────────
    private static final int PAGE_SIZE = 8;
    private int currentPage = 0;

    // All items currently loaded (after filter/search applied)
    private List<Object> allItems = new ArrayList<>();  // holds LostItem or FoundItem

    // ── View mode ────────────────────────────────────────────
    private boolean showingArchive = false;

    // ── Recycle Bin / Soft Deleted View ───────────────────────────────
    private boolean showingDeleted = false;

    // Date formatter
    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // =========================================================
    // INITIALIZE
    // =========================================================
    public void refreshDashboard() {loadDashboard();}

    @FXML
    public void initialize() {

        categoryCombo.getItems().addAll(
                "All Category", "Electronics", "Clothing", "Accessories",
                "Books", "ID/Documents", "Keys", "Bag", "Others"
        );
        categoryCombo.setValue("All Category");

        sortCombo.getItems().addAll("Newest", "Oldest", "Name A-Z", "Name Z-A");
        sortCombo.setValue("Newest");

        typeCombo.getItems().addAll("All Types", "Lost", "Found");
        typeCombo.setValue("All Types");
        typeCombo.getStyleClass().add("compact-dropdown");
        typeCombo.setStyle("-fx-padding: 0; -fx-cell-size: 25px;");
        typeCombo.setPadding(new Insets(0));
        typeCombo.setOnAction(e -> applyFilters());

        sortCombo.setOnAction(e -> applyFilters());
        categoryCombo.setOnAction(e -> applyFilters());
        typeCombo.setOnAction(e -> applyFilters());

        loadDashboard();

        aboutUsLink.setOnMouseClicked(e -> showInfoDialog("About Us", getAboutUsContent()));
        termsLink.setOnMouseClicked(e -> showInfoDialog("Terms of Service", getTermsContent()));
        privacyLink.setOnMouseClicked(e -> showInfoDialog("Privacy Policy", getPrivacyContent()));
    }

    // =========================================================
    // LOAD — pulls from DB and refreshes grid
    // =========================================================
    private void loadDashboard() {
        try {
            totalLostLabel.setText(String.valueOf(lostDAO.countActive()));
            totalFoundLabel.setText(String.valueOf(foundDAO.countActive()));
            totalUnresolvedLabel.setText(String.valueOf(
                    lostDAO.countUnresolved() + foundDAO.countUnclaimed()));

            applyFilters();

        } catch (DBConnection.NoConnectionException e) {
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");
        } catch (Exception e) {
            PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String category   = categoryCombo.getValue();
        String type       = typeCombo.getValue();
        String sortRaw    = sortCombo.getValue();
        String keywordRaw = searchField.getText();
        String keyword    = (keywordRaw == null || keywordRaw.isBlank())
                ? null : keywordRaw.trim();

        String cat  = (category == null || category.equals("All Category")) ? null : category;
        String sort = switch (sortRaw == null ? "Newest" : sortRaw) {
            case "Oldest"   -> "oldest";
            case "Name A-Z" -> "name_asc";
            case "Name Z-A" -> "name_desc";
            default         -> "newest";
        };

        allItems = new ArrayList<>();

        if (!showingArchive) {
            // ── Dashboard: simple Lost / Found / All ─────────────
            if ("Lost".equals(type)) {
                allItems.addAll(lostDAO.filter(keyword, cat, null, sort));
            } else if ("Found".equals(type)) {
                allItems.addAll(foundDAO.filter(keyword, cat, null, sort));
            } else {
                allItems.addAll(lostDAO.filter(keyword, cat, null, sort));
                allItems.addAll(foundDAO.filter(keyword, cat, null, sort));
            }
        } else {
            // ── Archive: granular status options ──────────────────
            switch (type == null ? "All Types" : type) {

                case "All Lost" ->
                        allItems.addAll(lostDAO.getAllArchived());

                case "All Found" ->
                        allItems.addAll(foundDAO.getAllArchived());

                case "Resolved Lost" ->
                        allItems.addAll(lostDAO.filterArchived(keyword, cat, "Found", sort));

                case "Claimed Found" ->
                        allItems.addAll(foundDAO.filterArchived(keyword, cat, "Claimed", sort));

                case "Unresolved Lost" ->
                        allItems.addAll(lostDAO.filterArchived(keyword, cat, "Unresolved", sort));

                case "Unclaimed Found" ->
                        allItems.addAll(foundDAO.filterArchived(keyword, cat, "Unclaimed", sort));

                default -> {
                    // "All Types" — show everything archived
                    allItems.addAll(lostDAO.getAllArchived());
                    allItems.addAll(foundDAO.getAllArchived());
                }
            }
        }

        currentPage = 0;
        renderPage();
    }

    private void renderPage() {

        itemGrid.getChildren().clear();

        // Empty state
        if (allItems.isEmpty()) {
            Label noResults = new Label("No results found.");
            noResults.setStyle(
                    "-fx-font-size: 16px; " +
                            "-fx-text-fill: #710912; " +
                            "-fx-padding: 40;"
            );
            itemGrid.add(noResults, 0, 0, 4, 1);
            pageLabel.setText("Page 0 of 0");
            prevPageBtn.setDisable(true);
            nextPageBtn.setDisable(true);
            return;
        }

        int totalPages = Math.max(1,
                (int) Math.ceil((double) allItems.size() / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allItems.size());

        List<Object> pageItems = allItems.subList(from, to);

        int col = 0, row = 0;

        for (Object item : pageItems) {
            try {
                Parent card;

                if (showingArchive) {
                    // ── Archive card ──────────────────────────────
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/view/ArchiveItemCard.fxml"));
                    card = loader.load();

                    ArchiveItemCardController cardCtrl = loader.getController();

                    if (item instanceof LostItem lost) {
                        cardCtrl.setLostItem(lost);
                        card.setOnMouseClicked(e -> openItemDialog(lost));
                    } else if (item instanceof FoundItem found) {
                        cardCtrl.setFoundItem(found);
                        card.setOnMouseClicked(e -> openItemDialog(found));
                    }

                } else {
                    // ── Dashboard card ────────────────────────────
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/view/AdminItemCard.fxml"));
                    card = loader.load();

                    AdminItemCardController cardCtrl = loader.getController();

                    if (item instanceof LostItem lost) {
                        String date = lost.getDateLost() != null
                                ? lost.getDateLost().format(UI_DATE) : "";
                        cardCtrl.setItem(lost.getItemName(), date,
                                lost.getImagePath(), lost.getItemStatus(), "Lost", lost.getCategory());
                        card.setOnMouseClicked(e -> openItemDialog(lost));

                    } else if (item instanceof FoundItem found) {
                        String date = found.getDateFound() != null
                                ? found.getDateFound().format(UI_DATE) : "";
                        cardCtrl.setItem(found.getItemName(), date,
                                found.getImagePath(), found.getItemStatus(), "Found", found.getCategory());
                        card.setOnMouseClicked(e -> openItemDialog(found));
                    }
                }

                itemGrid.add(card, col, row);
                col++;
                if (col >= 5) { col = 0; row++; }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= totalPages - 1);
    }
    // =========================================================
    // Pagination button handlers
    // =========================================================
    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) allItems.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderPage();
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    // =========================================================
    // POST LOST / POST FOUND
    // =========================================================
    @FXML
    private void handlePostLost() {
        openPostDialog("Lost", null);
    }

    @FXML
    private void handlePostFound() {
        openPostDialog("Found", null);
    }

    // =========================================================
    // ARCHIVED VIEW — password required to enter
    // =========================================================

    @FXML
    private void handleArchive() {

        // ── GUARD: only ask password when entering archive ────────
        if (!showingArchive) {
            if (!PasswordGuard.verify(
                    dashboardTitleLabel.getScene().getWindow(),
                    "Access Archive",
                    "Enter admin password to view archived items:")) return;
        }

        showingArchive = !showingArchive;

        if (showingArchive) {
            // ── Entering archive view ─────────────────────────────
            dashboardTitleLabel.setText("Archived Items");
            totalLostLabelText.setText("ARCHIVE LOST RECORDS");
            totalFoundLabelText.setText("ARCHIVE FOUND RECORDS");

            try {
                totalLostLabel.setText(String.valueOf(lostDAO.getAllArchived().size()));
                totalFoundLabel.setText(String.valueOf(foundDAO.getAllArchived().size()));
            } catch (DBConnection.NoConnectionException e) {
                // reset and bail out
                showingArchive = false;
                dashboardTitleLabel.setText("Welcome to the Dashboard!");
                totalLostLabelText.setText("TOTAL LOST ITEMS");
                totalFoundLabelText.setText("TOTAL FOUND ITEMS");
                restoreDashboardButtons();
                PasswordManager.showAlert("No Internet",
                        "Please connect to the Internet and try again.");
                return;
            }

            // Swap buttons
            normalPostLost.setVisible(false);   normalPostLost.setManaged(false);
            normalPostFound.setVisible(false);  normalPostFound.setManaged(false);
            normalArchive.setVisible(false);    normalArchive.setManaged(false);
            archiveBackToMain.setVisible(true); archiveBackToMain.setManaged(true);
            archiveRecentlyDeleted.setVisible(true); archiveRecentlyDeleted.setManaged(true);

            // Swap typeCombo to archive options
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll(
                    "All Types",
                    "All Lost", "All Found",
                    "Resolved Lost", "Claimed Found",
                    "Unresolved Lost", "Unclaimed Found"
            );
            typeCombo.setValue("All Types");

        } else {
            // ── Returning to dashboard ────────────────────────────
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            totalLostLabelText.setText("TOTAL LOST ITEMS");
            totalFoundLabelText.setText("TOTAL FOUND ITEMS");

            restoreDashboardButtons();

            // Restore typeCombo to dashboard options
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");

            // ADD THIS: Apply compact style for main dashboard
            typeCombo.setStyle("-fx-padding: 0; -fx-cell-size: 25px;");
            typeCombo.setPadding(new Insets(0));

            loadDashboard();

            // Force refresh of style after loading
            javafx.application.Platform.runLater(() -> {
                typeCombo.setStyle("-fx-padding: 0; -fx-cell-size: 25px;");
                typeCombo.setPadding(new javafx.geometry.Insets(0));
            });

            return; // loadDashboard() calls applyFilters() internally
        }

        // Apply filters (archive side only reaches here)
        try {
            applyFilters();
        } catch (DBConnection.NoConnectionException e) {
            showingArchive = false;
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            totalLostLabelText.setText("TOTAL LOST ITEMS");
            totalFoundLabelText.setText("TOTAL FOUND ITEMS");
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");
            restoreDashboardButtons();
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");
        } catch (Exception e) {
            showingArchive = false;
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            totalLostLabelText.setText("TOTAL LOST ITEMS");
            totalFoundLabelText.setText("TOTAL FOUND ITEMS");
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");
            restoreDashboardButtons();
            PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            e.printStackTrace();
        }
    }

    // ── Helper to avoid repeating button visibility resets ────────
    private void restoreDashboardButtons() {
        normalPostLost.setVisible(true);    normalPostLost.setManaged(true);
        normalPostFound.setVisible(true);   normalPostFound.setManaged(true);
        normalArchive.setVisible(true);     normalArchive.setManaged(true);
        archiveBackToMain.setVisible(false); archiveBackToMain.setManaged(false);
        archiveRecentlyDeleted.setVisible(false); archiveRecentlyDeleted.setManaged(false);
    }
  
    @FXML
    private void handleBackToMain() {
        if (showingArchive) {
            handleArchive();
        }
    }

    @FXML
    private void handleRecentlyDeleted() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/RecycleBin.fxml"));
            Parent root = loader.load();

            RecyclebinController ctrl = loader.getController();
            ctrl.load(lostDAO.getDeleted(), foundDAO.getDeleted());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Recently Deleted");
            stage.setScene(new Scene(root));
            stage.setWidth(820);
            stage.setHeight(560);
            stage.setResizable(false);
            stage.showAndWait();

            loadDashboard();

        } catch (DBConnection.NoConnectionException e) {
            PasswordManager.showAlert("No Internet",
                    "Cannot load Recycle Bin. Please check your connection.");
        } catch (IOException e) {
            e.printStackTrace();
            PasswordManager.showAlert("Error", "Failed to open Recycle Bin.");
        } catch (Exception e) {
            e.printStackTrace();
            PasswordManager.showAlert("Error", "Something went wrong.");
        }
    }
    // =========================================================
    // RECYCLE BIN — password required
    // =========================================================
    @FXML
    private void handleRecycleBin() {

        if (!PasswordGuard.verify(
                dashboardTitleLabel.getScene().getWindow(),
                "Access Recycle Bin",
                "Enter admin password to access the Recycle Bin:")) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/RecycleBin.fxml"));
            Parent root = loader.load();

            RecyclebinController ctrl = loader.getController();

            ctrl.load(
                    lostDAO.getDeleted(),
                    foundDAO.getDeleted()
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Recycle Bin");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadDashboard();

        } catch (DBConnection.NoConnectionException e) {

            PasswordManager.showAlert(
                    "No Internet",
                    "Cannot load Recycle Bin. Please check your connection."
            );

        } catch (IOException e) {

            e.printStackTrace();
            PasswordManager.showAlert(
                    "Error",
                    "Failed to open Recycle Bin."
            );

        } catch (Exception e) {

            e.printStackTrace();
            PasswordManager.showAlert(
                    "Error",
                    "Something went wrong."
            );
        }
    }
    // =========================================================
    // SETTINGS / HELP
    // =========================================================
    @FXML
    private void handleSettings() {
        openDialog("/view/SettingsDialog.fxml", "Settings");
    }

    @FXML
    private void handleNeedHelp() {
        openDialog("/view/FAQDialog.fxml", "Help / FAQ");
    }

    // =========================================================
    // HELPERS — paste this over your existing openItemDialog()
    // =========================================================
    private void openItemDialog(Object item) {

        // Decide which view to open based on record status
        String recordStatus = "";
        if (item instanceof LostItem lost)   recordStatus = lost.getRecordStatus();
        if (item instanceof FoundItem found) recordStatus = found.getRecordStatus();

        String fxmlPath = "Archived".equals(recordStatus)
                ? "/view/PostItemViewArchive.fxml"
                : "/view/PostItemView.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if ("Archived".equals(recordStatus)) {
                PostItemViewArchiveController ctrl = loader.getController();
                ctrl.setItem(item, this);
            } else {
                PostItemViewController ctrl = loader.getController();
                ctrl.setItem(item, this);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadDashboard();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openPostDialog(String type, Object existingItem) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PostItemForm.fxml"));
            Parent root = loader.load();

            PostItemFormController ctrl = loader.getController();
            ctrl.setMode(type.equals("Lost") ? "new_lost" : "new_found", null, this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDialog(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
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
    // INFO DIALOG METHODS FOR QUICK LINKS
    // =========================================================
    private void showInfoDialog(String title, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/InfoDialog.fxml"));
            Parent root = loader.load();

            InfoDialogController controller = loader.getController();
            controller.setContent(title, content);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setWidth(600);
            stage.setHeight(550);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getAboutUsContent() {
        return "LOST & FOUND SYSTEM\n" +
                "This system is a desktop-based application developed as a final project for Object-Oriented Programming.\n\n" +
                "PURPOSE:\n" +
                "The Lost and Found Management System aims to centralize and optimize the reporting, tracking, and claiming of lost and found belongings within the campus environment.\n\n" +
                "KEY FEATURES:\n" +
                "▶ Item Reporting System - Structured forms with validation\n" +
                "▶ Centralized Viewing Dashboard - Displays all active non-archived items\n" +
                "▶ Search and Filtering Engine - Keyword search with filters\n" +
                "▶ Admin-Controlled Management - Password-protected environment\n" +
                "▶ Archive Management System - Soft-deletion for historical records\n" +
                "▶ Local Database Integration - Secure storage\n\n" +
                "DEVELOPED BY:\n" +
                "OOP Lost & Found Team (BSIT 2-1)\n\n" +
                "© 2026 Lost and Found System. All rights reserved.\n";
    }

    private String getTermsContent() {
        return "1. Acceptance of Terms\n" +
                "By using the Lost and Found Management System, you agree to comply with these Terms of Service.\n\n" +
                "2. System Usage\n" +
                "• The system is intended for reporting lost and found items within the campus only.\n" +
                "• Users must provide accurate and truthful information when submitting reports.\n" +
                "• False reporting or misuse of the system may result in restricted access.\n\n" +
                "3. Claim Process\n" +
                "• Claiming an item requires valid proof of ownership.\n" +
                "• Claimant information (name, ID, contact details) will be stored for record purposes.\n" +
                "• The administrator reserves the right to verify claims before approval.\n\n" +
                "4. Data Privacy\n" +
                "• Personal information collected is used solely for system operations.\n" +
                "• Data will not be shared with third parties.\n" +
                "• Archived records are retained indefinitely for institutional audit purposes.\n\n" +
                "5. Admin Authority\n" +
                "• Only authorized administrators may edit, archive, or mark items as claimed.\n" +
                "• The admin password is hashed and stored securely.\n\n" +
                "6. Limitation of Liability\n" +
                "• The system is provided 'as is' for educational purposes.\n" +
                "• The developers are not responsible for physical lost items.\n\n" +
                "7. Modifications\n" +
                "• These terms may be updated at any time without prior notice.\n\n" +
                "8. Contact\n" +
                "• For questions or concerns, please contact the system administrator.";
    }

    private String getPrivacyContent() {
        return "1. Information We Collect\n" +
                "The Lost and Found Management System collects the following information:\n\n" +
                "• Reporter Information: Name, contact number, email address (for item reports)\n" +
                "• Claimant Information: Name, Student/Staff ID, contact number, email address, proof of ownership\n" +
                "• Item Information: Item name, category, color, description, date lost/found, image\n" +
                "• System Data: Item ID, timestamps, status, record status, archive reason\n\n" +
                "2. How We Use Your Information\n" +
                "• To process and track lost and found item reports\n" +
                "• To verify and process claims\n" +
                "• To maintain historical archives for audit purposes\n" +
                "• To improve system functionality\n\n" +
                "3. Data Storage and Security\n" +
                "• All data is stored in a local database\n" +
                "• Admin passwords are hashed using industry-standard algorithms (bcrypt)\n" +
                "• Personal information is treated as sensitive data (NFR-19)\n" +
                "• No data is shared with third parties\n\n" +
                "4. Data Retention\n" +
                "• Active item records are retained until archived or deleted\n" +
                "• Archived records are kept indefinitely for historical reference (NFR-20)\n" +
                "• Claimant information is retained as part of the archived record\n\n" +
                "5. Your Rights\n" +
                "• You may request correction of inaccurate information by contacting the administrator\n" +
                "• You may request clarification about how your data is used\n\n" +
                "6. System Administrator Access\n" +
                "• Authorized administrators have access to all records for verification purposes\n" +
                "• Admin actions are protected by password authentication\n\n" +
                "7. Changes to This Policy\n" +
                "• This privacy policy may be updated periodically\n" +
                "• Continued use of the system constitutes acceptance of any changes\n\n" +
                "8. Contact Information\n" +
                "• For privacy concerns, please contact the system administrator.";
    }
}
