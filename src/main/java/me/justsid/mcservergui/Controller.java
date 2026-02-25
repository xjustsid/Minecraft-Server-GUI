package me.justsid.mcservergui;

import static me.justsid.mcservergui.Constants.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Haupt-Controller für die McServerGUI Anwendung.
 * Verwaltet die UI-Interaktionen und koordiniert Server-Operationen.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public class Controller implements ServerOperationCoordinator.ServerStateListener {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @FXML
    private TextArea consoleOutput;

    @FXML
    private TextField commandInput;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button restartButton;

    @FXML
    public CheckBox autoSaveCheckbox;

    @FXML
    public CheckBox autoRestartCheckbox;

    @FXML
    public CheckBox keepAliveCheckbox;

    @FXML
    public CheckBox rollbackCheckbox;

    @FXML
    private Label lastSaveLabel;

    @FXML
    private Label nextSaveLabel;

    @FXML
    private Label lastRestartLabel;

    @FXML
    private Label nextRestartLabel;

    @FXML
    private Label serverStateLabel;

    @FXML
    private Label membersOnlineLabel;

    private AutoSaveManager autoSaveManager;
    private AutoRestartManager autoRestartManager;
    private KeepAliveManager keepAliveManager;
    private RollbackManager rollbackManager;
    private ScheduledCommandManager scheduledCommandManager;
    private ServerOperationCoordinator coordinator;

    private final ConfigManager configManager = new ConfigManager();
    private ServerProcessManager serverProcessManager;
    private Timer labelUpdaterTimer;

    public Controller() {
    }

    public void initialize() {
        serverProcessManager = new ServerProcessManager(configManager);
        serverProcessManager.setConsoleOutput(consoleOutput);
        
        // Spielerzahl-Update registrieren
        serverProcessManager.setPlayerCountConsumer(count -> {
            Platform.runLater(() -> {
                membersOnlineLabel.setText("Members Online: " + count);
            });
        });

        // Rollback-Überwachung registrieren
        serverProcessManager.setLogLineConsumer(line -> {
            if (rollbackManager != null && rollbackManager.isEnabled()) {
                rollbackManager.checkForWorldErrors(line);
            }
        });

        BackupManager backupManager = new BackupManager(configManager);
        coordinator = new ServerOperationCoordinator(serverProcessManager, backupManager, configManager);
        coordinator.setStateListener(this);

        autoSaveManager = new AutoSaveManager(configManager, coordinator);
        autoRestartManager = new AutoRestartManager(configManager, coordinator);
        keepAliveManager = new KeepAliveManager(serverProcessManager, coordinator, configManager);
        rollbackManager = new RollbackManager(serverProcessManager, coordinator, configManager);
        scheduledCommandManager = new ScheduledCommandManager(serverProcessManager, configManager);
        scheduledCommandManager.start();

        onStateChanged(ServerState.OFFLINE);
        logger.info("Controller initialized.");

        stopButton.setDisable(true);
        restartButton.setDisable(true);

        commandInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onCommandSend();
            }
        });

        autoSaveCheckbox.setSelected(Boolean.parseBoolean(configManager.getConfigValue("autoSave", "false")));
        autoRestartCheckbox.setSelected(Boolean.parseBoolean(configManager.getConfigValue("autoRestart", "false")));
        keepAliveCheckbox.setSelected(Boolean.parseBoolean(configManager.getConfigValue("keepAlive", "false")));
        rollbackCheckbox.setSelected(Boolean.parseBoolean(configManager.getConfigValue("rollback", "false")));

        // Listener für Checkboxen - beides in einem: config speichern UND timer starten/stoppen
        autoSaveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("autoSave", String.valueOf(newValue));
            logger.info("Auto Save Checkbox geändert: {}", newValue);
            if (newValue) {
                autoSaveManager.startAutoSave(true);
                appendToConsole("Auto Save aktiviert.");
            } else {
                autoSaveManager.stopAutoSave();
                appendToConsole("Auto Save deaktiviert.");
            }
        });

        autoRestartCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("autoRestart", String.valueOf(newValue));
            logger.info("Auto Restart Checkbox geändert: {}", newValue);
            if (newValue) {
                autoRestartManager.startAutoRestart(true);
                appendToConsole("Auto Restart aktiviert.");
            } else {
                autoRestartManager.stopAutoRestart();
                appendToConsole("Auto Restart deaktiviert.");
            }
        });

        keepAliveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("keepAlive", String.valueOf(newValue));
            logger.info("Keep Alive Checkbox geändert: {}", newValue);
            if (newValue) {
                keepAliveManager.start();
                appendToConsole("Keep Alive aktiviert.");
            } else {
                keepAliveManager.stop();
                appendToConsole("Keep Alive deaktiviert.");
            }
        });

        rollbackCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            configManager.setConfigValue("rollback", String.valueOf(newValue));
            logger.info("Rollback Checkbox geändert: {}", newValue);
            if (newValue) {
                rollbackManager.start();
                appendToConsole("Rollback aktiviert.");
            } else {
                rollbackManager.stop();
                appendToConsole("Rollback deaktiviert.");
            }
        });

        autoRestartManager.testTimeCalculations();
        autoSaveManager.testTimeCalculations();
        startLabelUpdater();
        updateLabels();
    }

    @Override
    public void onStateChanged(ServerState newState) {
        Platform.runLater(() -> {
            if (serverStateLabel == null) return;
            
            serverStateLabel.setText(newState.getDisplayName());
            serverStateLabel.setStyle("-fx-text-fill: " + newState.getColor() + ";");
            
            boolean isOnline = newState == ServerState.ONLINE;
            boolean canOperate = newState == ServerState.ONLINE || newState == ServerState.OFFLINE;
            
            if (startButton != null) startButton.setDisable(isOnline || coordinator.isOperationInProgress());
            if (stopButton != null) stopButton.setDisable(!isOnline || coordinator.isOperationInProgress());
            if (restartButton != null) restartButton.setDisable(!isOnline || coordinator.isOperationInProgress());
        });
    }

    public void updateLabels() {
        Platform.runLater(() -> {
            if (nextSaveLabel != null && lastSaveLabel != null && autoSaveManager != null) {
                nextSaveLabel.setText("Next Save: " + autoSaveManager.getNextSaveFormatted());
                lastSaveLabel.setText("Last Save: " + autoSaveManager.getLastSaveTime());
            }
            if (nextRestartLabel != null && lastRestartLabel != null && autoRestartManager != null) {
                nextRestartLabel.setText("Next Restart: " + autoRestartManager.getNextRestartFormatted());
                lastRestartLabel.setText("Last Restart: " + autoRestartManager.getLastRestartTime());
            }
        });
    }

    private void startLabelUpdater() {
        labelUpdaterTimer = new Timer(true);
        labelUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateLabels();
            }
        }, 0, LABEL_UPDATE_INTERVAL_MS); // Alle 1 Sekunde für bessere Reaktionszeit
    }

    @FXML
    public void onClose() {
        if (labelUpdaterTimer != null) {
            labelUpdaterTimer.cancel();
        }
        logger.info("Application closed. Timer stopped.");
    }

    public void stopServerOnShutdown() {
        if (keepAliveManager != null) {
            keepAliveManager.stop();
        }
        if (rollbackManager != null) {
            rollbackManager.stop();
        }
        if (scheduledCommandManager != null) {
            scheduledCommandManager.stop();
        }
        if (serverProcessManager != null && serverProcessManager.isRunning()) {
            logger.info("Stopping server on shutdown...");
            serverProcessManager.stopServerInternal();
            serverProcessManager.waitForServerFullyStopped(30);
        }
    }

    private void appendToConsole(String message) {
        Platform.runLater(() -> consoleOutput.appendText(message + "\n"));
    }

    @FXML
    private void onCommandSend() {
        String command = commandInput.getText();
        if (!command.isEmpty()) {
            serverProcessManager.sendCommand(command);
            appendToConsole("> " + command);
            commandInput.clear();
        } else {
            appendToConsole("No command entered.");
        }
    }

    @FXML
    private void onStartButtonClick() {
        logger.info("Start button clicked.");
        coordinator.startServer();
    }

    @FXML
    private void onStopButtonClick() {
        logger.info("Stop button clicked.");
        coordinator.stopServer();
    }

    @FXML
    private void onRestartButtonClick() {
        logger.info("Restart button clicked.");
        appendToConsole("Server wird neu gestartet...");
        coordinator.performRestart(true);
    }

    @FXML
    private void onSaveButtonClick() {
        if (coordinator.isOperationInProgress()) {
            appendToConsole("Eine Operation läuft bereits. Bitte warten.");
            return;
        }
        
        logger.info("Save button clicked - Server stoppen und Backup erstellen.");
        appendToConsole("Server wird gestoppt und Backup wird erstellt...");
        
        coordinator.performBackup();
    }

    @FXML
    private void onOpenFolderClick() {
        try {
            File projectFolder = new File(".");
            Desktop.getDesktop().open(projectFolder);
            logger.info("Project folder opened: {}", projectFolder.getAbsolutePath());
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen des Projektverzeichnisses.");
            logger.error("Error opening project folder: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void onOptionsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("OptionsDialog.fxml"));
            Stage optionsStage = new Stage();
            Scene scene = new Scene(loader.load());
            optionsStage.setScene(scene);
            optionsStage.setTitle("Einstellungen");
            optionsStage.initModality(Modality.APPLICATION_MODAL);

            OptionsDialogController dialogController = loader.getController();
            dialogController.setConfigManager(configManager);
            dialogController.setManagers(autoSaveManager, autoRestartManager);
            dialogController.setKeepAliveAndRollbackManagers(keepAliveManager, rollbackManager);

            optionsStage.showAndWait();
            appendToConsole("Einstellungen wurden geöffnet.");
            logger.info("Options dialog opened.");
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen des Options-Dialogs.");
            logger.error("Error opening options dialog: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void onServerPropertiesClick() {
        try {
            File propertiesFile = new File("server.properties");
            if (!propertiesFile.exists()) {
                appendToConsole("Die Datei 'server.properties' existiert nicht.");
                logger.warn("server.properties does not exist: {}", propertiesFile.getAbsolutePath());
                return;
            }
            Desktop.getDesktop().open(propertiesFile);
            logger.info("server.properties file opened: {}", propertiesFile.getAbsolutePath());
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen der server.properties-Datei.");
            logger.error("Error opening server.properties file: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void onReloadWhitelistClick() {
        String command = "whitelist reload";
        serverProcessManager.sendCommand(command);
        appendToConsole("> " + command);
        logger.info("Whitelist reload command sent.");
    }

    @FXML
    private void onViewLogsClick() {
        try {
            File logsFolder = new File("logs");
            if (!logsFolder.exists()) {
                appendToConsole("Logs-Ordner existiert nicht.");
                logger.warn("Logs folder does not exist: {}", logsFolder.getAbsolutePath());
                return;
            }
            Desktop.getDesktop().open(logsFolder);
            logger.info("Logs folder opened: {}", logsFolder.getAbsolutePath());
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen des Logs-Ordners.");
            logger.error("Error opening logs folder: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void onViewSavesClick() {
        try {
            String backupFolderPath = configManager.getConfigValue("backupFolder", "backups");
            File backupFolder = new File(backupFolderPath);
            if (!backupFolder.exists()) {
                appendToConsole("Backup-Ordner existiert nicht.");
                logger.warn("Backup folder does not exist: {}", backupFolder.getAbsolutePath());
                return;
            }
            Desktop.getDesktop().open(backupFolder);
            logger.info("Backup folder opened: {}", backupFolder.getAbsolutePath());
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen des Backup-Ordners.");
            logger.error("Error opening backup folder: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void onReloadClick() {
        consoleOutput.appendText("Reload Button wurde geklickt. Feature noch nicht implementiert.\n");
        logger.info("Reload GUI button clicked. Feature not yet implemented.");
    }

    @FXML
    private void onCommandClick() {
        consoleOutput.appendText("Command Button wurde geklickt. Feature noch nicht implementiert.\n");
        logger.info("Command button clicked. Feature not yet implemented.");
    }

    @FXML
    private void onScheduledCommandsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("CommandDialog.fxml"));
            Stage commandStage = new Stage();
            Scene scene = new Scene(loader.load());
            commandStage.setScene(scene);
            commandStage.setTitle("Geplante Befehle");
            commandStage.initModality(Modality.APPLICATION_MODAL);

            CommandDialogController dialogController = loader.getController();
            dialogController.setCommandManager(scheduledCommandManager);

            commandStage.showAndWait();
            logger.info("Scheduled commands dialog opened.");
        } catch (IOException e) {
            appendToConsole("Fehler beim Öffnen des Befehle-Dialogs.");
            logger.error("Error opening commands dialog: {}", e.getMessage(), e);
        }
    }
}
