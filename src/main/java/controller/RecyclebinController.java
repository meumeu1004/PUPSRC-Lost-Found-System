package controller;

import dao.AuditLogDAO;
import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.FoundItem;
import model.LostItem;
import util.DateUtil;
import database.DBConnection;
import controller.PasswordManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RecyclebinController {

    // ── FXML fields ──────────────────────────────────────────
    @FXML private TableView<RecycleBinRow>             recycleBinTable;
    @FXML private TableColumn<RecycleBinRow, String>   colId;
    @FXML private TableColumn<RecycleBinRow, String>   colType;
    @FXML private TableColumn<RecycleBinRow, String>   colName;
    @FXML private TableColumn<RecycleBinRow, String>   colCategory;
    @FXML private TableColumn<RecycleBinRow, String>   colDate;
    @FXML private TableColumn<RecycleBinRow, String>   colStatus;
    @FXML private TableColumn<RecycleBinRow, Void>     colAction;
    @FXML private Label                                countLabel;

    // ── DAOs ─────────────────────────────────────────────────
    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();
    private final AuditLogDAO  auditDAO = new AuditLogDAO();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ── Data ─────────────────────────────────────────────────
    private final ObservableList<RecycleBinRow> rows =
            FXCollections.observableArrayList();

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(data.getValue().getId())));

        colType.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getType()));

        colName.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getItemName()));

        colCategory.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategory()));

        colDate.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDateReported()));

        colStatus.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getItemStatus()));

        // Restore button per row
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Restore");
            {
                btn.setStyle(
                        "-fx-background-color: #16213E; " +
                                "-fx-text-fill: white; " +
                                "-fx-border-radius: 5; " +
                                "-fx-background-radius: 5; " +
                                "-fx-font-size: 11px;"
                );
                btn.setOnAction(e -> {
                    RecycleBinRow row = getTableView().getItems().get(getIndex());
                    handleRestore(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        recycleBinTable.setItems(rows);
    }

    // =========================================================
    // LOAD — called from AdminController after opening dialog
    // =========================================================
    public void load(List<LostItem> deletedLost, List<FoundItem> deletedFound) {
        rows.clear();

        for (LostItem item : deletedLost) {
            rows.add(new RecycleBinRow(
                    item.getId(),
                    "Lost",
                    item.getItemName(),
                    item.getCategory(),
                    DateUtil.format(item.getCreatedAt()),
                    item.getItemStatus()
            ));
        }

        for (FoundItem item : deletedFound) {
            rows.add(new RecycleBinRow(
                    item.getId(),
                    "Found",
                    item.getItemName(),
                    item.getCategory(),
                    DateUtil.format(item.getCreatedAt()),
                    item.getItemStatus()
            ));
        }

        countLabel.setText(rows.size() + " deleted item" +
                (rows.size() == 1 ? "" : "s"));
    }

    // =========================================================
    // RESTORE
    // =========================================================
    private void handleRestore(RecycleBinRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Item");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Restore \"" + row.getItemName() + "\" back to Active?");

        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            try {

                boolean ok;

                if ("Lost".equals(row.getType())) {
                    ok = lostDAO.restore(row.getId());

                    if (ok) auditDAO.insertLog(row.getId(), "Lost",
                            "Restored", "admin",
                            "{\"record_status\": \"Deleted\"}",
                            "{\"record_status\": \"Active\"}");

                } else {
                    ok = foundDAO.restore(row.getId());

                    if (ok) auditDAO.insertLog(row.getId(), "Found",
                            "Restored", "admin",
                            "{\"record_status\": \"Deleted\"}",
                            "{\"record_status\": \"Active\"}");
                }

                if (ok) {
                    rows.remove(row);
                    countLabel.setText(rows.size() + " deleted item"
                            + (rows.size() == 1 ? "" : "s"));

                    showAlert("Success",
                            "\"" + row.getItemName() + "\" has been restored to Active.");
                } else {
                    showAlert("Error", "Restore failed. Please try again.");
                }

            } catch (DBConnection.NoConnectionException e) {

                PasswordManager.showAlert(
                        "No Internet",
                        "Please connect to the Internet and try again."
                );

            } catch (Exception e) {

                PasswordManager.showAlert(
                        "Error",
                        "Something went wrong. Please try again."
                );

                e.printStackTrace();
            }
        });
    }

    // =========================================================
    // CLOSE
    // =========================================================
    @FXML
    private void handleClose() {
        Stage stage = (Stage) recycleBinTable.getScene().getWindow();
        stage.close();
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================
    // INNER CLASS — Row model for the TableView
    // =========================================================
    public static class RecycleBinRow {
        private final int    id;
        private final String type;
        private final String itemName;
        private final String category;
        private final String dateReported;
        private final String itemStatus;

        public RecycleBinRow(int id, String type, String itemName,
                             String category, String dateReported,
                             String itemStatus) {
            this.id           = id;
            this.type         = type;
            this.itemName     = itemName;
            this.category     = category;
            this.dateReported = dateReported;
            this.itemStatus   = itemStatus;
        }

        public int    getId()           { return id; }
        public String getType()         { return type; }
        public String getItemName()     { return itemName; }
        public String getCategory()     { return category; }
        public String getDateReported() { return dateReported; }
        public String getItemStatus()   { return itemStatus; }
    }
}