package me.justsid.mcservergui;

import java.time.LocalDateTime;

/**
 * Verwaltet automatische Neustarts des Minecraft-Servers.
 * Erbt Timer-Logik von AbstractScheduledManager.
 * 
 */
public class AutoRestartManager extends AbstractScheduledManager {

    /**
     * Erstellt einen neuen AutoRestartManager.
     * 
     * @param configManager Der ConfigManager.
     * @param coordinator Der ServerOperationCoordinator.
     */
    public AutoRestartManager(ConfigManager configManager, ServerOperationCoordinator coordinator) {
        super(configManager, coordinator, "restartTimes", "Restart", AutoRestartManager.class);
    }

    @Override
    protected void performAction() {
        if (coordinator.isOperationInProgress()) {
            logger.info("Auto Restart übersprungen - eine andere Operation läuft bereits.");
            return;
        }

        logger.info("Auto Restart wird durchgeführt...");
        coordinator.performAutoSaveAndRestart();
        this.lastActionTime = LocalDateTime.now().toString();
        logger.info("Auto Restart angestoßen.");
    }

    public void stopAutoRestart() {
        this.isActive = false;
    }
    
    // Kompatibilitätsmethoden für bestehenden Code
    public void startAutoRestart(boolean activate) {
        start(activate);
    }
    
    public void scheduleNextRestart() {
        scheduleNext();
    }
    
    public String getLastRestartTime() {
        return getLastActionTimeFormatted();
    }
    
    public String getNextRestartFormatted() {
        return getNextActionFormatted();
    }
}
