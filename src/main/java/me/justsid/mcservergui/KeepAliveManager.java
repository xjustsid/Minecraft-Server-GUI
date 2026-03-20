package me.justsid.mcservergui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeepAliveManager {
    private static final Logger logger = LoggerFactory.getLogger(KeepAliveManager.class);
    private static final long CHECK_INTERVAL_MS = 5000;

    private final ServerProcessManager serverProcessManager;
    private final ServerOperationCoordinator coordinator;
    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private Timer keepAliveTimer;

    public KeepAliveManager(ServerProcessManager serverProcessManager, 
                           ServerOperationCoordinator coordinator,
                           ConfigManager configManager) {
        this.serverProcessManager = serverProcessManager;
        this.coordinator = coordinator;
    }

    public void start() {
        if (isEnabled.get()) {
            return;
        }
        
        isEnabled.set(true);
        keepAliveTimer = new Timer("KeepAliveTimer", true);
        keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkServerStatus();
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);
        
        logger.info("Keep Alive gestartet.");
    }

    public void stop() {
        isEnabled.set(false);
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
        logger.info("Keep Alive gestoppt.");
    }

    private void checkServerStatus() {
        if (!isEnabled.get()) {
            return;
        }

        ServerState currentState = serverProcessManager.getCurrentState();
        
        if (currentState == ServerState.ONLINE && !serverProcessManager.isProcessAlive()) {
            logger.warn("Server-Prozess nicht mehr aktiv! Starte automatischen Neustart...");
            coordinator.startServer();
        }
    }

    public boolean isEnabled() {
        return isEnabled.get();
    }
}
