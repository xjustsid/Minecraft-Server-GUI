package me.justsid.mcservergui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstConfigController {
    private static final Logger logger = LoggerFactory.getLogger(FirstConfigController.class);
    
    @FXML
    private TextField serverJarField;
    @FXML
    private TextField minMemoryField;
    @FXML
    private TextField maxMemoryField;
    @FXML
    private TextField javaPathField;
    @FXML
    private TextField backupFolderField;
    @FXML
    private Pane focusPane;

    private final ConfigManager configManager = new ConfigManager();

    @FXML
    public void initialize() {
        // Java automatisch erkennen und Feld füllen
        String detectedJavaPath = detectJavaPath();
        if (detectedJavaPath != null) {
            javaPathField.setText(detectedJavaPath);
            logger.info("Java automatisch erkannt: {}", detectedJavaPath);
        }

        // Setzt den Fokus beim Start auf das unsichtbare Pane
        Platform.runLater(() -> focusPane.requestFocus());

        // Initialisiere die Felder mit aktuellen Konfigurationswerten oder Standardwerten
        serverJarField.setText("");
        minMemoryField.setText("");
        maxMemoryField.setText("");
        backupFolderField.setText("");

        // Fokus vom ersten Feld entfernen
        Platform.runLater(() -> serverJarField.getParent().requestFocus());

        // Tooltips für die Felder
        Tooltip serverJarTooltip = new Tooltip("Name der Server-JAR-Datei. Beispiel: minecraft-server.jar");
        Tooltip minMemoryTooltip = new Tooltip("Minimale Menge an Arbeitsspeicher, die der Server verwenden soll.Beispiel: 1024M für 1Gigabyte / 2G für 2Gigabyte / 4G für 4Gigabyte / 8G für 8Gigabyte");
        Tooltip maxMemoryTooltip = new Tooltip("Maximale Menge an Arbeitsspeicher, die der Server verwenden soll.Beispiel: 1024M / 2G für 2Gigabyte / 4G für 4Gigabyte / 8G für 8Gigabyte");
        Tooltip backupFolderTooltip = new Tooltip("Pfad zum Ordner, in dem Backups gespeichert werden. Beispiel: backups");

        serverJarField.setTooltip(serverJarTooltip);
        minMemoryField.setTooltip(minMemoryTooltip);
        maxMemoryField.setTooltip(maxMemoryTooltip);
        backupFolderField.setTooltip(backupFolderTooltip);

    }

    /**
     * Erkennt automatisch den Java-Pfad.
     * Sucht in JAVA_HOME, PATH und gängigen Installationsorten.
     */
    private String detectJavaPath() {
        // 1. JAVA_HOME Umgebungsvariable
        String javaHome = System.getProperty("java.home");
        Path javaExe = Paths.get(javaHome, "bin", "java.exe");
        if (Files.exists(javaExe)) {
            return javaExe.toAbsolutePath().toString();
        }

        // 2. JAVA_HOME Umgebungsvariable (System)
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null) {
            javaExe = Paths.get(envJavaHome, "bin", "java.exe");
            if (Files.exists(javaExe)) {
                return javaExe.toAbsolutePath().toString();
            }
        }

        // 3. Aktuelle Java-Executable die gerade läuft
        Path currentJava = Paths.get(System.getProperty("java.home"), "bin", "java");
        if (Files.exists(currentJava)) {
            return currentJava.toAbsolutePath().toString();
        }

        // 4. Gängige Pfade unter Windows
        String[] commonPaths = {
            "C:\\Program Files\\Java\\jdk-23\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-22\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-20\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-19\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-18\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-17\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-16\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-15\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-14\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-13\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-12\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-11\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-10\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk-9\\bin\\java.exe",
            "C:\\Program Files\\Java\\jdk1.8.0\\bin\\java.exe",
            "C:\\Program Files (x86)\\Java\\jdk1.8.0\\bin\\java.exe"
        };

        for (String path : commonPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        logger.warn("Konnte Java nicht automatisch erkennen. Bitte manuell auswählen.");
        return null;
    }

    @FXML
    private void saveConfig() {
        // Eingaben validieren
        if (!validateFields()) {
            showAlert("Ungültige Eingabe", "Bitte überprüfen Sie die Felder und geben Sie gültige Werte ein.");
            return;
        }

        // Werte speichern
        configManager.setConfigValue("serverJar", serverJarField.getText().trim());
        configManager.setConfigValue("minMemory", minMemoryField.getText().trim());
        configManager.setConfigValue("maxMemory", maxMemoryField.getText().trim());
        configManager.setConfigValue("javaPath", javaPathField.getText().trim());
        configManager.setConfigValue("backupFolder", backupFolderField.getText().trim());
        configManager.saveConfig();

        showAlert("Erfolg", "Die Konfiguration wurde erfolgreich gespeichert.");
        closeDialog();
    }

    @FXML
    private void cancelConfig() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) serverJarField.getScene().getWindow();
        stage.close();
    }

    /**
     * Validiert die Eingabefelder.
     *
     * @return true, wenn alle Felder gültige Werte enthalten.
     */
    private boolean validateFields() {
        if (serverJarField.getText().trim().isEmpty() ||
                minMemoryField.getText().trim().isEmpty() ||
                maxMemoryField.getText().trim().isEmpty())

                 {
            return false;
        }

        // Speicherangaben validieren (z. B. 1024M oder 2G)
        return minMemoryField.getText().matches("\\d+[MG]") &&
                maxMemoryField.getText().matches("\\d+[MG]");
    }

    /**
     * Zeigt einen einfachen Alert-Dialog an.
     *
     * @param title   Der Titel des Dialogs.
     * @param message Die Nachricht des Dialogs.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
