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
import model.FoundItem;
import model.LostItem;
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

        List<LostItem>  allLost  = lostDAO.getAllActive();
        List<FoundItem> allFound = foundDAO.getAllActive();

        totalLostLabel.setText(String.valueOf(allLost.size()));
        totalFoundLabel.setText(String.valueOf(allFound.size()));

        long unresolved = allLost.stream().filter(i -> "Unresolved".equals(i.getItemStatus())).count()
                + allFound.stream().filter(i -> "Unclaimed".equals(i.getItemStatus())).count();
        totalUnresolvedLabel.setText(String.valueOf(unresolved));

        applyFilters();
    }

    private void applyFilters() {

        String category = categoryCombo.getValue();
        String type   = typeCombo.getValue();
        String sortRaw  = sortCombo.getValue();

        // FIX 1: single keyword declaration (null-safe)
        String keywordRaw = searchField.getText();
        String keyword = (keywordRaw == null || keywordRaw.isBlank())
                ? null
                : keywordRaw.trim();

        String cat  = (category == null || category.equals("All Category")) ? null : category;
        String sort = switch (sortRaw == null ? "Newest" : sortRaw) {
            case "Oldest"    -> "oldest";
            case "Name A-Z"  -> "name_asc";
            case "Name Z-A"  -> "name_desc";
            default          -> "newest";
        };

        allItems = new ArrayList<>();

        if (!showingArchive) {
            if ("Lost".equals(type)) {
                allItems.addAll(lostDAO.filter(keyword, cat, null, sort));
            } else if ("Found".equals(type)) {
                allItems.addAll(foundDAO.filter(keyword, cat, null, sort));
            } else {
                // All — query both
                allItems.addAll(lostDAO.filter(keyword, cat, null, sort));
                allItems.addAll(foundDAO.filter(keyword, cat, null, sort));
            }
        } else {
            allItems.addAll(lostDAO.getAllArchived());
            allItems.addAll(foundDAO.getAllArchived());
        }

        currentPage = 0;
        renderPage();
    }

    private void renderPage() {

        itemGrid.getChildren().clear();

        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / PAGE_SIZE));
        currentPage    = Math.max(0, Math.min(currentPage, totalPages - 1));

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allItems.size());

        List<Object> pageItems = allItems.subList(from, to);

        int col = 0, row = 0;
        for (Object item : pageItems) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/AdminItemCard.fxml"));
                Parent card = loader.load();

                AdminItemCardController cardCtrl = loader.getController();

                if (item instanceof LostItem lost) {
                    String date = lost.getDateLost() != null
                            ? lost.getDateLost().format(UI_DATE)
                            : "";

                    cardCtrl.setItem(
                            lost.getItemName(),
                            date,
                            lost.getImagePath(),
                            lost.getItemStatus(),
                            "Lost"
                    );

                    card.setOnMouseClicked(e -> openItemDialog(lost));

                } else if (item instanceof FoundItem found) {
                    String date = found.getDateFound() != null
                            ? found.getDateFound().format(UI_DATE)
                            : "";

                    cardCtrl.setItem(
                            found.getItemName(),
                            date,
                            found.getImagePath(),
                            found.getItemStatus(),
                            "Found"
                    );

                    card.setOnMouseClicked(e -> openItemDialog(found));
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
    // ARCHIVE toggle
    // =========================================================
    @FXML
    private void handleArchive() {
        showingArchive = !showingArchive;

        if (showingArchive) {
            dashboardTitleLabel.setText("Archived Items");
            totalLostLabelText.setText("ARCHIVE LOST RECORDS");
            totalFoundLabelText.setText("ARCHIVE FOUND RECORDS");
            totalLostLabel.setText(String.valueOf(lostDAO.getAllArchived().size()));
            totalFoundLabel.setText(String.valueOf(foundDAO.getAllArchived().size()));

            // I-hide ang normal na 3 buttons
            normalPostLost.setVisible(false);
            normalPostLost.setManaged(false);
            normalPostFound.setVisible(false);
            normalPostFound.setManaged(false);
            normalArchive.setVisible(false);
            normalArchive.setManaged(false);

            // I-show ang 2 buttons para sa archive
            archiveBackToMain.setVisible(true);
            archiveBackToMain.setManaged(true);
            archiveRecentlyDeleted.setVisible(true);
            archiveRecentlyDeleted.setManaged(true);

        } else {
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            totalLostLabelText.setText("TOTAL LOST ITEMS");
            totalFoundLabelText.setText("TOTAL FOUND ITEMS");

            // I-show ang normal na 3 buttons
            normalPostLost.setVisible(true);
            normalPostLost.setManaged(true);
            normalPostFound.setVisible(true);
            normalPostFound.setManaged(true);
            normalArchive.setVisible(true);
            normalArchive.setManaged(true);

            // I-hide ang 2 buttons para sa archive
            archiveBackToMain.setVisible(false);
            archiveBackToMain.setManaged(false);
            archiveRecentlyDeleted.setVisible(false);
            archiveRecentlyDeleted.setManaged(false);

            loadDashboard();
        }

        applyFilters();
    }

    // =========================================================
    // BACK TO MAIN (from archive view)
    // =========================================================
    @FXML
    private void handleBackToMain() {
        if (showingArchive) {
            handleArchive();
        }
    }

    // =========================================================
    // RECENTLY DELETED (from archive view)
    // =========================================================
    @FXML
    private void handleRecentlyDeleted() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RecycleBin.fxml"));
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
        } catch (IOException e) {
            e.printStackTrace();
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

    @FXML
    private void handleRecycleBin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/RecycleBinDialog.fxml"));
            Parent root = loader.load();

            RecyclebinController ctrl = loader.getController();
            ctrl.load(lostDAO.getDeleted(), foundDAO.getDeleted());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Recycle Bin");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadDashboard();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void openItemDialog(Object item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PostItemView.fxml"));
            Parent root = loader.load();

            PostItemViewController ctrl = loader.getController();
            ctrl.setItem(item, this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
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