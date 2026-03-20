package me.justsid.mcservergui;

import static me.justsid.mcservergui.Constants.*;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Verwaltet den Minecraft-Server-Prozess.
 * Startet, stoppt und überwacht den Server.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public class ServerProcessManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerProcessManager.class);
    private Process serverProcess;
    private PrintWriter serverInput;
    private final ConfigManager configManager;
    private final AtomicReference<ServerState> currentState = new AtomicReference<>(ServerState.OFFLINE);
    private Consumer<Integer> playerCountConsumer;
    private Consumer<String> logLineConsumer;
    private int currentPlayerCount = 0;

    /**
     * Erstellt einen neuen ServerProcessManager.
     * 
     * @param configManager Der ConfigManager für Konfigurationswerte.
     */
    public ServerProcessManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setConsoleOutput(TextArea consoleOutput) {
        // Console rendering is handled in Controller via logLineConsumer.
    }

    /**
     * Setzt einen Consumer der bei Spielerzahl-Änderungen aufgerufen wird.
     * 
     * @param consumer Der Consumer der die Spielerzahl erhält.
     */
    public void setPlayerCountConsumer(Consumer<Integer> consumer) {
        this.playerCountConsumer = consumer;
    }
    
    public void setLogLineConsumer(Consumer<String> consumer) {
        this.logLineConsumer = consumer;
    }
    
    /**
     * Gibt die aktuelle Spielerzahl zurück.
     * 
     * @return Die Anzahl der Spieler.
     */
    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }
    
    /**
     * Parst die Spielerzahl aus einer Server-Log-Zeile.
     * 
     * @param line Die Log-Zeile.
     */
    private void parsePlayerCount(String line) {
        // "There are X of a max of Y players online"
        if (line.contains("players online")) {
            try {
                String[] parts = line.split("are ");
                if (parts.length > 1) {
                    String[] split2 = parts[1].split(" of ");
                    if (split2.length > 0) {
                        int count = Integer.parseInt(split2[0].trim());
                        if (count != currentPlayerCount) {
                            currentPlayerCount = count;
                            if (playerCountConsumer != null) {
                                Platform.runLater(() -> playerCountConsumer.accept(count));
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse player count from: {}", line);
            }
        }
        
        // Spieler joined
        if (line.contains("joined the game")) {
            currentPlayerCount++;
            if (playerCountConsumer != null) {
                Platform.runLater(() -> playerCountConsumer.accept(currentPlayerCount));
            }
        }
        
        // Spieler left
        if (line.contains("left the game")) {
            currentPlayerCount = Math.max(0, currentPlayerCount - 1);
            if (playerCountConsumer != null) {
                Platform.runLater(() -> playerCountConsumer.accept(currentPlayerCount));
            }
        }
    }

    public ServerState getCurrentState() {
        return currentState.get();
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    public void sendCommand(String command) {
        if (serverProcess != null && serverInput != null) {
            try {
                serverInput.println(command);
                logger.debug("Command sent."); // Don't log command content to avoid sensitive data in logs
            } catch (Exception e) {
                appendToConsole("Fehler beim Senden des Befehls: " + e.getMessage());
                logger.error("Failed to send command: {}", command, e);
            }
        } else {
            appendToConsole("Kein laufender Serverprozess.");
        }
    }

    public boolean startServerInternal() {
        if (serverProcess != null && serverProcess.isAlive()) {
            appendToConsole("Server läuft bereits.");
            return true;
        }

        if (!validateConfig()) {
            return false;
        }

        Path serverDirectory = Paths.get(configManager.getServerDirectory());
        EulaManager eulaManager = new EulaManager();
        if (!eulaManager.isAccepted(serverDirectory)) {
            showEulaPopup(serverDirectory, eulaManager);
            return false;
        }

        try {
            // Liste verwenden um Leerzeichen im Pfad zu unterstützen
            List<String> command = new ArrayList<>();
            command.add(configManager.getConfigValue("javaPath"));
            command.add("-Xmx" + configManager.getConfigValue("maxMemory"));
            command.add("-Xms" + configManager.getConfigValue("minMemory"));
            command.add("-jar");
            command.add(configManager.getConfigValue("serverJar"));
            command.add("nogui");
            
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.directory(serverDirectory.toFile());
            serverProcess = builder.start();
            serverInput = new PrintWriter(serverProcess.getOutputStream(), true);
            currentState.set(ServerState.STARTING);
            currentPlayerCount = 0;

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendToConsole(line);
                        parsePlayerCount(line);
                        // Auf "Done" warten für State-Übergang zu ONLINE
                        if (line.contains("Done") && line.contains("For help, type")) {
                            currentState.set(ServerState.ONLINE);
                        }
                    }
                } catch (IOException e) {
                    appendToConsole("Fehler beim Lesen der Server-Ausgabe.");
                    logger.error("Error reading server output: ", e);
                }
                currentState.set(ServerState.OFFLINE);
            }).start();

            appendToConsole("Server gestartet.");
            return true;
        } catch (IOException e) {
            appendToConsole("Fehler beim Starten des Servers: " + e.getMessage());
            logger.error("Error starting server: ", e);
            currentState.set(ServerState.ERROR);
            return false;
        }
    }

    public boolean stopServerInternal() {
        if (serverProcess != null) {
            if (serverInput != null) {
                serverInput.println("stop");
            }
            try {
                boolean exited = serverProcess.waitFor(SERVER_STOP_TIMEOUT_MS / 1000, TimeUnit.SECONDS);
                if (!exited) {
                    logger.warn("Server stop timeout - force killing");
                    serverProcess.destroyForcibly();
                    serverProcess.waitFor(5, TimeUnit.SECONDS);
                }
                serverProcess.destroy();
                serverProcess = null;
                if (serverInput != null) {
                    serverInput.close();
                }
                serverInput = null;
                currentState.set(ServerState.OFFLINE);
                currentPlayerCount = 0;
                if (playerCountConsumer != null) {
                    Platform.runLater(() -> playerCountConsumer.accept(0));
                }
                appendToConsole("Server gestoppt.");
                return true;
            } catch (InterruptedException e) {
                logger.error("Error stopping server: ", e);
                Thread.currentThread().interrupt();
                currentState.set(ServerState.ERROR);
                return false;
            }
        } else {
            appendToConsole("Kein Server läuft.");
            return true;
        }
    }

    public boolean waitForServerFullyStopped(int maxWaitSeconds) {
        if (serverProcess != null && serverProcess.isAlive()) {
            logger.warn("Server process still alive, waiting...");
            try {
                return serverProcess.waitFor(maxWaitSeconds, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    public boolean isProcessAlive() {
        return serverProcess != null && serverProcess.isAlive();
    }

    private void showEulaPopup(Path serverDirectory, EulaManager eulaManager) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("EULA Zustimmung erforderlich");
        alert.setHeaderText("Sie müssen der Minecraft EULA zustimmen, um den Server zu starten.");
        alert.setContentText("Möchten Sie der EULA zustimmen?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                eulaManager.accept(serverDirectory);
                appendToConsole("EULA akzeptiert.");
            } catch (IOException e) {
                appendToConsole("Fehler beim Schreiben der EULA-Datei.");
                logger.error("Error writing EULA file: ", e);
            }
        } else {
            appendToConsole("EULA wurde nicht akzeptiert. Der Server kann nicht gestartet werden.");
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("EULA Fehler");
            errorAlert.setHeaderText("Server konnte nicht gestartet werden");
            errorAlert.setContentText("Sie müssen der EULA zustimmen, um den Server zu starten.");
            errorAlert.showAndWait();
        }
    }

    private boolean validateConfig() {
        String javaPath = configManager.getConfigValue("javaPath");
        String serverJar = configManager.getConfigValue("serverJar");

        if (javaPath == null || javaPath.isEmpty()) {
            appendToConsole("Fehler: Java-Pfad nicht gesetzt.");
            return false;
        }

        // Security: ensure javaPath points to a regular file, not a symlink or directory
        Path javaExe = Paths.get(javaPath);
        if (!Files.exists(javaExe) && !javaPath.equals("java")) {
            appendToConsole("Fehler: Java-Executable nicht gefunden: " + javaPath);
            return false;
        }
        if (Files.exists(javaExe) && (Files.isSymbolicLink(javaExe) || !Files.isRegularFile(javaExe))) {
            appendToConsole("Fehler: Java-Pfad ist kein normales Executable: " + javaPath);
            logger.error("Sicherheitswarnung: javaPath '{}' ist kein regul\u00e4res Executable.", javaPath);
            return false;
        }

        if (serverJar == null || serverJar.isEmpty()) {
            appendToConsole("Fehler: Server-JAR nicht gesetzt.");
            return false;
        }

        File jarFile = new File(serverJar);
        if (!jarFile.exists()) {
            appendToConsole("Fehler: Server-JAR nicht gefunden: " + serverJar);
            return false;
        }

        // Security: ensure serverJar is a regular file
        if (!jarFile.isFile() || Files.isSymbolicLink(jarFile.toPath())) {
            appendToConsole("Fehler: Server-JAR ist kein normales Archiv: " + serverJar);
            logger.error("Sicherheitswarnung: serverJar '{}' ist kein regul\u00e4res Archiv.", serverJar);
            return false;
        }

        return true;
    }

    private void appendToConsole(String text) {
        if (logLineConsumer != null) {
            logLineConsumer.accept(text);
        }
    }
}
