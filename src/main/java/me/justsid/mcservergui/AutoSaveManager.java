package me.justsid.mcservergui;

import java.time.LocalDateTime;

/**
 * Verwaltet automatische Backups des Minecraft-Servers.
 * Erbt Timer-Logik von AbstractScheduledManager.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public class AutoSaveManager extends AbstractScheduledManager {

    /**
     * Erstellt einen neuen AutoSaveManager.
     * 
     * @param configManager Der ConfigManager.
     * @param coordinator Der ServerOperationCoordinator.
     */
    public AutoSaveManager(ConfigManager configManager, ServerOperationCoordinator coordinator) {
        super(configManager, coordinator, "saveTimes", "Save", AutoSaveManager.class);
    }

    @Override
    protected void performAction() {
        if (coordinator.isOperationInProgress()) {
            logger.info("Auto Save übersprungen - eine andere Operation läuft bereits.");
            return;
        }

        logger.info("Auto Save wird durchgeführt...");
        coordinator.performBackup();
        this.lastActionTime = LocalDateTime.now().toString();
        logger.info("Auto Save angestoßen.");
    }

    public void stopAutoSave() {
        this.isActive = false;
    }
    
    // Kompatibilitätsmethoden für bestehenden Code
    public void startAutoSave(boolean activate) {
        start(activate);
    }
    
    public void scheduleNextSave() {
        scheduleNext();
    }
    
    public String getLastSaveTime() {
        return getLastActionTimeFormatted();
    }
    
    public String getNextSaveFormatted() {
        return getNextActionFormatted();
    }
}
