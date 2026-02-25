package me.justsid.mcservergui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledCommandManager {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledCommandManager.class);
    private static final long CHECK_INTERVAL_SECONDS = 30;
    private static final String CONFIG_FILE = "scheduled_commands.json";

    private final ServerProcessManager serverProcessManager;
    private final ConfigManager configManager;
    private final Set<ScheduledCommand> commands = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastExecutionTimes = new ConcurrentHashMap<>();
    private final Set<String> executedThisCycle = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scheduler;

    public ScheduledCommandManager(ServerProcessManager serverProcessManager, ConfigManager configManager) {
        this.serverProcessManager = serverProcessManager;
        this.configManager = configManager;
        loadCommands();
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScheduledCommandManager");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::checkAndExecute, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.info("ScheduledCommandManager gestartet.");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        saveCommands();
        logger.info("ScheduledCommandManager gestoppt.");
    }

    private void checkAndExecute() {
        if (!serverProcessManager.isRunning()) {
            executedThisCycle.clear();
            return;
        }

        int currentPlayerCount = serverProcessManager.getCurrentPlayerCount();
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        for (ScheduledCommand cmd : commands) {
            if (!cmd.isEnabled() || !serverProcessManager.isRunning()) {
                continue;
            }

            boolean shouldExecute = cmd.shouldExecute(currentPlayerCount, currentTime);
            boolean wasExecuted = executedThisCycle.contains(cmd.getId());
            
            if (shouldExecute && !wasExecuted) {
                executeCommand(cmd);
                executedThisCycle.add(cmd.getId());
                
                if (!cmd.isRepeat()) {
                    cmd.setEnabled(false);
                }
            }
            
            if (!shouldExecute && cmd.getTriggerType() != ScheduledCommand.TriggerType.EVERY_X_MINUTES) {
                executedThisCycle.remove(cmd.getId());
            }
        }
    }

    private void executeCommand(ScheduledCommand cmd) {
        if (serverProcessManager.isRunning()) {
            logger.info("Führe geplanten Befehl aus: {}", cmd.getCommand());
            serverProcessManager.sendCommand(cmd.getCommand());
        }
    }

    public void addCommand(ScheduledCommand command) {
        commands.add(command);
        saveCommands();
    }

    public void removeCommand(String id) {
        commands.removeIf(cmd -> cmd.getId().equals(id));
        saveCommands();
    }

    public void updateCommand(ScheduledCommand command) {
        commands.removeIf(cmd -> cmd.getId().equals(command.getId()));
        commands.add(command);
        saveCommands();
    }

    public Set<ScheduledCommand> getCommands() {
        return new HashSet<>(commands);
    }

    public void saveCommands() {
        try {
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            
            boolean first = true;
            for (ScheduledCommand cmd : commands) {
                if (!first) json.append(",\n");
                first = false;
                
                json.append("  {\n");
                json.append("    \"id\": \"").append(escapeJson(cmd.getId())).append("\",\n");
                json.append("    \"command\": \"").append(escapeJson(cmd.getCommand())).append("\",\n");
                json.append("    \"triggerType\": \"").append(cmd.getTriggerType().name()).append("\",\n");
                json.append("    \"playerCountCondition\": ").append(cmd.getPlayerCountCondition()).append(",\n");
                json.append("    \"timeCondition\": \"").append(escapeJson(cmd.getTimeCondition() != null ? cmd.getTimeCondition() : "")).append("\",\n");
                json.append("    \"repeat\": ").append(cmd.isRepeat()).append(",\n");
                json.append("    \"enabled\": ").append(cmd.isEnabled()).append("\n");
                json.append("  }");
            }
            
            json.append("\n]");
            
            Files.writeString(Paths.get(CONFIG_FILE), json.toString());
            logger.info("Geplante Befehle gespeichert.");
        } catch (Exception e) {
            logger.error("Fehler beim Speichern der geplanten Befehle: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void loadCommands() {
        try {
            Path path = Paths.get(CONFIG_FILE);
            if (!Files.exists(path)) {
                return;
            }

            String content = Files.readString(path);
            if (content.trim().isEmpty() || content.trim().equals("[]")) {
                return;
            }

            String[] entries = content.split("\\},\\s*\\{");
            for (String entry : entries) {
                entry = entry.trim();
                if (entry.startsWith("[")) entry = entry.substring(1);
                if (entry.endsWith("]")) entry = entry.substring(0, entry.length() - 1);
                if (!entry.startsWith("{")) entry = "{" + entry;
                if (!entry.endsWith("}")) entry = entry + "}";
                
                try {
                    ScheduledCommand cmd = parseCommand(entry);
                    if (cmd != null) {
                        commands.add(cmd);
                    }
                } catch (Exception e) {
                    logger.warn("Fehler beim Parsen eines Commands: {}", e.getMessage());
                }
            }
            
            logger.info("Geplante Befehle geladen: {}", commands.size());
        } catch (Exception e) {
            logger.error("Fehler beim Laden der geplanten Befehle: {}", e.getMessage());
        }
    }

    private ScheduledCommand parseCommand(String json) {
        ScheduledCommand cmd = new ScheduledCommand();
        
        cmd.setId(extractString(json, "id"));
        cmd.setCommand(extractString(json, "command"));
        
        String triggerType = extractString(json, "triggerType");
        if (triggerType != null) {
            cmd.setTriggerType(ScheduledCommand.TriggerType.valueOf(triggerType));
        }
        
        cmd.setPlayerCountCondition(extractInt(json, "playerCountCondition"));
        cmd.setTimeCondition(extractString(json, "timeCondition"));
        cmd.setRepeat(extractBoolean(json, "repeat"));
        cmd.setEnabled(extractBoolean(json, "enabled"));
        
        return cmd;
    }

    private String extractString(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return null;
        start = json.indexOf(":", start);
        if (start == -1) return null;
        start = json.indexOf("\"", start);
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private int extractInt(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return 0;
        start = json.indexOf(":", start);
        if (start == -1) return 0;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return 0;
        try {
            return Integer.parseInt(json.substring(start + 1, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean extractBoolean(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start == -1) return false;
        start = json.indexOf(":", start);
        if (start == -1) return false;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return false;
        String value = json.substring(start + 1, end).trim();
        return value.equals("true");
    }
}
