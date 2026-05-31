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
    // ARCHIVE toggle
    // =========================================================
    @FXML
    private void handleArchive() {
        showingArchive = !showingArchive;
        dashboardTitleLabel.setText(showingArchive ? "Archived Items" : "Welcome to the Dashboard!");
        applyFilters();
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
}