package me.justsid.mcservergui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CommandDialogController {
    private static final Logger logger = LoggerFactory.getLogger(CommandDialogController.class);

    @FXML
    private TextField commandField;

    @FXML
    private ComboBox<String> triggerTypeComboBox;

    @FXML
    private Label conditionLabel;

    @FXML
    private TextField conditionField;

    @FXML
    private TextField timeField;

    @FXML
    private CheckBox repeatCheckbox;

    @FXML
    private ListView<String> commandListView;

    private ScheduledCommandManager commandManager;
    private ObservableList<String> commandDisplayList = FXCollections.observableArrayList();
    private ScheduledCommand selectedCommand;

    @FXML
    public void initialize() {
        triggerTypeComboBox.getItems().addAll(
            "Keine Spieler online",
            "Weniger als X Spieler",
            "Mehr als X Spieler",
            "Zur bestimmten Zeit",
            "Alle X Minuten"
        );
        triggerTypeComboBox.setValue("Keine Spieler online");
        
        commandListView.setItems(commandDisplayList);
        
        commandListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadCommandDetails(newVal);
            }
        });
    }

    public void setCommandManager(ScheduledCommandManager commandManager) {
        this.commandManager = commandManager;
        refreshCommandList();
    }

    private void refreshCommandList() {
        commandDisplayList.clear();
        if (commandManager != null) {
            for (ScheduledCommand cmd : commandManager.getCommands()) {
                commandDisplayList.add(cmd.getDisplayText());
            }
        }
    }

    private void loadCommandDetails(String displayText) {
        if (commandManager == null) return;
        
        for (ScheduledCommand cmd : commandManager.getCommands()) {
            if (cmd.getDisplayText().equals(displayText)) {
                selectedCommand = cmd;
                commandField.setText(cmd.getCommand());
                repeatCheckbox.setSelected(cmd.isRepeat());
                
                switch (cmd.getTriggerType()) {
                    case PLAYER_COUNT_ZERO:
                        triggerTypeComboBox.setValue("Keine Spieler online");
                        conditionField.setText("");
                        conditionField.setVisible(false);
                        timeField.setVisible(false);
                        break;
                    case PLAYER_COUNT_LESS_THAN:
                        triggerTypeComboBox.setValue("Weniger als X Spieler");
                        conditionField.setText(String.valueOf(cmd.getPlayerCountCondition()));
                        conditionField.setVisible(true);
                        timeField.setVisible(false);
                        break;
                    case PLAYER_COUNT_MORE_THAN:
                        triggerTypeComboBox.setValue("Mehr als X Spieler");
                        conditionField.setText(String.valueOf(cmd.getPlayerCountCondition()));
                        conditionField.setVisible(true);
                        timeField.setVisible(false);
                        break;
                    case AT_TIME:
                        triggerTypeComboBox.setValue("Zur bestimmten Zeit");
                        conditionField.setVisible(false);
                        timeField.setText(cmd.getTimeCondition());
                        timeField.setVisible(true);
                        break;
                    case EVERY_X_MINUTES:
                        triggerTypeComboBox.setValue("Alle X Minuten");
                        conditionField.setText(String.valueOf(cmd.getPlayerCountCondition()));
                        conditionField.setVisible(true);
                        timeField.setVisible(false);
                        break;
                }
                break;
            }
        }
    }

    @FXML
    private void onTriggerTypeChanged() {
        String selected = triggerTypeComboBox.getValue();
        
        conditionField.setVisible(false);
        timeField.setVisible(false);
        
        if (selected == null) return;
        
        switch (selected) {
            case "Keine Spieler online":
                conditionLabel.setText("Bedingung:");
                break;
            case "Weniger als X Spieler":
                conditionLabel.setText("Spieler <");
                conditionField.setVisible(true);
                conditionField.setPromptText("Anzahl");
                break;
            case "Mehr als X Spieler":
                conditionLabel.setText("Spieler >");
                conditionField.setVisible(true);
                conditionField.setPromptText("Anzahl");
                break;
            case "Zur bestimmten Zeit":
                conditionLabel.setText("Uhrzeit:");
                timeField.setVisible(true);
                timeField.setPromptText("HH:mm");
                break;
            case "Alle X Minuten":
                conditionLabel.setText("Intervall:");
                conditionField.setVisible(true);
                conditionField.setPromptText("Minuten");
                break;
        }
    }

    @FXML
    private void onAddClick() {
        String command = commandField.getText();
        if (command == null || command.trim().isEmpty()) {
            showAlert("Fehler", "Bitte geben Sie einen Befehl ein.");
            return;
        }

        ScheduledCommand.TriggerType triggerType = getTriggerType();
        int playerCount = 0;
        String time = null;

        try {
            switch (triggerType) {
                case PLAYER_COUNT_LESS_THAN:
                case PLAYER_COUNT_MORE_THAN:
                case EVERY_X_MINUTES:
                    if (conditionField.getText().isEmpty()) {
                        showAlert("Fehler", "Bitte geben Sie eine Zahl ein.");
                        return;
                    }
                    playerCount = Integer.parseInt(conditionField.getText());
                    break;
                case AT_TIME:
                    time = timeField.getText();
                    if (time == null || time.isEmpty()) {
                        showAlert("Fehler", "Bitte geben Sie eine Uhrzeit ein (HH:mm).");
                        return;
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            showAlert("Fehler", "Ungültige Zahl eingegeben.");
            return;
        }

        ScheduledCommand newCmd = new ScheduledCommand();
        newCmd.setCommand(command);
        newCmd.setTriggerType(triggerType);
        newCmd.setPlayerCountCondition(playerCount);
        newCmd.setTimeCondition(time);
        newCmd.setRepeat(repeatCheckbox.isSelected());
        newCmd.setEnabled(true);

        commandManager.addCommand(newCmd);
        refreshCommandList();
        clearFields();
        
        logger.info("Befehl hinzugefügt: {}", command);
    }

    @FXML
    private void onUpdateClick() {
        if (selectedCommand == null) {
            showAlert("Fehler", "Bitte wählen Sie einen Befehl aus der Liste aus.");
            return;
        }

        String command = commandField.getText();
        if (command == null || command.trim().isEmpty()) {
            showAlert("Fehler", "Bitte geben Sie einen Befehl ein.");
            return;
        }

        ScheduledCommand.TriggerType triggerType = getTriggerType();
        int playerCount = 0;
        String time = null;

        try {
            switch (triggerType) {
                case PLAYER_COUNT_LESS_THAN:
                case PLAYER_COUNT_MORE_THAN:
                case EVERY_X_MINUTES:
                    if (!conditionField.getText().isEmpty()) {
                        playerCount = Integer.parseInt(conditionField.getText());
                    }
                    break;
                case AT_TIME:
                    time = timeField.getText();
                    break;
            }
        } catch (NumberFormatException e) {
            showAlert("Fehler", "Ungültige Zahl eingegeben.");
            return;
        }

        selectedCommand.setCommand(command);
        selectedCommand.setTriggerType(triggerType);
        selectedCommand.setPlayerCountCondition(playerCount);
        selectedCommand.setTimeCondition(time);
        selectedCommand.setRepeat(repeatCheckbox.isSelected());

        commandManager.updateCommand(selectedCommand);
        refreshCommandList();
        
        logger.info("Befehl aktualisiert: {}", command);
    }

    @FXML
    private void onDeleteClick() {
        if (selectedCommand == null) {
            showAlert("Fehler", "Bitte wählen Sie einen Befehl aus der Liste aus.");
            return;
        }

        commandManager.removeCommand(selectedCommand.getId());
        refreshCommandList();
        clearFields();
        selectedCommand = null;
        
        logger.info("Befehl gelöscht.");
    }

    @FXML
    private void onCloseClick() {
        Stage stage = (Stage) commandField.getScene().getWindow();
        stage.close();
    }

    private ScheduledCommand.TriggerType getTriggerType() {
        String selected = triggerTypeComboBox.getValue();
        if (selected == null) return ScheduledCommand.TriggerType.PLAYER_COUNT_ZERO;
        
        switch (selected) {
            case "Keine Spieler online":
                return ScheduledCommand.TriggerType.PLAYER_COUNT_ZERO;
            case "Weniger als X Spieler":
                return ScheduledCommand.TriggerType.PLAYER_COUNT_LESS_THAN;
            case "Mehr als X Spieler":
                return ScheduledCommand.TriggerType.PLAYER_COUNT_MORE_THAN;
            case "Zur bestimmten Zeit":
                return ScheduledCommand.TriggerType.AT_TIME;
            case "Alle X Minuten":
                return ScheduledCommand.TriggerType.EVERY_X_MINUTES;
            default:
                return ScheduledCommand.TriggerType.PLAYER_COUNT_ZERO;
        }
    }

    private void clearFields() {
        commandField.clear();
        triggerTypeComboBox.setValue("Keine Spieler online");
        conditionField.clear();
        timeField.clear();
        repeatCheckbox.setSelected(false);
        selectedCommand = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
