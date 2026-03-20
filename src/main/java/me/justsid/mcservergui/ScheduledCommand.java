package me.justsid.mcservergui;

import java.util.UUID;

public class ScheduledCommand {
    private String id;
    private String command;
    private TriggerType triggerType;
    private int playerCountCondition;
    private String timeCondition;
    private boolean repeat;
    private boolean enabled;

    public enum TriggerType {
        PLAYER_COUNT_ZERO,
        PLAYER_COUNT_LESS_THAN,
        PLAYER_COUNT_MORE_THAN,
        AT_TIME,
        EVERY_X_MINUTES
    }

    public ScheduledCommand() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.repeat = false;
        this.triggerType = TriggerType.PLAYER_COUNT_ZERO;
    }

    public ScheduledCommand(String command, TriggerType triggerType) {
        this();
        this.command = command;
        this.triggerType = triggerType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public int getPlayerCountCondition() {
        return playerCountCondition;
    }

    public void setPlayerCountCondition(int playerCountCondition) {
        this.playerCountCondition = playerCountCondition;
    }

    public String getTimeCondition() {
        return timeCondition;
    }

    public void setTimeCondition(String timeCondition) {
        this.timeCondition = timeCondition;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append(command);
        TriggerType effectiveTriggerType = triggerType != null ? triggerType : TriggerType.PLAYER_COUNT_ZERO;
        
        switch (effectiveTriggerType) {
            case PLAYER_COUNT_ZERO:
                sb.append(" (Keine Spieler)");
                break;
            case PLAYER_COUNT_LESS_THAN:
                sb.append(" (Spieler < " + playerCountCondition + ")");
                break;
            case PLAYER_COUNT_MORE_THAN:
                sb.append(" (Spieler > " + playerCountCondition + ")");
                break;
            case AT_TIME:
                sb.append(" (Zeit: " + timeCondition + ")");
                break;
            case EVERY_X_MINUTES:
                sb.append(" (Alle " + playerCountCondition + " Min)");
                break;
        }
        
        if (repeat) {
            sb.append(" [Wdh]");
        }
        
        return sb.toString();
    }

    public boolean shouldExecute(int currentPlayerCount, String currentTime) {
        if (!enabled) return false;

        TriggerType effectiveTriggerType = triggerType != null ? triggerType : TriggerType.PLAYER_COUNT_ZERO;

        switch (effectiveTriggerType) {
            case PLAYER_COUNT_ZERO:
                return currentPlayerCount == 0;
            case PLAYER_COUNT_LESS_THAN:
                return currentPlayerCount < playerCountCondition;
            case PLAYER_COUNT_MORE_THAN:
                return currentPlayerCount > playerCountCondition;
            case AT_TIME:
                return currentTime != null && currentTime.equals(timeCondition);
            case EVERY_X_MINUTES:
                return true;
            default:
                return false;
        }
    }
}
