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
import javafx.stage.Modality;
import javafx.stage.Stage;
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

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> typeCombo;

    @FXML private GridPane  itemGrid;
    @FXML private Button    prevPageBtn;
    @FXML private Button    nextPageBtn;
    @FXML private Label     pageLabel;

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
                                lost.getImagePath(), lost.getItemStatus(), "Lost");
                        card.setOnMouseClicked(e -> openItemDialog(lost));

                    } else if (item instanceof FoundItem found) {
                        String date = found.getDateFound() != null
                                ? found.getDateFound().format(UI_DATE) : "";
                        cardCtrl.setItem(found.getItemName(), date,
                                found.getImagePath(), found.getItemStatus(), "Found");
                        card.setOnMouseClicked(e -> openItemDialog(found));
                    }
                }

                itemGrid.add(card, col, row);
                col++;
                if (col >= 4) { col = 0; row++; }

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
        if (!showingArchive) {
            if (!PasswordGuard.verify(
                    dashboardTitleLabel.getScene().getWindow(),
                    "Access Archive",
                    "Enter admin password to view archived items:")) return;
        }

        showingArchive = !showingArchive;
        dashboardTitleLabel.setText(
                showingArchive ? "Archived Items" : "Welcome to the Dashboard!");

        // Swap typeCombo options based on view
        typeCombo.getItems().clear();
        if (showingArchive) {
            typeCombo.getItems().addAll(
                    "All Types",
                    "All Lost",
                    "All Found",
                    "Resolved Lost",
                    "Claimed Found",
                    "Unresolved Lost",
                    "Unclaimed Found"
            );
        } else {
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
        }
        typeCombo.setValue("All Types");

        try {
            applyFilters();
        } catch (DBConnection.NoConnectionException e) {
            showingArchive = !showingArchive;
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            // restore dashboard combo options on failure
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");
            PasswordManager.showAlert("No Internet",
                    "Please connect to the Internet and try again.");
        } catch (Exception e) {
            showingArchive = !showingArchive;
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");
            PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            e.printStackTrace();
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
}