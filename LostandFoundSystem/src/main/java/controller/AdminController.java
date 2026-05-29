package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private GridPane itemGrid;
    @FXML private Label totalLostLabel;
    @FXML private Label totalFoundLabel;
    @FXML private Label totalUnresolvedLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> sortCombo;

    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label pageLabel;
    @FXML private HBox pageButtonsContainer;
    @FXML private Button archiveButton;
    @FXML private Label dashboardTitleLabel;

    // Data storage
    private List<ItemData> allItems = new ArrayList<>();
    private List<ItemData> archivedItems = new ArrayList<>();
    private List<ItemData> filteredItems = new ArrayList<>();
    private List<String[][]> allPages = new ArrayList<>();
    private int currentPage = 0;
    private int itemsPerPage = 25;
    private int totalPages = 0;
    private boolean isArchiveView = false;

    @FXML
    public void initialize() {
        sortCombo.getItems().addAll("Newest First", "Oldest First");
        sortCombo.setValue("Newest First");

        categoryCombo.getItems().addAll("All Categories", "Electronics", "Clothing", "Documents", "Jewelry", "Other");
        categoryCombo.setValue("All Categories");

        updateFilterCombo();
        filterCombo.setValue("All Status");

        loadSampleData();
        refreshDisplay();

        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
            setupPagination();
        });

        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
            setupPagination();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
            setupPagination();
        });

        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
            setupPagination();
        });
    }

    private void updateFilterCombo() {
        filterCombo.getItems().clear();
        if (isArchiveView) {
            filterCombo.getItems().addAll("All Status", "Lost - Unresolved", "Found - Unresolved", "Claimed");
        } else {
            filterCombo.getItems().addAll("All Status", "Lost", "Found");
        }
        filterCombo.setValue("All Status");
    }

    private void refreshDisplay() {
        updateStatistics();
        applyFilters();
        setupPagination();
        updateArchiveButtonUI();
    }

    private void updateArchiveButtonUI() {
        if (archiveButton != null) {
            if (isArchiveView) {
                archiveButton.setText("← BACK TO MAIN");
                archiveButton.setStyle("-fx-background-color: #710912; -fx-cursor: hand; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                archiveButton.setText("ARCHIVE");
                archiveButton.setStyle("-fx-background-color: #154230; -fx-cursor: hand; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            }
        }

        if (dashboardTitleLabel != null) {
            dashboardTitleLabel.setText("Welcome to the Dashboard!");
        }
    }

    private void loadSampleData() {
        allItems = new ArrayList<>();
        archivedItems = new ArrayList<>();

        // Active items
        allItems.add(new ItemData("Laptop", "Reported: 2026-01-15", "💻", "Electronics", "Lost", "John Doe", "johndoe@email.com", "09123456789", "Silver laptop with stickers", "Silver", null));
        allItems.add(new ItemData("Wallet", "Reported: 2026-01-14", "👛", "Clothing", "Lost", "Jane Smith", "janesmith@email.com", "09876543210", "Brown leather wallet", "Brown", null));
        allItems.add(new ItemData("Phone", "Reported: 2026-01-13", "📱", "Electronics", "Found", "Mike Johnson", "mikej@email.com", "09123456780", "iPhone 13 black", "Black", null));
        allItems.add(new ItemData("Keys", "Reported: 2026-01-12", "🔑", "Other", "Lost", "Sarah Lee", "sarahlee@email.com", "09234567890", "Keychain with car key", "Silver", null));
        allItems.add(new ItemData("Backpack", "Reported: 2026-01-11", "🎒", "Clothing", "Found", "Tom Brown", "tombrown@email.com", "09345678901", "Black North Face backpack", "Black", null));
        allItems.add(new ItemData("Glasses", "Reported: 2026-01-10", "👓", "Other", "Lost", "Lisa Wong", "lisawong@email.com", "09456789012", "Ray-Ban sunglasses", "Black", null));
        allItems.add(new ItemData("Watch", "Reported: 2026-01-09", "⌚", "Jewelry", "Found", "Chris Lee", "chrislee@email.com", "09567890123", "Silver Casio watch", "Silver", null));
        allItems.add(new ItemData("Charger", "Reported: 2026-01-08", "🔌", "Electronics", "Lost", "Alex Kim", "alexkim@email.com", "09678901234", "USB-C charger", "White", null));
        allItems.add(new ItemData("Mouse", "Reported: 2026-01-16", "🖱️", "Electronics", "Found", "Emily Chen", "emilychen@email.com", "09789012345", "Wireless Logitech mouse", "Black", null));
        allItems.add(new ItemData("Notebook", "Reported: 2026-01-07", "📓", "Documents", "Lost", "Kevin Garcia", "keving@email.com", "09890123456", "Red spiral notebook", "Red", null));
        allItems.add(new ItemData("Headphones", "Reported: 2026-01-17", "🎧", "Electronics", "Found", "Maria Santos", "maria@email.com", "09901234567", "Sony noise-cancelling", "Black", null));
        allItems.add(new ItemData("Water Bottle", "Reported: 2026-01-06", "💧", "Other", "Lost", "James Wilson", "jameswilson@email.com", "09456789012", "Blue Hydro Flask", "Blue", null));

        // Archived items - using new status format
        archivedItems.add(new ItemData("Umbrella", "Reported: 2026-01-05", "☔", "Other", "Claimed", "David Park", "davidpark@email.com", "09890123456", "Blue folding umbrella", "Blue", null));
        archivedItems.add(new ItemData("Bag", "Reported: 2026-01-02", "👜", "Clothing", "Lost - Unresolved", "Daniel Kim", "danielkim@email.com", "09234567890", "Brown leather handbag", "Brown", null));
        archivedItems.add(new ItemData("Sunglasses", "Reported: 2025-12-31", "🕶️", "Jewelry", "Found - Unresolved", "Michelle Lee", "michelle@email.com", "09345678901", "Ray-Ban aviator", "Gold", null));
    }

    private void updateStatistics() {
        long lostCount = allItems.stream()
                .filter(i -> i.getStatus().equals("Lost"))
                .count();

        long foundCount = allItems.stream()
                .filter(i -> i.getStatus().equals("Found"))
                .count();

        long archivedCount = archivedItems.size();

        totalLostLabel.setText(String.valueOf(lostCount));
        totalFoundLabel.setText(String.valueOf(foundCount));
        totalUnresolvedLabel.setText(String.valueOf(archivedCount));
    }

    private void applyFilters() {
        String selectedCategory = categoryCombo.getValue();
        String selectedStatus = filterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();

        List<ItemData> sourceList = isArchiveView ? archivedItems : allItems;

        filteredItems = sourceList.stream()
                .filter(item -> {
                    if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                        if (!item.getCategory().equals(selectedCategory)) return false;
                    }
                    if (selectedStatus != null && !selectedStatus.equals("All Status")) {
                        if (!item.getStatus().equals(selectedStatus)) return false;
                    }
                    if (searchText != null && !searchText.isEmpty()) {
                        return item.getName().toLowerCase().contains(searchText) ||
                                item.getDescription().toLowerCase().contains(searchText) ||
                                (item.getColor() != null && item.getColor().toLowerCase().contains(searchText));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        applySorting();
    }

    private void applySorting() {
        String sortOrder = sortCombo.getValue();

        if (sortOrder == null || sortOrder.equals("Newest First")) {
            filteredItems.sort((a, b) -> {
                String dateA = extractDateFromString(a.getDate());
                String dateB = extractDateFromString(b.getDate());
                return dateB.compareTo(dateA);
            });
        } else if (sortOrder.equals("Oldest First")) {
            filteredItems.sort((a, b) -> {
                String dateA = extractDateFromString(a.getDate());
                String dateB = extractDateFromString(b.getDate());
                return dateA.compareTo(dateB);
            });
        }
    }

    private String extractDateFromString(String dateString) {
        if (dateString == null) return "";
        if (dateString.contains("Reported:")) {
            return dateString.replace("Reported:", "").trim();
        }
        return dateString;
    }

    private void setupPagination() {
        allPages.clear();

        for (int i = 0; i < filteredItems.size(); i += itemsPerPage) {
            int end = Math.min(i + itemsPerPage, filteredItems.size());
            String[][] page = new String[end - i][4];
            for (int j = i; j < end; j++) {
                ItemData item = filteredItems.get(j);
                page[j - i][0] = item.getName();
                String displayDate = formatDateForDisplay(item.getDate(), item.getStatus());
                page[j - i][1] = displayDate;
                page[j - i][2] = item.getIcon();
                page[j - i][3] = item.getImageBase64() != null ? item.getImageBase64() : "";
            }
            allPages.add(page);
        }

        totalPages = allPages.size();
        currentPage = 0;

        loadItemsFromDatabase();
        updatePageDisplay();
        createPageButtons();
    }

    private String formatDateForDisplay(String dateStr, String status) {
        if (dateStr == null) return "No date";
        if (dateStr.startsWith("Reported:")) {
            String date = dateStr.replace("Reported:", "").trim();
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date parsedDate = inputFormat.parse(date);
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy");
                if (status.equals("Lost") || status.equals("Found")) {
                    return (status.equals("Lost") ? "Lost on: " : "Found on: ") + outputFormat.format(parsedDate);
                } else {
                    return "Reported: " + outputFormat.format(parsedDate);
                }
            } catch (Exception e) {
                return date;
            }
        }
        return dateStr;
    }

    private void createPageButtons() {
        pageButtonsContainer.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            final int pageNum = i;
            Button pageBtn = new Button(String.valueOf(i + 1));
            pageBtn.setStyle("-fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
            pageBtn.setOnAction(e -> goToPage(pageNum));
            pageButtonsContainer.getChildren().add(pageBtn);
        }
        updatePageButtonStyles();
    }

    private void goToPage(int page) {
        if (page >= 0 && page < totalPages) {
            currentPage = page;
            loadItemsFromDatabase();
            updatePageDisplay();
            updatePageButtonStyles();
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadItemsFromDatabase();
            updatePageDisplay();
            updatePageButtonStyles();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadItemsFromDatabase();
            updatePageDisplay();
            updatePageButtonStyles();
        }
    }

    private void updatePageDisplay() {
        if (totalPages > 0) {
            pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
            prevPageBtn.setDisable(currentPage == 0);
            nextPageBtn.setDisable(currentPage == totalPages - 1);
        } else {
            pageLabel.setText("Page 0 of 0");
            prevPageBtn.setDisable(true);
            nextPageBtn.setDisable(true);
        }
    }

    private void updatePageButtonStyles() {
        for (int i = 0; i < pageButtonsContainer.getChildren().size(); i++) {
            Button btn = (Button) pageButtonsContainer.getChildren().get(i);
            if (i == currentPage) {
                btn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
            } else {
                btn.setStyle("-fx-background-color: rgba(113, 9, 18, 0.4); -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
            }
        }
    }

    private void loadItemsFromDatabase() {
        itemGrid.getChildren().clear();

        if (allPages.isEmpty() || currentPage >= allPages.size()) {
            return;
        }

        String[][] currentPageItems = allPages.get(currentPage);

        for (int i = 0; i < currentPageItems.length; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminItemCard.fxml"));
                VBox card = loader.load();
                AdminItemCardController controller = loader.getController();

                String imageBase64 = currentPageItems[i].length > 3 ? currentPageItems[i][3] : null;
                controller.setItemData(currentPageItems[i][0], currentPageItems[i][1], currentPageItems[i][2], imageBase64);

                int row = i / 5;
                int col = i % 5;
                itemGrid.add(card, col, row);

                final int index = i;
                final String itemName = currentPageItems[index][0];
                card.setOnMouseClicked(event -> {
                    ItemData clickedItem = filteredItems.stream()
                            .filter(item -> item.getName().equals(itemName))
                            .findFirst()
                            .orElse(null);
                    if (clickedItem != null) {
                        viewItemDetails(clickedItem);
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void viewItemDetails(ItemData item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PostItemDialog.fxml"));
            VBox root = loader.load();

            PostItemController controller = loader.getController();

            PostItemController.ArchiveCallback callback = new PostItemController.ArchiveCallback() {
                @Override
                public void moveItemToArchive(ItemData item, String reason) {
                    allItems.removeIf(i -> i.getName().equals(item.getName()));
                    archivedItems.add(item);
                    System.out.println("Item moved to archive: " + item.getName() + " - Reason: " + reason);
                    refreshDisplay();
                }

                @Override
                public void restoreItemFromArchive(ItemData item) {
                    archivedItems.removeIf(i -> i.getName().equals(item.getName()));
                    allItems.add(item);
                    refreshDisplay();
                }

                @Override
                public void updateItem(ItemData item) {
                    if (isArchiveView) {
                        int index = findItemIndex(archivedItems, item.getName());
                        if (index != -1) archivedItems.set(index, item);
                    } else {
                        int index = findItemIndex(allItems, item.getName());
                        if (index != -1) allItems.set(index, item);
                    }
                    refreshDisplay();
                }

                @Override
                public void addNewItem(ItemData item) {
                    allItems.add(item);
                    refreshDisplay();
                }
            };

            controller.setArchiveCallback(callback);
            controller.setExistingItemMode(true, item.getStatus(), isArchiveView);
            controller.setItemDataForEditing(
                    item.getName(),
                    item.getCategory(),
                    item.getColor(),
                    item.getStatus(),
                    item.getReporterName(),
                    item.getContactNumber(),
                    item.getEmail(),
                    item.getDescription(),
                    item.getImageBase64()
            );

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshDisplay();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findItemIndex(List<ItemData> list, String itemName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(itemName)) {
                return i;
            }
        }
        return -1;
    }

    @FXML
    private void handlePostLost() {
        showNewItemPopup(true);
    }

    @FXML
    private void handlePostFound() {
        showNewItemPopup(false);
    }

    private void showNewItemPopup(boolean isLost) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PostItemDialog.fxml"));
            VBox root = loader.load();

            PostItemController controller = loader.getController();

            PostItemController.ArchiveCallback callback = new PostItemController.ArchiveCallback() {
                @Override
                public void moveItemToArchive(ItemData item, String reason) {
                    allItems.removeIf(i -> i.getName().equals(item.getName()));
                    archivedItems.add(item);
                    refreshDisplay();
                }

                @Override
                public void restoreItemFromArchive(ItemData item) {
                    archivedItems.removeIf(i -> i.getName().equals(item.getName()));
                    allItems.add(item);
                    refreshDisplay();
                }

                @Override
                public void updateItem(ItemData item) {
                    int index = findItemIndex(allItems, item.getName());
                    if (index != -1) allItems.set(index, item);
                    refreshDisplay();
                }

                @Override
                public void addNewItem(ItemData item) {
                    allItems.add(item);
                    refreshDisplay();
                }
            };

            controller.setArchiveCallback(callback);
            controller.setLostMode(isLost);
            controller.setExistingItemMode(false, isLost ? "Lost" : "Found", false);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshDisplay();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not open dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleArchive() {
        if (isArchiveView) {
            isArchiveView = false;
            updateFilterCombo();
            refreshDisplay();
            return;
        }

        Dialog<ButtonType> authDialog = new Dialog<>();
        authDialog.setTitle("ADMIN AUTHENTICATION REQUIRED");
        authDialog.setHeaderText(null);

        VBox dialogVBox = new VBox(15);
        dialogVBox.setPadding(new Insets(20));
        dialogVBox.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("🔒 ARCHIVE ACCESS");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #710912; -fx-font-size: 18px;");

        Label infoLabel = new Label("The archive contains claimed and unresolved items.\nEnter admin password to continue.");
        infoLabel.setStyle("-fx-text-fill: #5b5b5b; -fx-font-size: 13px;");
        infoLabel.setWrapText(true);

        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle("-fx-font-weight: bold;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        passwordField.setPrefWidth(200);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        dialogVBox.getChildren().addAll(titleLabel, infoLabel, passwordBox);

        authDialog.getDialogPane().setContent(dialogVBox);
        authDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        authDialog.getDialogPane().setStyle("-fx-background-color: white;");

        Button okBtn = (Button) authDialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 20 8 20;");

        Button cancelBtn = (Button) authDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color: #A5A5A5; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20 8 20;");

        authDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String enteredPassword = passwordField.getText();
                if (PasswordManager.verifyPassword(enteredPassword)) {
                    isArchiveView = true;
                    updateFilterCombo();
                    refreshDisplay();
                    PasswordManager.showAlert("Archive Mode", "Showing archived items (Claimed and Unresolved items).\nClick 'BACK TO MAIN' to return.");
                } else {
                    PasswordManager.showAlert("Access Denied", "Incorrect admin password.\nYou cannot access the archive.");
                    isArchiveView = false;
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        refreshDisplay();
    }

    @FXML
    private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SettingsDialog.fxml"));
            VBox root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not open settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleNeedHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FAQDialog.fxml"));
            VBox root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Help & FAQ");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not open FAQ: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        alert.showAndWait();
    }

    private void showAlert(String message) {
        showAlert("Information", message);
    }

    public static class ItemData {
        private String name;
        private String date;
        private String icon;
        private String category;
        private String status;
        private String reporterName;
        private String email;
        private String contactNumber;
        private String description;
        private String color;
        private String imageBase64;

        public ItemData(String name, String date, String icon, String category, String status,
                        String reporterName, String email, String contactNumber, String description,
                        String color, String imageBase64) {
            this.name = name;
            this.date = date;
            this.icon = icon;
            this.category = category;
            this.status = status;
            this.reporterName = reporterName;
            this.email = email;
            this.contactNumber = contactNumber;
            this.description = description;
            this.color = color;
            this.imageBase64 = imageBase64;
        }

        public String getName() { return name; }
        public String getDate() { return date; }
        public String getIcon() { return icon; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public String getReporterName() { return reporterName; }
        public String getEmail() { return email; }
        public String getContactNumber() { return contactNumber; }
        public String getDescription() { return description; }
        public String getColor() { return color; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public void setStatus(String status) { this.status = status; }
    }
}