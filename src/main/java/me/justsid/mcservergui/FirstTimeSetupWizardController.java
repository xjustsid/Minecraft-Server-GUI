package me.justsid.mcservergui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public class FirstTimeSetupWizardController {
    private static final Logger logger = LoggerFactory.getLogger(FirstTimeSetupWizardController.class);

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
    private final JavaDetectionService javaDetectionService = new JavaDetectionService();
    private boolean setupCompleted;

    @FXML
    public void initialize() {
        SetupState state = SetupState.fromConfig(configManager);
        serverJarField.setText(state.serverJar());
        minMemoryField.setText(state.minMemory());
        maxMemoryField.setText(state.maxMemory());
        backupFolderField.setText(state.backupFolder());

        Optional<JavaDetectionService.JavaCandidate> bestCandidate = javaDetectionService.detectBestCandidate();
        if (bestCandidate.isPresent()) {
            JavaDetectionService.JavaCandidate candidate = bestCandidate.get();
            javaPathField.setText(candidate.executable().toString());
            logger.info("Java automatisch erkannt: {} ({})", candidate.executable(), candidate.version());
        } else {
            javaPathField.setText(state.javaPath());
            logger.warn("Keine kompatible Java-Version automatisch erkannt. Fallback auf konfigurierten Wert.");
        }

        Platform.runLater(() -> focusPane.requestFocus());
        Platform.runLater(() -> serverJarField.getParent().requestFocus());

        serverJarField.setTooltip(new Tooltip("Name oder Pfad der Server-JAR-Datei. Beispiel: server.jar"));
        javaPathField.setTooltip(new Tooltip("Pfad zur Java-Executable. Es wird Java 17+ empfohlen."));
        minMemoryField.setTooltip(new Tooltip("Minimale Menge an Arbeitsspeicher, z. B. 1024M oder 2G"));
        maxMemoryField.setTooltip(new Tooltip("Maximale Menge an Arbeitsspeicher, z. B. 2048M oder 4G"));
        backupFolderField.setTooltip(new Tooltip("Pfad zum Backup-Ordner. Standard: ./backups"));
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }

    @FXML
    private void saveConfig() {
        SetupState state = buildSetupState(
            serverJarField.getText().trim(),
            javaPathField.getText().trim(),
            minMemoryField.getText().trim(),
            maxMemoryField.getText().trim(),
            backupFolderField.getText().trim()
        );

        String validationError = validate(state);
        if (validationError != null) {
            showAlert(Alert.AlertType.ERROR, "Ungültige Eingabe", validationError);
            return;
        }

        for (Map.Entry<String, String> entry : state.toConfigMap().entrySet()) {
            configManager.setConfigValueSilently(entry.getKey(), entry.getValue());
        }
        configManager.saveConfig();

        setupCompleted = true;
        showAlert(Alert.AlertType.INFORMATION, "Erfolg", "Die Erstkonfiguration wurde gespeichert.");
        closeDialog();
    }

    @FXML
    private void cancelConfig() {
        setupCompleted = false;
        closeDialog();
    }

    private String validate(SetupState state) {
        if (!state.hasRequiredFields()) {
            return "Bitte alle Pflichtfelder ausfüllen.";
        }

        Path serverDirectory = Paths.get(state.serverDirectory()).normalize();
        Path serverJarPath = serverDirectory.resolve(state.serverJar()).normalize();
        if (!Files.exists(serverJarPath) || !Files.isRegularFile(serverJarPath)) {
            return "Die angegebene Server-JAR wurde nicht gefunden. Bitte einen gültigen Pfad zur Server-JAR angeben.";
        }

        if (!state.minMemory().matches("\\d+[MG]") || !state.maxMemory().matches("\\d+[MG]")) {
            return "RAM-Werte müssen im Format 1024M oder 2G angegeben werden.";
        }

        if (toMegabytes(state.maxMemory()) < toMegabytes(state.minMemory())) {
            return "Max RAM muss größer oder gleich Min RAM sein.";
        }

        Path javaPath = Paths.get(state.javaPath());
        if (!"java".equalsIgnoreCase(state.javaPath())
            && (!Files.exists(javaPath) || !Files.isRegularFile(javaPath))) {
            return "Der angegebene Java-Pfad existiert nicht oder ist keine Datei.";
        }

        if ("java".equalsIgnoreCase(state.javaPath())) {
            if (!javaDetectionService.isCompatible(state.javaPath())) {
                return "Das Java-Kommando aus dem PATH ist nicht kompatibel. Es wird Java 17 oder neuer benötigt.";
            }
        } else if (!javaDetectionService.isCompatible(javaPath)) {
            return "Die angegebene Java-Version ist nicht kompatibel. Es wird Java 17 oder neuer benötigt.";
        }

        return null;
    }

    private SetupState buildSetupState(String serverJarInput, String javaPath, String minMemory, String maxMemory, String backupFolder) {
        Path serverJarPath = Paths.get(serverJarInput).normalize();
        Path serverDirectory = serverJarPath.getParent();

        String normalizedServerJar = serverJarPath.getFileName() == null
            ? serverJarInput
            : serverJarPath.getFileName().toString();
        String normalizedServerDirectory = serverDirectory == null
            ? "."
            : serverDirectory.toString();

        return new SetupState(
            normalizedServerJar,
            javaPath,
            minMemory,
            maxMemory,
            backupFolder,
            normalizedServerDirectory
        );
    }

    private long toMegabytes(String value) {
        String normalized = value.trim().toUpperCase();
        long amount = Long.parseLong(normalized.substring(0, normalized.length() - 1));
        return normalized.endsWith("G") ? amount * 1024 : amount;
    }

    private void closeDialog() {
        Stage stage = (Stage) serverJarField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}