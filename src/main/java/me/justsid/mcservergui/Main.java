package me.justsid.mcservergui;

import static me.justsid.mcservergui.Constants.*;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.Objects;

/**
 * Hauptklasse der McServerGUI Anwendung.
 * Startet die JavaFX-Anwendung und verwaltet das Hauptfenster.
 * 
 */
public class Main extends Application {
    
    /** Statische Referenz auf den Haupt-Controller für Zugriff von anderen Klassen. */
    private static Controller primaryController;
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Gibt die Haupt-Controller-Instanz zurück.
     * 
     * @return Der Haupt-Controller oder null wenn nicht initialisiert.
     */
    public static Controller getPrimaryController() {
        return primaryController;
    }

    /**
     * Startet die JavaFX-Anwendung.
     * Prüft ob es der erste Start ist und zeigt ggf. den Konfigurationsdialog.
     * 
     * @param primaryStage Das Hauptfenster der Anwendung.
     */
    @Override
    public void start(Stage primaryStage) {
        ConfigManager configManager = new ConfigManager();

        if (configManager.isFirstRun()) {
            logger.info("No configuration found. Opening FirstConfigDialog.");
            showFirstConfigDialog();
        }

        startMainApp(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        logger.info("Application stop() called - stopping server if running...");
        if (primaryController != null) {
            primaryController.stopServerOnShutdown();
        }
        super.stop();
    }

    /**
     * Zeigt den Ersteinrichtungsdialog an.
     */
    private void showFirstConfigDialog() {
        try {
            Stage dialogStage = new Stage();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("FirstConfigDialog.fxml"));
            Scene scene = new Scene(loader.load());
            dialogStage.setScene(scene);
            dialogStage.setTitle("First Configuration");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load FirstConfigDialog: {}", e.getMessage(), e);
            showErrorDialog("Fehler beim Laden des Dialogs", "Die Anwendung kann nicht fortgesetzt werden.");
        }
    }

    /**
     * Startet die Hauptanwendung mit dem Hauptfenster.
     * 
     * @param primaryStage Das Hauptfenster.
     */
    private void startMainApp(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("MCServerGUI-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);
            primaryStage.setTitle("Minecraft Server GUI");
            primaryStage.setScene(scene);

            primaryController = fxmlLoader.getController();

            // Window-Close-Request Handler - stoppt Server beim Schließen des Fensters
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Window close requested - stopping server if running...");
                if (primaryController != null) {
                    primaryController.stopServerOnShutdown();
                }
            });

            // Shutdown Hook - stoppt Server bei JVM-Shutdown (z.B. Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("JVM shutdown - stopping server if running...");
                if (primaryController != null) {
                    primaryController.stopServerOnShutdown();
                }
            }));

            primaryStage.show();
            applyStyles(scene);
            logger.info("Application started successfully.");
        } catch (IOException e) {
            logger.error("Failed to start application: {}", e.getMessage(), e);
            showErrorDialog("Fehler beim Starten der Anwendung", "Die Anwendung konnte nicht gestartet werden.");
        }
    }

    /**
     * Wendet das CSS-Stylesheet auf die Szene an.
     * 
     * @param scene Die Szene die gestylt werden soll.
     */
    private void applyStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
    }

    /**
     * Zeigt einen Fehlerdialog an und beendet die Anwendung.
     * 
     * @param title Der Titel des Dialogs.
     * @param message Die Fehlermeldung.
     */
    private void showErrorDialog(String title, String message) {
        logger.error(message);
        System.exit(1);
    }

    /**
     * Einstiegspunkt der Anwendung.
     * 
     * @param args Kommandozeilenargumente.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
