package me.justsid.mcservergui;

import static me.justsid.mcservergui.Constants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigManager {

    public static final String KEY_CONFIG_VERSION = "app.config.version";
    public static final String KEY_SERVER_DIRECTORY = "serverDirectory";
    public static final int CURRENT_CONFIG_VERSION = 1;

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private final Path configFilePath = Paths.get(CONFIG_FILE);
    private final Map<String, String> config = new LinkedHashMap<>();
    private final Map<String, String> comments = new LinkedHashMap<>();
    private final boolean firstRun;

    /**
     * Konstruktor: Lädt die Konfiguration und erstellt Standardwerte beim ersten Start.
     */
    public ConfigManager() {
        initializeComments();
        createDefaultConfig();

        this.firstRun = !checkIfConfigExists();
        if (!firstRun) {
            loadConfig();
        } else {
            logger.info("Erste Ausführung erkannt. Warte auf Setup-Wizard bevor config.txt geschrieben wird.");
        }

        validateConfig();
        ensureConfigVersion();
        migrateIfNeeded();
    }

    /**
     * Prüft, ob die Konfigurationsdatei existiert.
     *
     * @return true, wenn die Datei existiert.
     */
    private boolean checkIfConfigExists() {
        return Files.exists(configFilePath);
    }

    /**
     * Gibt zurück, ob es sich um die erste Ausführung handelt.
     * Prüft ob wichtige Konfigurationswerte fehlen.
     *
     * @return true, wenn wichtige Konfigurationswerte fehlen.
     */
    public boolean isFirstRun() {
        String serverJar = config.get(KEY_SERVER_JAR);
        String maxMemory = config.get(KEY_MAX_MEMORY);
        String javaPath = config.get(KEY_JAVA_PATH);
        return firstRun
            || serverJar == null || serverJar.isEmpty()
            || maxMemory == null || maxMemory.isEmpty()
            || javaPath == null || javaPath.isEmpty();
    }

    public boolean exists() {
        return checkIfConfigExists();
    }

    /**
     * Lädt die Konfiguration aus der Datei.
     */
    public void loadConfig() {
        if (Files.exists(configFilePath)) {
            try (BufferedReader reader = Files.newBufferedReader(configFilePath)) {
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
        logger.debug("Geladene Restart-Zeiten: {}", config.get("restartTimes"));
    }

    /**
     * Erstellt eine Standardkonfiguration.
     */
    private void createDefaultConfig() {
        config.put(KEY_CONFIG_VERSION, String.valueOf(CURRENT_CONFIG_VERSION));
        config.put(KEY_SERVER_DIRECTORY, ".");
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
        comments.put(KEY_CONFIG_VERSION, "Version der Konfigurationsdatei für spätere Migrationen.");
        comments.put(KEY_SERVER_DIRECTORY, "Basisverzeichnis des Minecraft-Servers. Standard: '.'");
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
            if (!config.containsKey(key) || config.get(key) == null || config.get(key).isBlank()) {
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

        if (config.get(KEY_SERVER_DIRECTORY) == null || config.get(KEY_SERVER_DIRECTORY).isBlank()) {
            config.put(KEY_SERVER_DIRECTORY, ".");
            updated = true;
        }

        if (updated && exists()) {
            logger.info("Fehlende oder ungültige Konfigurationswerte erkannt und korrigiert.");
            saveConfig();
        }
    }

    public void ensureConfigVersion() {
        String version = config.get(KEY_CONFIG_VERSION);
        if (version == null || version.isBlank()) {
            config.put(KEY_CONFIG_VERSION, String.valueOf(CURRENT_CONFIG_VERSION));
            if (exists()) {
                saveConfig();
            }
            logger.info("Config-Versionsfeld hinzugefügt: v{}", CURRENT_CONFIG_VERSION);
        }
    }

    public void migrateIfNeeded() {
        String currentVersion = config.get(KEY_CONFIG_VERSION);
        int versionNumber = CURRENT_CONFIG_VERSION;
        boolean updated = false;
        if (currentVersion == null || currentVersion.isBlank()) {
            versionNumber = CURRENT_CONFIG_VERSION;
        } else {
            try {
                versionNumber = Integer.parseInt(currentVersion);
            } catch (NumberFormatException e) {
                logger.warn("Ungueltiges Config-Versionsfeld '{}', setze auf 1", currentVersion);
                versionNumber = 1;
                config.put(KEY_CONFIG_VERSION, "1");
                updated = true;
            }
        }

        if (versionNumber < CURRENT_CONFIG_VERSION) {
            logger.info("Führe Konfigurationsmigration von v{} auf v{} durch", versionNumber, CURRENT_CONFIG_VERSION);
            config.put(KEY_CONFIG_VERSION, String.valueOf(CURRENT_CONFIG_VERSION));
            updated = true;
        }

        if (updated && exists()) {
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
     * Schreibt atomar über eine temporäre Datei (kein Datenverlust bei Absturz).
     */
    public void saveConfig() {
        Path target = configFilePath;
        Path tempFile = null;
        try {
            Path parentDir = target.toAbsolutePath().getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            tempFile = Files.createTempFile(parentDir, ".config-", ".tmp");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(tempFile))) {
                writer.println("# Minecraft Server Configuration");
                writer.println("# Diese Datei wurde automatisch generiert.");
                for (Map.Entry<String, String> entry : config.entrySet()) {
                    if (comments.containsKey(entry.getKey())) {
                        writer.println("# " + comments.get(entry.getKey()));
                    }
                    writer.println(entry.getKey() + "=" + entry.getValue());
                }
            }

            // Try to set restrictive permissions (owner-read/write only) on POSIX systems
            try {
                Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows doesn't support POSIX permissions – acceptable for a desktop app
            }

            // Atomic move: prevents partial-write corruption
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.info("Konfiguration erfolgreich gespeichert.");
        } catch (IOException e) {
            logger.error("Fehler beim Speichern der Konfiguration.", e);
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
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

    public String getProperty(String key) {
        return getConfigValue(key);
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

    public String getProperty(String key, String defaultValue) {
        return getConfigValue(key, defaultValue);
    }

    /**
     * Setzt einen neuen Wert für eine Konfiguration und speichert die Datei.
     *
     * @param key   Der Schlüssel der Konfiguration.
     * @param value Der neue Wert.
     */
    public void setConfigValue(String key, String value) {
        if (!Objects.equals(value, config.get(key))) { // Nur speichern, wenn sich der Wert ändert
            config.put(key, value);
            saveConfig();
            // Don't log values to avoid exposing sensitive paths/credentials in log files
            logger.debug("Konfiguration geändert: key={}", key);
        }
    }

    public void setProperty(String key, String value) {
        setConfigValue(key, value);
    }

    public void setConfigValueSilently(String key, String value) {
        if (!Objects.equals(value, config.get(key))) {
            config.put(key, value);
        }
    }

    public void setPropertySilently(String key, String value) {
        setConfigValueSilently(key, value);
    }

    public void setPropertyIfAbsent(String key, String value) {
        if (!config.containsKey(key) || config.get(key) == null || config.get(key).isBlank()) {
            config.put(key, value);
        }
    }

    public String getServerDirectory() {
        return getConfigValue(KEY_SERVER_DIRECTORY, ".");
    }

    public String getBackupDirectory() {
        return getConfigValue(KEY_BACKUP_FOLDER, "./backups");
    }

    /**
     * Gibt den Standardwert für einen Schlüssel zurück.
     *
     * @param key Der Schlüssel der Konfiguration.
     * @return Der Standardwert.
     */
    private String getDefaultConfigValue(String key) {
        switch (key) {
            case "app.config.version": return String.valueOf(CURRENT_CONFIG_VERSION);
            case "serverDirectory": return ".";
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
