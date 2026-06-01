package application;

import dao.FoundItemDAO;
import dao.LostItemDAO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.enums.ArchiveReason;

public class AdminApplication extends Application {

    private final LostItemDAO  lostDAO  = new LostItemDAO();
    private final FoundItemDAO foundDAO = new FoundItemDAO();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AdminDashboard.fxml"));
        BorderPane root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Lost and Found System");

        primaryStage.setMaximized(true);

        primaryStage.setScene(scene);
        primaryStage.show();

        startAutoArchiveScheduler();
    }

    private void startAutoArchiveScheduler() {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {

                    // Archive active items older than 60 days
                    lostDAO.autoArchiveExpired(60, ArchiveReason.AUTO_LOST_UNRESOLVED.getLabel());
                    foundDAO.autoArchiveExpired(60, ArchiveReason.AUTO_FOUND_UNCLAIMED.getLabel());

                    // Soft delete archived items older than 60 days
                    lostDAO.autoDeleteExpired(60);
                    foundDAO.autoDeleteExpired(60);

                }, 0, 1, java.util.concurrent.TimeUnit.HOURS);
    }

    public static void main(String[] args) {
        launch(args);
    }
}