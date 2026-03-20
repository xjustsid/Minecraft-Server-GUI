package me.justsid.mcservergui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstrakte Basisklasse für zeitbasierte Aktionen wie Auto-Save und Auto-Restart.
 * Verwaltet den Timer und die Zeitplanung.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public abstract class AbstractScheduledManager {

    protected final ConfigManager configManager;
    protected final ServerOperationCoordinator coordinator;
    protected final Logger logger;
    
    protected Timer timer;
    protected LocalDateTime nextScheduledTime;
    protected String lastActionTime;
    protected boolean isActive;
    protected final String configKey;
    protected final String actionName;

    protected AbstractScheduledManager(ConfigManager configManager, 
                                     ServerOperationCoordinator coordinator,
                                     String configKey,
                                     String actionName,
                                     Class<?> loggerClass) {
        this.configManager = configManager;
        this.coordinator = coordinator;
        this.configKey = configKey;
        this.actionName = actionName;
        this.logger = LoggerFactory.getLogger(loggerClass);
    }

    public void start(boolean activate) {
        if (isActive == activate) {
            logger.info("Auto {} ist bereits im gewünschten Zustand: {}", actionName, activate);
            return;
        }

        isActive = activate;

        if (activate) {
            scheduleNext();
            logger.info("Auto {} aktiviert.", actionName);
        } else {
            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
            logger.info("Auto {} deaktiviert.", actionName);
        }
    }

    public void scheduleNext() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }

        nextScheduledTime = getNextScheduledTime();
        long delay = Duration.between(LocalDateTime.now(), nextScheduledTime).toMillis();

        logger.debug("Auto {} Timer geplant mit Verzögerung: {} ms (Nächste Zeit: {})", 
            actionName, delay, nextScheduledTime);

        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                performAction();
                scheduleNext();
            }
        }, delay);
        notifyControllerUpdate();
    }

    protected void notifyControllerUpdate() {
        Controller mainController = Main.getPrimaryController();
        if (mainController != null) {
            mainController.updateLabels();
        }
    }

    protected LocalDateTime getNextScheduledTime() {
        String times = configManager.getConfigValue(configKey, "00:00,06:00,12:00,18:00");
        return TimeUtils.getNextTime(times.split(","), LocalDateTime.now());
    }

    public String getLastActionTimeFormatted() {
        return lastActionTime != null ? lastActionTime : "-";
    }

    public String getNextActionFormatted() {
        if (nextScheduledTime != null) {
            return TimeUtils.formatDuration(
                Duration.between(LocalDateTime.now(), nextScheduledTime).getSeconds());
        }
        return "-";
    }

    public void testTimeCalculations() {
        logger.debug("Testlauf der Zeitberechnung für {} gestartet...", actionName);
        String times = configManager.getConfigValue(configKey, "00:00,06:00,12:00,18:00");
        logger.debug("Konfigurierte Zeiten für {}: {}", actionName, times);
        LocalDateTime nextTime = getNextScheduledTime();
        logger.debug("Berechnete nächste Zeit: {}", nextTime);
        Duration untilNextTime = Duration.between(LocalDateTime.now(), nextTime);
        logger.debug("Zeit bis zum nächsten Ereignis (Millisekunden): {}", untilNextTime.toMillis());
    }

    protected abstract void performAction();
}
