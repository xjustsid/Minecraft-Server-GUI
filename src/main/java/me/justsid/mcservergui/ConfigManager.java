package me.justsid.mcservergui;

import static me.justsid.mcservergui.Constants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private final String configFilePath = CONFIG_FILE;
    private final Map<String, String> config = new HashMap<>();
    private final Map<String, String> comments = new HashMap<>();
    private final boolean firstRun;

    /**
     * Konstruktor: Lädt die Konfiguration und erstellt Standardwerte beim ersten Start.
     */
    public ConfigManager() {
        this.firstRun = !checkIfConfigExists();
        initializeComments(); // Kommentare für alle Konfigurationsschlüssel setzen
        if (firstRun) {
            logger.info("Erste Ausführung erkannt. Erstelle Standardkonfiguration...");
            createDefaultConfig();
            saveConfig();
        }
        loadConfig();
    }

    /**
     * Prüft, ob die Konfigurationsdatei existiert.
     *
     * @return true, wenn die Datei existiert.
     */
    private boolean checkIfConfigExists() {
        File configFile = new File(configFilePath);
        return configFile.exists();
    }

    /**
     * Gibt zurück, ob es sich um die erste Ausführung handelt.
     * Prüft ob wichtige Konfigurationswerte fehlen.
     *
     * @return true, wenn wichtige Konfigurationswerte fehlen.
     */
    public boolean isFirstRun() {
        // Prüfen ob wichtige Konfigurationswerte fehlen
        String serverJar = config.get(KEY_SERVER_JAR);
        String maxMemory = config.get(KEY_MAX_MEMORY);
        
        // Wenn wichtige Werte fehlen, gilt es als erster Start
        return serverJar == null || serverJar.isEmpty() ||
               maxMemory == null || maxMemory.isEmpty();
    }

    /**
     * Lädt die Konfiguration aus der Datei.
     */
    public void loadConfig() {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue; // Kommentare und leere Zeilen überspringen
                    }
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        config.put(parts[0].trim(), parts[1].trim());
                    }
                }
                logger.info("Konfiguration erfolgreich geladen.");
            } catch (IOException e) {
                logger.error("Fehler beim Laden der Konfiguration.", e);
            }
        } else {
            logger.warn("Konfigurationsdatei nicht gefunden. Standardwerte werden verwendet.");
        }
        validateConfig();
        logger.debug("Geladene Restart-Zeiten: {}", config.get("restartTimes"));
    }

    /**
     * Erstellt eine Standardkonfiguration.
     */
    private void createDefaultConfig() {
        config.put(KEY_JAVA_PATH, "java");
        config.put(KEY_SERVER_JAR, "server.jar");
        config.put(KEY_MIN_MEMORY, "1024M");
        config.put(KEY_MAX_MEMORY, "2048M");
        config.put(KEY_AUTO_RESTART, "false");
        config.put(KEY_RESTART_TIMES, DEFAULT_RESTART_TIMES);
        config.put(KEY_AUTO_SAVE, "false");
        config.put(KEY_BACKUP_FOLDER, "./backups");
        config.put("backupBeforeRestart", "true");
    }

    /**
     * Initialisiert Kommentare für die Konfigurationswerte.
     */
    private void initializeComments() {
        comments.put(KEY_JAVA_PATH, "Pfad zur Java-Installation. Standard: 'java'");
        comments.put(KEY_SERVER_JAR, "Name der Server-JAR-Datei. Beispiel: 'server.jar'");
        comments.put(KEY_MIN_MEMORY, "Minimale Menge an zugewiesenem Speicher. Beispiel: '1024M' oder '2G'");
        comments.put(KEY_MAX_MEMORY, "Maximale Menge an zugewiesenem Speicher. Beispiel: '2048M' oder '4G'");
        comments.put(KEY_AUTO_RESTART, "Automatische Neustarts aktivieren. 'true' oder 'false'");
        comments.put(KEY_RESTART_TIMES, "Zeiten für geplante Neustarts, getrennt durch Kommata. Beispiel: '00:00,06:00'");
        comments.put(KEY_AUTO_SAVE, "Automatische Backups aktivieren. 'true' oder 'false'");
        comments.put(KEY_BACKUP_FOLDER, "Pfad zum Ordner, in dem Backups gespeichert werden.");
        comments.put("backupBeforeRestart", "Backup vor jedem Neustart erzwingen. 'true' oder 'false'");
    }

    /**
     * Validiert die Konfiguration und ergänzt fehlende Standardwerte.
     */
    private void validateConfig() {
        boolean updated = false;

        for (String key : comments.keySet()) {
            if (!config.containsKey(key)) {
                config.put(key, getDefaultConfigValue(key));
                updated = true;
            }
        }

        if (!isMemoryValueValid(config.get("minMemory"))) {
            config.put("minMemory", "1024M");
            updated = true;
        }

        if (!isMemoryValueValid(config.get("maxMemory"))) {
            config.put("maxMemory", "2048M");
            updated = true;
        }

        if (updated) {
            logger.info("Fehlende oder ungültige Konfigurationswerte erkannt und korrigiert.");
            saveConfig();
        }
    }

    /**
     * Überprüft, ob ein Speicherwert gültig ist (z. B. '1024M' oder '2G').
     *
     * @param value Der zu prüfende Speicherwert.
     * @return true, wenn der Wert gültig ist.
     */
    private boolean isMemoryValueValid(String value) {
        return value != null && value.matches("\\d+[MG]");
    }

    /**
     * Speichert die aktuelle Konfiguration in die Datei.
     */
    public void saveConfig() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFilePath))) {
            writer.println("# Minecraft Server Configuration");
            writer.println("# Diese Datei wurde automatisch generiert.");
            for (Map.Entry<String, String> entry : config.entrySet()) {
                if (comments.containsKey(entry.getKey())) {
                    writer.println("# " + comments.get(entry.getKey())); // Kommentar hinzufügen
                }
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
            logger.info("Konfiguration erfolgreich gespeichert.");
        } catch (IOException e) {
            logger.error("Fehler beim Speichern der Konfiguration.", e);
        }
    }

    /**
     * Gibt den Wert einer bestimmten Konfiguration zurück.
     *
     * @param key Der Schlüssel der Konfiguration.
     * @return Der Wert oder null, falls der Schlüssel nicht existiert.
     */
    public String getConfigValue(String key) {
        return config.get(key);
    }

    /**
     * Gibt den Wert einer bestimmten Konfiguration zurück, mit einem Standardwert.
     *
     * @param key          Der Schlüssel der Konfiguration.
     * @param defaultValue Der Standardwert, falls der Schlüssel nicht existiert.
     * @return Der Wert oder der Standardwert.
     */
    public String getConfigValue(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    /**
     * Setzt einen neuen Wert für eine Konfiguration und speichert die Datei.
     *
     * @param key   Der Schlüssel der Konfiguration.
     * @param value Der neue Wert.
     */
    public void setConfigValue(String key, String value) {
        if (!value.equals(config.get(key))) { // Nur speichern, wenn sich der Wert ändert
            config.put(key, value);
            saveConfig();
            logger.info("Konfiguration geändert: {} = {}", key, value);
        }
    }

    /**
     * Gibt den Standardwert für einen Schlüssel zurück.
     *
     * @param key Der Schlüssel der Konfiguration.
     * @return Der Standardwert.
     */
    private String getDefaultConfigValue(String key) {
        switch (key) {
            case "javaPath": return "java";
            case "serverJar": return "server.jar";
            case "minMemory": return "1024M";
            case "maxMemory": return "2048M";
            case "autoRestart": return "false";
            case "restartTimes": return "00:00,06:00,12:00,18:00";
            case "autoSave": return "false";
            case "backupFolder": return "./backups";
            case "backupBeforeRestart": return "true";
            default: return "";
        }
    }
}
