package controller;

import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Objects;

import java.io.IOException;
import java.sql.SQLException;
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
    @FXML private Label     totalPendingTextLabel;

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

    private Image sortImg;
    private Image categoryImg;
    private Image typeImg;

    // ── DAOs ─────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();

    // ── Pagination state ──────────────────────────────────────
    private static final int PAGE_SIZE = 25;
    private int currentPage = 0;

    // All items currently loaded (after filter/search applied)
    private List<Object> allItems = new ArrayList<>();

    // ── View mode ────────────────────────────────────────────
    private boolean showingArchive = false;
    private boolean showingDeleted = false;

    // ── Loading guard — prevents concurrent DB tasks ──────────
    private volatile boolean isLoading = false;

    // Date formatter
    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // =========================================================
    // PUBLIC REFRESH — called by child dialogs after save/edit
    // =========================================================
    public void refreshDashboard() {
        refreshStatsAsync(showingArchive);  // update the 3 counter labels
        applyFiltersAsync();                // reload the grid cards
    }

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {

        categoryCombo.getItems().addAll(
                "All Category", "Electronics", "Clothing", "Accessories",
                "Books", "ID/Documents", "Keys", "Bag", "Others"
        );
        categoryCombo.setValue("All Category");

        sortCombo.getItems().addAll("Newest", "Oldest", "Name A-Z", "Name Z-A");
        sortCombo.setValue("Newest");
        sortCombo.getStyleClass().add("sort-combo");
        typeCombo.getStyleClass().add("type-combo");

        typeCombo.getItems().addAll("All Types", "Lost", "Found");
        typeCombo.setValue("All Types");
        typeCombo.getStyleClass().add("compact-dropdown");
        typeCombo.setStyle("-fx-padding: 0; -fx-background-radius: 8;");
        typeCombo.setPadding(new Insets(0));
        typeCombo.setPrefWidth(130);

        // Load combo icons
        sortImg     = new Image(Objects.requireNonNull(
                AdminController.class.getResourceAsStream("/media/sortingarrow.png")));
        categoryImg = new Image(Objects.requireNonNull(
                AdminController.class.getResourceAsStream("/media/tagicon.png")));
        typeImg     = new Image(Objects.requireNonNull(
                AdminController.class.getResourceAsStream("/media/foldericon.png")));

        // Sort ComboBox icon
        sortCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                ImageView icon = new ImageView(sortImg);
                icon.setFitHeight(16); icon.setFitWidth(16);
                Label lbl = new Label(item == null || empty ? "  Sort by" : "  " + item);
                lbl.setGraphic(icon);
                lbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");
                setGraphic(lbl); setText(null);
            }
        });

        // Category ComboBox icon
        categoryCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                ImageView icon = new ImageView(categoryImg);
                icon.setFitHeight(16); icon.setFitWidth(16);
                Label lbl = new Label(item == null || empty ? "  Category" : "  " + item);
                lbl.setGraphic(icon);
                lbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");
                setGraphic(lbl); setText(null);
            }
        });

        // Type ComboBox icon
        typeCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                ImageView icon = new ImageView(typeImg);
                icon.setFitHeight(16); icon.setFitWidth(16);
                Label lbl = new Label(item == null || empty ? "  Type" : "  " + item);
                lbl.setGraphic(icon);
                lbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 14px;");
                setGraphic(lbl); setText(null);
            }
        });
        typeCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 14px; -fx-cell-size: 25px;");
                }
            }
        });

        sortCombo.setOnAction(e -> applyFiltersAsync());
        categoryCombo.setOnAction(e -> applyFiltersAsync());
        typeCombo.setOnAction(e -> applyFiltersAsync());

        // Initial load
        loadDashboardAsync();

        aboutUsLink.setOnMouseClicked(e -> showInfoDialog("About Us", getAboutUsContent()));
        termsLink.setOnMouseClicked(e -> showInfoDialog("Terms of Service", getTermsContent()));
        privacyLink.setOnMouseClicked(e -> showInfoDialog("Privacy Policy", getPrivacyContent()));
    }

    // =========================================================
    // ASYNC LOAD — initial dashboard load with stats
    // =========================================================
    private void loadDashboardAsync() {
        if (isLoading) return;
        isLoading = true;
        setGridBusy(true);

        Task<long[]> statsTask = new Task<>() {
            @Override
            protected long[] call() throws Exception {
                long lost    = lostDAO.countActive();
                long found   = foundDAO.countActive();
                return new long[]{ lost, found, lost + found };
            }
        };

        statsTask.setOnSucceeded(e -> {
            long[] counts = statsTask.getValue();
            totalLostLabel.setText(String.valueOf(counts[0]));
            totalFoundLabel.setText(String.valueOf(counts[1]));
            totalUnresolvedLabel.setText(String.valueOf(counts[2]));
            isLoading = false;
            applyFiltersAsync();   // chain: load items after stats
        });

        statsTask.setOnFailed(e -> {
            isLoading = false;
            setGridBusy(false);
            Throwable ex = statsTask.getException();
            if (ex instanceof DBConnection.NoConnectionException) {
                PasswordManager.showAlert("No Internet",
                        "Please connect to the Internet and try again.");
            } else {
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(statsTask);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================
    // ASYNC FILTER + RENDER — the hot path
    // =========================================================
    private void applyFiltersAsync() {
        // Capture UI-thread values before jumping to background
        final String category   = categoryCombo.getValue();
        final String type       = typeCombo.getValue();
        final String sortRaw    = sortCombo.getValue();
        final String keywordRaw = searchField.getText();
        final boolean archive   = showingArchive;

        final String keyword = (keywordRaw == null || keywordRaw.isBlank())
                ? null : keywordRaw.trim();
        final String cat  = (category == null || category.equals("All Category")) ? null : category;
        final String sort = switch (sortRaw == null ? "Newest" : sortRaw) {
            case "Oldest"   -> "oldest";
            case "Name A-Z" -> "name_asc";
            case "Name Z-A" -> "name_desc";
            default         -> "newest";
        };

        setGridBusy(true);

        Task<List<Object>> filterTask = new Task<>() {
            @Override
            protected List<Object> call() throws Exception {
                List<Object> items = new ArrayList<>();

                if (!archive) {
                    // ── Dashboard ─────────────────────────────────
                    if ("Lost".equals(type)) {
                        items.addAll(lostDAO.filter(keyword, cat, null, sort));
                    } else if ("Found".equals(type)) {
                        items.addAll(foundDAO.filter(keyword, cat, null, sort));
                    } else {
                        items.addAll(lostDAO.filter(keyword, cat, null, sort));
                        items.addAll(foundDAO.filter(keyword, cat, null, sort));
                        sortCombined(items, sort);
                    }
                } else {
                    // ── Archive ───────────────────────────────────
                    switch (type == null ? "All Types" : type) {
                        case "All Lost"       -> items.addAll(lostDAO.filterArchived(keyword, cat, null, sort));
                        case "All Found"      -> items.addAll(foundDAO.filterArchived(keyword, cat, null, sort));
                        case "Resolved Lost"  -> items.addAll(lostDAO.filterArchived(keyword, cat, "Found", sort));
                        case "Unresolved Lost"-> items.addAll(lostDAO.filterArchived(keyword, cat, "Unresolved", sort));
                        case "Claimed Found"  -> items.addAll(foundDAO.filterArchived(keyword, cat, "Claimed", sort));
                        case "Unclaimed Found"-> items.addAll(foundDAO.filterArchived(keyword, cat, "Unclaimed", sort));
                        default -> {
                            items.addAll(lostDAO.filterArchived(keyword, cat, null, sort));
                            items.addAll(foundDAO.filterArchived(keyword, cat, null, sort));
                            sortCombinedArchive(items, sort);
                        }
                    }
                }
                return items;
            }
        };

        filterTask.setOnSucceeded(e -> {
            allItems = filterTask.getValue();
            currentPage = 0;
            renderPage();        // still on FX thread, but no DB calls here
            setGridBusy(false);
        });

        filterTask.setOnFailed(e -> {
            setGridBusy(false);
            Throwable ex = filterTask.getException();
            if (ex instanceof DBConnection.NoConnectionException) {
                PasswordManager.showAlert("No Internet",
                        "Please connect to the Internet and try again.");
            } else {
                ex.printStackTrace();
                PasswordManager.showAlert("Error", "Something went wrong. Please try again.");
            }
        });

        Thread t = new Thread(filterTask);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================
    // ASYNC STATS ONLY — used after archive toggle
    // =========================================================
    private void refreshStatsAsync(boolean archive) {
        Task<long[]> task = new Task<>() {
            @Override
            protected long[] call() throws Exception {
                if (archive) {
                    long lost  = lostDAO.getAllArchived().size();
                    long found = foundDAO.getAllArchived().size();
                    return new long[]{ lost, found, lost + found };
                } else {
                    long lost  = lostDAO.countActive();
                    long found = foundDAO.countActive();
                    return new long[]{ lost, found, lost + found };
                }
            }
        };
        task.setOnSucceeded(e -> {
            long[] c = task.getValue();
            totalLostLabel.setText(String.valueOf(c[0]));
            totalFoundLabel.setText(String.valueOf(c[1]));
            totalUnresolvedLabel.setText(String.valueOf(c[2]));
        });
        task.setOnFailed(e -> task.getException().printStackTrace());
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================
    // RENDER PAGE — pure UI, no DB calls, fast
    // =========================================================
    private void renderPage() {

        itemGrid.getChildren().clear();

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

    // ── Show a loading indicator while the grid is fetching data ──
    private void setGridBusy(boolean busy) {
        itemGrid.setOpacity(busy ? 0.4 : 1.0);
        itemGrid.setMouseTransparent(busy);
        prevPageBtn.setDisable(busy);
        nextPageBtn.setDisable(busy);
    }

    // =========================================================
    // Pagination
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
        applyFiltersAsync();
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
    // ARCHIVE VIEW — password guard, then async load
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

        if (showingArchive) {
            // ── Entering archive view ──────────────────────────
            dashboardTitleLabel.setText("Archived Items");
            totalLostLabelText.setText("ARCHIVE LOST RECORDS");
            totalFoundLabelText.setText("ARCHIVE FOUND RECORDS");
            totalPendingTextLabel.setText("TOTAL ARCHIVE ITEMS");

            normalPostLost.setVisible(false);   normalPostLost.setManaged(false);
            normalPostFound.setVisible(false);  normalPostFound.setManaged(false);
            normalArchive.setVisible(false);    normalArchive.setManaged(false);
            archiveBackToMain.setVisible(true); archiveBackToMain.setManaged(true);
            archiveRecentlyDeleted.setVisible(true); archiveRecentlyDeleted.setManaged(true);

            typeCombo.getItems().clear();
            typeCombo.getItems().addAll(
                    "All Types",
                    "All Lost", "All Found",
                    "Resolved Lost", "Claimed Found",
                    "Unresolved Lost", "Unclaimed Found"
            );
            typeCombo.setValue("All Types");
            typeCombo.setPrefWidth(200);
            typeCombo.getStyleClass().remove("type-combo");

        } else {
            // ── Returning to dashboard ─────────────────────────
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
            totalLostLabelText.setText("TOTAL LOST ITEMS");
            totalFoundLabelText.setText("TOTAL FOUND ITEMS");
            totalPendingTextLabel.setText("TOTAL PENDING ITEMS");

            restoreDashboardButtons();

            typeCombo.getItems().clear();
            typeCombo.getItems().addAll("All Types", "Lost", "Found");
            typeCombo.setValue("All Types");
            typeCombo.setStyle("-fx-padding: 0; -fx-background-radius: 8;");
            typeCombo.setPadding(new Insets(0));
            typeCombo.setPrefWidth(130);
            typeCombo.getStyleClass().add("type-combo");
        }

        // Kick off async stats + items for whichever view we just entered
        refreshStatsAsync(showingArchive);
        applyFiltersAsync();
    }

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
        // Load recycle bin data async, then open dialog
        setGridBusy(true);
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                return new Object[]{ lostDAO.getDeleted(), foundDAO.getDeleted() };
            }
        };
        task.setOnSucceeded(e -> {
            setGridBusy(false);
            try {
                @SuppressWarnings("unchecked")
                java.util.List<LostItem>  deleted_l = (java.util.List<LostItem>)  task.getValue()[0];
                @SuppressWarnings("unchecked")
                java.util.List<FoundItem> deleted_f = (java.util.List<FoundItem>) task.getValue()[1];

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/RecycleBin.fxml"));
                Parent root = loader.load();
                RecyclebinController ctrl = loader.getController();
                ctrl.load(deleted_l, deleted_f);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Recently Deleted");
                stage.setScene(new Scene(root));
                stage.setWidth(820);
                stage.setHeight(560);
                stage.setResizable(false);
                stage.showAndWait();

                // Refresh after dialog closes
                refreshStatsAsync(showingArchive);
                applyFiltersAsync();

            } catch (IOException ex) {
                ex.printStackTrace();
                PasswordManager.showAlert("Error", "Failed to open Recycle Bin.");
            }
        });
        task.setOnFailed(e -> {
            setGridBusy(false);
            if (task.getException() instanceof DBConnection.NoConnectionException) {
                PasswordManager.showAlert("No Internet",
                        "Cannot load Recycle Bin. Please check your connection.");
            } else {
                task.getException().printStackTrace();
                PasswordManager.showAlert("Error", "Something went wrong.");
            }
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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

        setGridBusy(true);
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                return new Object[]{ lostDAO.getDeleted(), foundDAO.getDeleted() };
            }
        };
        task.setOnSucceeded(e -> {
            setGridBusy(false);
            try {
                @SuppressWarnings("unchecked")
                java.util.List<LostItem>  dl = (java.util.List<LostItem>)  task.getValue()[0];
                @SuppressWarnings("unchecked")
                java.util.List<FoundItem> df = (java.util.List<FoundItem>) task.getValue()[1];

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/view/RecycleBin.fxml"));
                Parent root = loader.load();
                RecyclebinController ctrl = loader.getController();
                ctrl.load(dl, df);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Recycle Bin");
                stage.setScene(new Scene(root));
                stage.showAndWait();

                refreshStatsAsync(showingArchive);
                applyFiltersAsync();

            } catch (IOException ex) {
                ex.printStackTrace();
                PasswordManager.showAlert("Error", "Failed to open Recycle Bin.");
            }
        });
        task.setOnFailed(e -> {
            setGridBusy(false);
            if (task.getException() instanceof DBConnection.NoConnectionException) {
                PasswordManager.showAlert("No Internet",
                        "Cannot load Recycle Bin. Please check your connection.");
            } else {
                task.getException().printStackTrace();
                PasswordManager.showAlert("Error", "Something went wrong.");
            }
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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
    // ITEM DIALOG — open detail view, refresh after close
    // =========================================================
    private void openItemDialog(Object item) {
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
            stage.setResizable(false);
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
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Dashboard already refreshed inside PostItemFormController.handleResult()
            // via adminController.refreshDashboard() — no double-refresh needed

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
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // SORT HELPERS (background thread)
    // =========================================================
    private void sortCombined(List<Object> items, String sort) {
        if ("name_asc".equals(sort) || "name_desc".equals(sort)) {
            items.sort((a, b) -> {
                String na = getName(a).toLowerCase();
                String nb = getName(b).toLowerCase();
                return "name_desc".equals(sort) ? nb.compareTo(na) : na.compareTo(nb);
            });
        } else {
            items.sort((a, b) -> {
                java.time.LocalDateTime ca = getCreatedAt(a);
                java.time.LocalDateTime cb = getCreatedAt(b);
                if (ca == null && cb == null) return 0;
                if (ca == null) return 1;
                if (cb == null) return -1;
                return "oldest".equals(sort) ? ca.compareTo(cb) : cb.compareTo(ca);
            });
        }
    }

    private void sortCombinedArchive(List<Object> items, String sort) {
        if ("name_asc".equals(sort) || "name_desc".equals(sort)) {
            items.sort((a, b) -> {
                String na = getName(a).toLowerCase();
                String nb = getName(b).toLowerCase();
                return "name_desc".equals(sort) ? nb.compareTo(na) : na.compareTo(nb);
            });
        } else {
            items.sort((a, b) -> {
                java.time.LocalDateTime aa = getArchivedAt(a);
                java.time.LocalDateTime ab = getArchivedAt(b);
                if (aa == null && ab == null) return 0;
                if (aa == null) return 1;
                if (ab == null) return -1;
                return "oldest".equals(sort) ? aa.compareTo(ab) : ab.compareTo(aa);
            });
        }
    }

    private String getName(Object o) {
        if (o instanceof LostItem  l) return l.getItemName();
        if (o instanceof FoundItem f) return f.getItemName();
        return "";
    }
    private java.time.LocalDateTime getCreatedAt(Object o) {
        if (o instanceof LostItem  l) return l.getCreatedAt();
        if (o instanceof FoundItem f) return f.getCreatedAt();
        return null;
    }
    private java.time.LocalDateTime getArchivedAt(Object o) {
        if (o instanceof LostItem  l) return l.getArchivedAt();
        if (o instanceof FoundItem f) return f.getArchivedAt();
        return null;
    }

    // =========================================================
    // INFO DIALOG CONTENT
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
            stage.setScene(new Scene(root));
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
                "▶ Cloud Database Integration - Powered by Supabase for secure, reliable storage\n" +
                "▶ Intelligent Item Matching - Automatically suggests potential matches between lost and found reports based on item name, category, color, and date\n\n" +
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
                "• All data is stored in a cloud database\n" +
                "• Admin passwords are hashed using industry-standard algorithms (bcrypt)\n" +
                "• Personal information is treated as sensitive data\n" +
                "• No data is shared with third parties\n\n" +
                "4. Data Retention\n" +
                "• Active item records are retained until archived or deleted\n" +
                "• Archived records are kept indefinitely for historical reference\n" +
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
