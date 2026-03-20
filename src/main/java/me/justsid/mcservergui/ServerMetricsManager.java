package me.justsid.mcservergui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parst TPS-, RAM- und Spieler-Metriken aus dem Server-Log-Stream.
 * Misst ausdrücklich NICHT den RAM oder die CPU der GUI-JVM.
 */
public class ServerMetricsManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerMetricsManager.class);

    // Paper/Spigot/Purpur (moderner Standard)
    private static final Pattern TPS_PATTERN_MODERN =
            Pattern.compile("TPS from last 1m, 5m, 15m: ([\\d.]+), ([\\d.]+), ([\\d.]+)");

    // Ältere Bukkit-Server
    private static final Pattern TPS_PATTERN_LEGACY =
            Pattern.compile("Current TPS from last 1m, 5m, 15m: ([\\d.]+), ([\\d.]+), ([\\d.]+)");

    // Player-Join / -Leave / -List aus Standard-Server-Output
    private static final Pattern PLAYER_JOIN_PATTERN =
            Pattern.compile("([\\w]+) joined the game");
    private static final Pattern PLAYER_LEAVE_PATTERN =
            Pattern.compile("([\\w]+) left the game");
    private static final Pattern PLAYER_LIST_PATTERN =
            Pattern.compile("There are (\\d+) of a max of (\\d+) players online:(.*)");

    // Optional: RAM wenn ein Plugin/der Server Speichernutzung ins Log schreibt
    private static final Pattern RAM_PATTERN =
            Pattern.compile("(?:Memory|RAM).*?([0-9]+) ?MB", Pattern.CASE_INSENSITIVE);

    public enum MetricsSource { LOG_PARSE, UNAVAILABLE }

    private double currentTPS = 0.0;
    private MetricsSource tpsSource = MetricsSource.UNAVAILABLE;
    private String currentRamDisplay = "n/a";
    private int onlinePlayerCount = 0;
    private int maxPlayers = 20;
    private long serverStartTime = System.currentTimeMillis();
    private final Set<String> currentlyOnlinePlayers = new HashSet<>();

    private final List<Consumer<List<String>>> playerListListeners = new CopyOnWriteArrayList<>();

    public void parseTpsFromLog(String logLine) {
        Matcher m = TPS_PATTERN_MODERN.matcher(logLine);
        if (m.find()) {
            currentTPS = Double.parseDouble(m.group(1));
            tpsSource = MetricsSource.LOG_PARSE;
            logger.debug("TPS geparst (MODERN): 1m={}", currentTPS);
            return;
        }
        m = TPS_PATTERN_LEGACY.matcher(logLine);
        if (m.find()) {
            currentTPS = Double.parseDouble(m.group(1));
            tpsSource = MetricsSource.LOG_PARSE;
            logger.debug("TPS geparst (LEGACY): {}", currentTPS);
        }
    }

    public void parsePlayersFromLog(String logLine) {
        Matcher listMatcher = PLAYER_LIST_PATTERN.matcher(logLine);
        if (listMatcher.find()) {
            onlinePlayerCount = Integer.parseInt(listMatcher.group(1));
            maxPlayers = Integer.parseInt(listMatcher.group(2));
            String names = listMatcher.group(3).trim();
            currentlyOnlinePlayers.clear();
            if (!names.isBlank()) {
                for (String name : names.split(", ")) {
                    String trimmed = name.trim();
                    if (!trimmed.isBlank()) {
                        currentlyOnlinePlayers.add(trimmed);
                    }
                }
            }
            firePlayerListListeners();
            return;
        }

        Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(logLine);
        if (joinMatcher.find()) {
            currentlyOnlinePlayers.add(joinMatcher.group(1));
            onlinePlayerCount = currentlyOnlinePlayers.size();
            firePlayerListListeners();
            return;
        }

        Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(logLine);
        if (leaveMatcher.find()) {
            currentlyOnlinePlayers.remove(leaveMatcher.group(1));
            onlinePlayerCount = currentlyOnlinePlayers.size();
            firePlayerListListeners();
        }
    }

    public void parseRamFromLog(String logLine) {
        Matcher m = RAM_PATTERN.matcher(logLine);
        if (m.find()) {
            currentRamDisplay = m.group(1) + " MB";
        }
    }

    public void addPlayerListListener(Consumer<List<String>> listener) {
        playerListListeners.add(listener);
    }

    private void firePlayerListListeners() {
        List<String> snapshot = new ArrayList<>(currentlyOnlinePlayers);
        for (Consumer<List<String>> listener : playerListListeners) {
            try {
                listener.accept(snapshot);
            } catch (Exception e) {
                logger.debug("Exception in player-list listener", e);
            }
        }
    }

    public double getTPS() { return currentTPS; }
    public MetricsSource getTpsSource() { return tpsSource; }

    /**
     * Gibt den RAM-Verbrauch des Servers zurück.
     * Wichtig: Misst NICHT den RAM der GUI-JVM.
     * Gibt "n/a" zurück wenn kein parsbarer Log-Wert vorhanden ist.
     */
    public String getRAMUsageFormatted() {
        return currentRamDisplay;
    }

    /** Setzt den konfigurierten Max-RAM-Wert als Fallback-Anzeige (z.B. "max 4G"). */
    public void setConfiguredMaxRam(String maxRam) {
        if (currentRamDisplay.equals("n/a") && maxRam != null && !maxRam.isBlank()) {
            currentRamDisplay = "max " + maxRam;
        }
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - serverStartTime) / 1000;
    }

    public int getOnlinePlayerCount() { return onlinePlayerCount; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public List<String> getOnlinePlayers() { return new ArrayList<>(currentlyOnlinePlayers); }

    public void reset() {
        serverStartTime = System.currentTimeMillis();
        currentTPS = 0.0;
        tpsSource = MetricsSource.UNAVAILABLE;
        currentRamDisplay = "n/a";
        onlinePlayerCount = 0;
        currentlyOnlinePlayers.clear();
        logger.info("ServerMetricsManager zurückgesetzt");
    }
}
