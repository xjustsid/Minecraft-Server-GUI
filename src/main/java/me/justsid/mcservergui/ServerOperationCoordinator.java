package me.justsid.mcservergui;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Koordiniert alle Server-Operationen und verhindert gleichzeitige Aktionen.
 * Stellt sicher, dass nur eine Operation zur Zeit ausgeführt wird.
 * 
 */
public class ServerOperationCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(ServerOperationCoordinator.class);

    private final ServerProcessManager serverProcessManager;
    private final BackupManager backupManager;
    private final ConfigManager configManager;
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    private final AtomicReference<OperationType> currentOperation = new AtomicReference<>(null);
    private ServerStateListener stateListener;

    /**
     * Enum für die verschiedenen Operationstypen.
     */
    public enum OperationType {
        START, STOP, RESTART, BACKUP, SAVE_BACKUP
    }

    /**
     * Interface für State-Listener die bei Zustandsänderungen benachrichtigt werden.
     */
    public interface ServerStateListener {
        /**
         * Wird aufgerufen wenn sich der Serverzustand ändert.
         * 
         * @param newState Der neue Zustand.
         */
        void onStateChanged(ServerState newState);
    }

    /**
     * Erstellt einen neuen ServerOperationCoordinator.
     * 
     * @param serverProcessManager Der ServerProcessManager.
     * @param backupManager Der BackupManager.
     * @param configManager Der ConfigManager.
     */
    public ServerOperationCoordinator(ServerProcessManager serverProcessManager, BackupManager backupManager, ConfigManager configManager) {
        this.serverProcessManager = serverProcessManager;
        this.backupManager = backupManager;
        this.configManager = configManager;
    }

    /**
     * Setzt den State-Listener.
     * 
     * @param listener Der Listener.
     */
    public void setStateListener(ServerStateListener listener) {
        this.stateListener = listener;
    }

    private void setState(ServerState state) {
        if (stateListener != null) {
            Platform.runLater(() -> stateListener.onStateChanged(state));
        }
    }

    public boolean isOperationInProgress() {
        return operationInProgress.get();
    }

    public OperationType getCurrentOperation() {
        return currentOperation.get();
    }

    public boolean canStartOperation(OperationType type) {
        if (operationInProgress.get()) {
            logger.warn("Cannot start {} - operation {} already in progress", type, currentOperation.get());
            return false;
        }
        return true;
    }

    private static final int WAIT_AFTER_STOP_SECONDS = 3;

    public synchronized void startServer() {
        if (!canStartOperation(OperationType.START)) return;
        
        operationInProgress.set(true);
        currentOperation.set(OperationType.START);
        setState(ServerState.STARTING);

        new Thread(() -> {
            try {
                boolean success = serverProcessManager.startServerInternal();
                if (success) {
                    setState(ServerState.ONLINE);
                } else {
                    setState(ServerState.ERROR);
                }
            } finally {
                operationInProgress.set(false);
                currentOperation.set(null);
            }
        }).start();
    }

    public synchronized void stopServer() {
        if (!canStartOperation(OperationType.STOP)) return;

        operationInProgress.set(true);
        currentOperation.set(OperationType.STOP);
        setState(ServerState.STOPPING);

        new Thread(() -> {
            try {
                serverProcessManager.stopServerInternal();
                setState(ServerState.OFFLINE);
            } finally {
                operationInProgress.set(false);
                currentOperation.set(null);
            }
        }).start();
    }

    public synchronized void performBackup() {
        if (!canStartOperation(OperationType.BACKUP)) return;

        boolean wasOnline = serverProcessManager.getCurrentState() == ServerState.ONLINE;

        operationInProgress.set(true);
        currentOperation.set(OperationType.BACKUP);
        setState(ServerState.BACKUP);

        new Thread(() -> {
            try {
                if (wasOnline) {
                    logger.info("Stopping server for offline backup...");
                    serverProcessManager.stopServerInternal();
                    
                    waitForServerFullyStopped();
                }

                logger.info("Creating backup...");
                backupManager.triggerBackup();

                if (wasOnline) {
                    logger.info("Starting server after backup...");
                    setState(ServerState.STARTING);
                    boolean success = serverProcessManager.startServerInternal();
                    setState(success ? ServerState.ONLINE : ServerState.ERROR);
                } else {
                    setState(ServerState.OFFLINE);
                }
            } catch (Exception e) {
                logger.error("Backup failed: {}", e.getMessage(), e);
                setState(ServerState.ERROR);
            } finally {
                operationInProgress.set(false);
                currentOperation.set(null);
            }
        }).start();
    }

    public synchronized void performRestart(boolean withBackup) {
        if (!canStartOperation(OperationType.RESTART)) return;

        operationInProgress.set(true);
        currentOperation.set(OperationType.RESTART);
        setState(ServerState.RESTARTING);

        new Thread(() -> {
            try {
                boolean wasOnline = serverProcessManager.getCurrentState() == ServerState.ONLINE;

                if (wasOnline) {
                    logger.info("Stopping server for restart...");
                    setState(ServerState.STOPPING);
                    serverProcessManager.stopServerInternal();
                }

                if (withBackup) {
                    boolean backupEnabled = Boolean.parseBoolean(configManager.getConfigValue("backupBeforeRestart", "true"));
                    if (backupEnabled) {
                        waitForServerFullyStopped();
                        
                        logger.info("Creating backup before restart...");
                        setState(ServerState.BACKUP);
                        backupManager.triggerBackup();
                        setState(ServerState.RESTARTING);
                    }
                }

                logger.info("Starting server after restart...");
                setState(ServerState.STARTING);
                boolean success = serverProcessManager.startServerInternal();
                setState(success ? ServerState.ONLINE : ServerState.ERROR);

            } catch (Exception e) {
                logger.error("Restart failed: {}", e.getMessage(), e);
                setState(ServerState.ERROR);
            } finally {
                operationInProgress.set(false);
                currentOperation.set(null);
            }
        }).start();
    }

    public synchronized void performAutoSaveAndRestart() {
        if (!canStartOperation(OperationType.RESTART)) return;

        operationInProgress.set(true);
        currentOperation.set(OperationType.RESTART);
        setState(ServerState.RESTARTING);

        new Thread(() -> {
            try {
                boolean wasOnline = serverProcessManager.getCurrentState() == ServerState.ONLINE;

                if (wasOnline) {
                    logger.info("Stopping server for scheduled restart with backup...");
                    setState(ServerState.STOPPING);
                    serverProcessManager.stopServerInternal();
                }

                waitForServerFullyStopped();

                logger.info("Creating scheduled backup...");
                setState(ServerState.BACKUP);
                backupManager.triggerBackup();

                logger.info("Starting server after scheduled restart...");
                setState(ServerState.STARTING);
                boolean success = serverProcessManager.startServerInternal();
                setState(success ? ServerState.ONLINE : ServerState.ERROR);

            } catch (Exception e) {
                logger.error("Scheduled restart with backup failed: {}", e.getMessage(), e);
                setState(ServerState.ERROR);
            } finally {
                operationInProgress.set(false);
                currentOperation.set(null);
            }
        }).start();
    }

    public ServerState getCurrentServerState() {
        return serverProcessManager.getCurrentState();
    }

    private void waitForServerFullyStopped() {
        logger.info("Waiting for server to fully stop and release files...");
        
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            if (!serverProcessManager.isProcessAlive()) {
                logger.info("Server process confirmed stopped.");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        try {
            logger.info("Waiting {} seconds for file handles to be released...", WAIT_AFTER_STOP_SECONDS);
            Thread.sleep(WAIT_AFTER_STOP_SECONDS * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("Server fully stopped, ready for backup.");
    }
}
