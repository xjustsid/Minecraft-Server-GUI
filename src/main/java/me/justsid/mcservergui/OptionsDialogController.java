package me.justsid.mcservergui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OptionsDialogController {

    private static final Logger logger = LoggerFactory.getLogger(OptionsDialogController.class);

    @FXML
    private TextField javaPathField;
    @FXML
    private TextField serverJarField;
    @FXML
    private TextField minMemoryField;
    @FXML
    private TextField maxMemoryField;
    @FXML
    private TextField backupFolderField;
    @FXML
    private ComboBox<String> backupModeComboBox;
    @FXML
    private CheckBox autoSaveCheckbox;
    @FXML
    private CheckBox autoRestartCheckbox;
    @FXML
    private CheckBox keepAliveCheckbox;
    @FXML
    private CheckBox rollbackCheckbox;

    @FXML
    private TextField restartTimesField;

    private ConfigManager configManager;
    private AutoSaveManager autoSaveManager;
    private AutoRestartManager autoRestartManager;
    private KeepAliveManager keepAliveManager;
    private RollbackManager rollbackManager;

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @FXML
    public void initialize() {
        if (configManager == null) {
            configManager = new ConfigManager();
        }

        // Verhindern, dass Items mehrfach hinzugefügt werden
        if (backupModeComboBox.getItems().isEmpty()) {
            backupModeComboBox.getItems().addAll("Einmalig", "Wiederholt");
        }

        // Checkboxen initialisieren
        boolean autoSaveEnabled = Boolean.parseBoolean(configManager.getConfigValue("autoSave", "false"));
        boolean autoRestartEnabled = Boolean.parseBoolean(configManager.getConfigValue("autoRestart", "false"));
        boolean keepAliveEnabled = Boolean.parseBoolean(configManager.getConfigValue("keepAlive", "false"));
        boolean rollbackEnabled = Boolean.parseBoolean(configManager.getConfigValue("rollback", "false"));
        autoSaveCheckbox.setSelected(autoSaveEnabled);
        autoRestartCheckbox.setSelected(autoRestartEnabled);
        keepAliveCheckbox.setSelected(keepAliveEnabled);
        rollbackCheckbox.setSelected(rollbackEnabled);

        // Listener für Checkboxen (Synchronisierung mit der Hauptanwendung)
        autoSaveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("autoSave", String.valueOf(newValue));
            updateMainCheckbox("autoSave", newValue); // Bestehende Methode verwenden
        });

        autoRestartCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("autoRestart", String.valueOf(newValue));
            updateMainCheckbox("autoRestart", newValue); // Bestehende Methode verwenden
        });

        keepAliveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("keepAlive", String.valueOf(newValue));
            updateMainCheckbox("keepAlive", newValue);
        });

        rollbackCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("rollback", String.valueOf(newValue));
            updateMainCheckbox("rollback", newValue);
        });

        // Modusauswahl initialisieren
        String backupMode = configManager.getConfigValue("backupMode", "Einmalig");
        backupModeComboBox.setValue(backupMode);

        // Listener für Modusauswahl
            backupModeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("backupMode", newValue);
        });

        // Felder initialisieren
        javaPathField.setText(configManager.getConfigValue("javaPath", "java"));
        serverJarField.setText(configManager.getConfigValue("serverJar", "server.jar"));
        minMemoryField.setText(configManager.getConfigValue("minMemory", "1024M"));
        maxMemoryField.setText(configManager.getConfigValue("maxMemory", "2048M"));
        backupFolderField.setText(configManager.getConfigValue("backupFolder", "backups"));

        // Restart-Zeiten laden
        restartTimesField.setText(configManager.getConfigValue("restartTimes", "00:00,06:00,12:00,18:00"));

        // Tooltips für die Benutzerfreundlichkeit
        initializeTooltips();
    }


    private void initializeTooltips() {
        javaPathField.setTooltip(new Tooltip("Pfad zur Java-Installation. Beispiel: 'java' oder '/usr/bin/java'"));
        serverJarField.setTooltip(new Tooltip("Name oder Pfad der Server-JAR-Datei. Beispiel: 'server.jar'"));
        minMemoryField.setTooltip(new Tooltip("Minimaler Arbeitsspeicher (z. B. '1024M' oder '2G')"));
        maxMemoryField.setTooltip(new Tooltip("Maximaler Arbeitsspeicher (z. B. '2048M' oder '4G')"));
        backupFolderField.setTooltip(new Tooltip("Pfad zum Ordner, in dem Backups gespeichert werden. Beispiel: 'backups'"));
        backupModeComboBox.setTooltip(new Tooltip("Backup-Modus: 'Einmalig' (ein Backup) oder 'Wiederholt' (regelmäßige Backups)"));
        restartTimesField.setTooltip(new Tooltip("Zeiten für geplante Neustarts, getrennt durch Kommata (max. 4 Zeiten erlaubt). Beispiel: '00:00,06:00'"));
        autoSaveCheckbox.setTooltip(new Tooltip("Aktiviert automatische Backups zu geplanten Zeiten."));
        autoRestartCheckbox.setTooltip(new Tooltip("Aktiviert automatische Neustarts zu geplanten Zeiten."));
        keepAliveCheckbox.setTooltip(new Tooltip("Startet den Server automatisch neu, wenn er unerwartet abstürzt."));
        rollbackCheckbox.setTooltip(new Tooltip("Stellt das letzte Backup wieder her, wenn die Welt nicht geladen werden kann."));
    }

    private void updateMainCheckbox(String key, boolean value) {
        Controller mainController = Main.getPrimaryController();
        if (mainController != null) {
            if (key.equals("autoSave")) {
                mainController.autoSaveCheckbox.setSelected(value);
            } else if (key.equals("autoRestart")) {
                mainController.autoRestartCheckbox.setSelected(value);
            } else if (key.equals("keepAlive")) {
                mainController.keepAliveCheckbox.setSelected(value);
            } else if (key.equals("rollback")) {
                mainController.rollbackCheckbox.setSelected(value);
            }
        }
    }

    public void setManagers(AutoSaveManager autoSaveManager, AutoRestartManager autoRestartManager) {
        this.autoSaveManager = autoSaveManager;
        this.autoRestartManager = autoRestartManager;
    }

    public void setKeepAliveAndRollbackManagers(KeepAliveManager keepAliveManager, RollbackManager rollbackManager) {
        this.keepAliveManager = keepAliveManager;
        this.rollbackManager = rollbackManager;
    }

    @FXML
    private void onSaveClick() {
        // Überprüfe die Eingaben, bevor wir fortfahren
        if (!validateInputs()) {
            showAlert("Ungültige Eingabe", "Bitte überprüfen Sie Ihre Eingaben und versuchen Sie es erneut.");
            return;
        }

        // Speichern der Konfigurationen (Werte der Eingabefelder)
        configManager.setConfigValue("restartTimes", restartTimesField.getText().trim());
        configManager.setConfigValue("autoSave", String.valueOf(autoSaveCheckbox.isSelected()));
        configManager.setConfigValue("autoRestart", String.valueOf(autoRestartCheckbox.isSelected()));
        configManager.setConfigValue("keepAlive", String.valueOf(keepAliveCheckbox.isSelected()));
        configManager.setConfigValue("rollback", String.valueOf(rollbackCheckbox.isSelected()));
        configManager.setConfigValue("backupMode", backupModeComboBox.getValue());
        configManager.setConfigValue("javaPath", javaPathField.getText());
        configManager.setConfigValue("serverJar", serverJarField.getText());
        configManager.setConfigValue("minMemory", minMemoryField.getText());
        configManager.setConfigValue("maxMemory", maxMemoryField.getText());

        // Backup-Ordner validieren und speichern
        String backupFolderInput = backupFolderField.getText().trim();
        if (backupFolderInput.isEmpty()) {
            backupFolderInput = System.getProperty("user.dir") + File.separator + "backups";  // Default-Pfad
            backupFolderField.setText(backupFolderInput); // GUI aktualisieren
        }
        String backupFolder = createBackupFolderIfNotExists(backupFolderInput);
        configManager.setConfigValue("backupFolder", backupFolder);

        // Neustart-Manager und Auto-Save-Manager aktivieren
        if (autoRestartManager != null) {
            autoRestartManager.scheduleNextRestart();
            autoRestartManager.testTimeCalculations(); // Testlauf für Debugging
            logger.info("Auto-Restart-Zeit aktualisiert: {}", restartTimesField.getText());
        }

        if (autoSaveManager != null) {
            autoSaveManager.scheduleNextSave();
            logger.info("Auto-Save-Zeit aktualisiert: {}", restartTimesField.getText());
        }

        // Keep Alive und Rollback aktivieren/deaktivieren
        if (keepAliveManager != null) {
            if (keepAliveCheckbox.isSelected()) {
                keepAliveManager.start();
            } else {
                keepAliveManager.stop();
            }
        }

        if (rollbackManager != null) {
            if (rollbackCheckbox.isSelected()) {
                rollbackManager.start();
            } else {
                rollbackManager.stop();
            }
        }

        // Konfiguration speichern
        configManager.saveConfig();

        // Benachrichtige den Hauptcontroller über die Änderungen
        notifyMainController();

        // Bestätigung und Dialog schließen
        showAlert("Erfolgreich gespeichert", "Die Einstellungen wurden erfolgreich gespeichert.");
        closeDialog();

        // Logge den Erfolg
        logger.info("Konfiguration erfolgreich gespeichert.");
    }


    private void notifyMainController() {
        Controller mainController = Main.getPrimaryController();
        if (mainController != null) {
            mainController.updateLabels(); // Labels aktualisieren
        }
    }


    @FXML
    private void onCancelClick() {
        logger.info("Optionen wurden abgebrochen.");
        notifyMainController();
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) restartTimesField.getScene().getWindow();
        notifyMainController();
        stage.close();
    }

    private boolean validateInputs() {
        String minMemory = minMemoryField.getText().trim();
        String maxMemory = maxMemoryField.getText().trim();
        String serverJar = serverJarField.getText().trim();
        String javaPath = javaPathField.getText().trim();
        
        if (javaPath.isEmpty()) {
            showAlert("Ungültige Eingabe", "Das Java Path-Feld darf nicht leer sein.");
            return false;
        }
        
        if (serverJar.isEmpty()) {
            showAlert("Ungültige Eingabe", "Das Server Jar-Feld darf nicht leer sein.");
            return false;
        }
        
        if (minMemory.isEmpty() || maxMemory.isEmpty()) {
            showAlert("Ungültige Eingabe", "Die Felder für Min und Max Memory dürfen nicht leer sein.");
            return false;
        }

        // minMemory > maxMemory prüfen
        if (parseMemory(minMemory) > parseMemory(maxMemory)) {
            showAlert("Ungültige Eingabe", "Min Memory darf nicht größer als Max Memory sein.");
            return false;
        }

        // Speichergrößen validieren
        if (!minMemory.matches("\\d+[MG]") || !maxMemory.matches("\\d+[MG]")) {
            showAlert("Ungültige Eingabe", "Speichergrößen müssen im Format '1024M' oder '2G' angegeben werden.");
            return false;
        }

        // Server-JAR existiert
        File serverJarFile = new File(serverJar);
        if (!serverJarFile.exists()) {
            showAlert("Ungültige Eingabe", "Die Server-JAR-Datei existiert nicht: " + serverJar);
            return false;
        }
        
        // Java-Pfad ist gültig und ausführbar
        File javaExe = new File(javaPath);
        if (!javaExe.exists() || !javaExe.canExecute()) {
            // Versuchen den PATH zu durchsuchen, falls nur "java" angegeben wurde
            String pathFromEnv = findJavaInPath(javaPath);
            if (pathFromEnv == null) {
                showAlert("Ungültige Eingabe", "Der Java-Pfad ist ungültig oder nicht ausführbar: " + javaPath);
                return false;
            }
        }

        if (backupFolderField.getText().trim().isEmpty()) {
            showAlert("Ungültige Eingabe", "Das Backup Folder-Feld darf nicht leer sein.");
            return false;
        }

        // Backup-Ordner beschreibbar
        String backupFolder = backupFolderField.getText().trim();
        File backupDir = new File(backupFolder);
        if (!backupDir.exists()) {
            try {
                java.nio.file.Files.createDirectories(backupDir.toPath());
            } catch (java.io.IOException e) {
                showAlert("Ungültige Eingabe", "Backup-Ordner konnte nicht erstellt werden: " + e.getMessage());
                return false;
            }
        }
        if (!backupDir.canWrite()) {
            showAlert("Ungültige Eingabe", "Backup-Ordner ist nicht beschreibbar.");
            return false;
        }

        String restartTimes = restartTimesField.getText();
        if (restartTimes == null || restartTimes.trim().isEmpty()) {
            showAlert("Ungültige Eingabe", "Bitte geben Sie mindestens eine Restart-Zeit ein.");
            return false;
        }

        String[] times = restartTimes.split(",");
        
        // Mehr als 4 Zeiten prüfen
        if (times.length > 4) {
            showAlert("Ungültige Eingabe", "Maximal 4 Restart-Zeiten erlaubt.");
            return false;
        }
        
        // Auf doppelte Zeiten prüfen
        java.util.Set<String> uniqueTimes = new java.util.HashSet<>();
        
        for (String time : times) {
            String trimmedTime = time.trim();
            if (!trimmedTime.matches("\\d{2}:\\d{2}")) {
                showAlert("Ungültige Eingabe", "Restart-Zeiten müssen im Format HH:mm angegeben werden (z. B. 00:00).");
                return false;
            }

            String[] parts = trimmedTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
        
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                showAlert("Ungültige Eingabe", "Restart-Zeiten müssen zwischen 00:00 und 23:59 liegen.");
                return false;
            }
            
            // Doppelte Zeit prüfen
            if (!uniqueTimes.add(trimmedTime)) {
                showAlert("Ungültige Eingabe", "Doppelte Zeit gefunden: " + trimmedTime);
                return false;
            }
        }

        return true;
    }
    
    private int parseMemory(String memory) {
        // Konvertiert Memory-String (z.B. "1024M", "2G") zu MB
        memory = memory.toUpperCase().trim();
        int multiplier = 1;
        
        if (memory.endsWith("G")) {
            multiplier = 1024;
            memory = memory.substring(0, memory.length() - 1);
        } else if (memory.endsWith("M")) {
            memory = memory.substring(0, memory.length() - 1);
        }
        
        try {
            return Integer.parseInt(memory) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String createBackupFolderIfNotExists(String backupFolderPath) {
        File folder = new File(backupFolderPath);

        // Falls der Pfad nicht absolut ist, relativ zum Arbeitsverzeichnis erstellen
        if (!folder.isAbsolute()) {
            folder = new File(System.getProperty("user.dir"), backupFolderPath);
        }

        // Ordner erstellen, falls er nicht existiert
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.info("Backup-Ordner erstellt: {}", folder.getAbsolutePath());
            } else {
                logger.error("Backup-Ordner konnte nicht erstellt werden: {}", folder.getAbsolutePath());
            }
        }

        return folder.getAbsolutePath();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String findJavaInPath(String javaName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        boolean isWindows = OsUtils.IS_WINDOWS;
        String[] paths = pathEnv.split(File.pathSeparator);
        String executableName = isWindows
                ? (javaName.endsWith(".exe") ? javaName : javaName + ".exe")
                : (javaName.endsWith(".exe") ? javaName.substring(0, javaName.length() - 4) : javaName);

        for (String dir : paths) {
            File javaFile = new File(dir, executableName);
            if (javaFile.exists() && javaFile.canExecute()) {
                return javaFile.getAbsolutePath();
            }
        }
        return null;
    }

}
