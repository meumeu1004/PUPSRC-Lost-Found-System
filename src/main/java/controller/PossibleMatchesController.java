package controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FoundItem;
import model.LostItem;
import model.MatchResult;
import util.MatcherService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PossibleMatchesController {

    @FXML private Label                    resultCountLabel;
    @FXML private TableView<MatchResult>   matchTable;
    @FXML private TableColumn<MatchResult, String> scoreCol;
    @FXML private TableColumn<MatchResult, String> idCol;
    @FXML private TableColumn<MatchResult, String> nameCol;
    @FXML private TableColumn<MatchResult, String> categoryCol;
    @FXML private TableColumn<MatchResult, String> colorCol;
    @FXML private TableColumn<MatchResult, String> dateCol;
    @FXML private TableColumn<MatchResult, String> statusCol;
    @FXML private TableColumn<MatchResult, String> actionCol;
    @FXML private Label                    noResultsLabel;

    private AdminController adminController;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ── Entry point ───────────────────────────────────────────
    public void init(Object sourceItem, AdminController adminCtrl) {
        this.adminController = adminCtrl;
        setupColumns();

        // Run DB query off the FX thread to keep UI responsive
        Thread worker = new Thread(() -> {
            try {
                MatcherService matcher = new MatcherService();
                List<MatchResult> results;

                if (sourceItem instanceof LostItem lost) {
                    results = matcher.findForLost(lost);
                } else {
                    results = matcher.findForFound((FoundItem) sourceItem);
                }

                List<MatchResult> finalResults = results;
                Platform.runLater(() -> displayResults(finalResults));

            } catch (Exception e) {
                Platform.runLater(() -> resultCountLabel.setText("Error: could not retrieve matches."));
                e.printStackTrace();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ── Column definitions ────────────────────────────────────
    private void setupColumns() {
        scoreCol.setCellValueFactory(r ->
                new SimpleStringProperty(r.getValue().getScore() + "%"));

        idCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            int id = item instanceof LostItem l ? l.getId() : ((FoundItem) item).getId();
            String prefix = item instanceof LostItem ? "L-" : "F-";
            return new SimpleStringProperty(prefix + id);
        });

        nameCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            String name = item instanceof LostItem l ? l.getItemName() : ((FoundItem) item).getItemName();
            return new SimpleStringProperty(name);
        });

        categoryCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            String cat = item instanceof LostItem l ? l.getCategory() : ((FoundItem) item).getCategory();
            return new SimpleStringProperty(cat);
        });

        colorCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            String color = item instanceof LostItem l ? l.getColor() : ((FoundItem) item).getColor();
            return new SimpleStringProperty(color);
        });

        dateCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            String date;
            if (item instanceof LostItem l)
                date = l.getDateLost() != null ? l.getDateLost().format(DATE_FMT) : "-";
            else {
                FoundItem f = (FoundItem) item;
                date = f.getDateFound() != null ? f.getDateFound().format(DATE_FMT) : "-";
            }
            return new SimpleStringProperty(date);
        });

        statusCol.setCellValueFactory(r -> {
            Object item = r.getValue().getItem();
            String status = item instanceof LostItem l ? l.getItemStatus() : ((FoundItem) item).getItemStatus();
            return new SimpleStringProperty(status);
        });

        // "View" button column
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.setStyle("-fx-background-color: #710912; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 3 10;");
                btn.setOnAction(e -> {
                    MatchResult mr = getTableView().getItems().get(getIndex());
                    openItemView(mr.getItem());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ── Populate table ────────────────────────────────────────
    private void displayResults(List<MatchResult> results) {
        if (results.isEmpty()) {
            matchTable.setVisible(false);
            matchTable.setManaged(false);
            noResultsLabel.setVisible(true);
            noResultsLabel.setManaged(true);
            resultCountLabel.setText("No matches found.");
        } else {
            matchTable.getItems().setAll(results);
            resultCountLabel.setText(results.size() + " possible match(es) found.");
        }
    }

    // ── Open the standard PostItemView for the selected item ──
    private void openItemView(Object item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/PostItemView.fxml"));
            Parent root = loader.load();

            PostItemViewController ctrl = loader.getController();
            ctrl.setItem(item, adminController);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Item Details");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) matchTable.getScene().getWindow();
        stage.close();
    }
}
