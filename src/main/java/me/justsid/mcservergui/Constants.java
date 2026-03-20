package me.justsid.mcservergui;

/**
 * Zentrale Konstanten-Klasse für die Anwendung.
 * Enthält alle Magic Numbers und wiederverwendbare Werte.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public class Constants {
    
    // Timing (in Millisekunden)
    /** Intervall für Label-Updates in Millisekunden. */
    public static final long LABEL_UPDATE_INTERVAL_MS = 1000;
    /** Timeout für Server-Start in Millisekunden. */
    public static final long SERVER_START_TIMEOUT_MS = 60000;
    /** Timeout für Server-Stopp in Millisekunden. */
    public static final long SERVER_STOP_TIMEOUT_MS = 30000;
    
    // Window Sizes
    /** Breite des Hauptfensters. */
    public static final int MAIN_WINDOW_WIDTH = 845;
    /** Höhe des Hauptfensters. */
    public static final int MAIN_WINDOW_HEIGHT = 677;
    /** Breite des Options-Dialogs. */
    public static final int OPTIONS_DIALOG_WIDTH = 400;
    /** Höhe des Options-Dialogs. */
    public static final int OPTIONS_DIALOG_HEIGHT = 600;
    /** Breite des Ersteinrichtungs-Dialogs. */
    public static final int FIRST_CONFIG_DIALOG_WIDTH = 300;
    /** Höhe des Ersteinrichtungs-Dialogs. */
    public static final int FIRST_CONFIG_DIALOG_HEIGHT = 400;
    
    // Default Values
    /** Standard-Name des World-Ordners. */
    public static final String DEFAULT_WORLD_FOLDER = "world";
    /** Maximale Anzahl von Backups. */
    public static final int DEFAULT_MAX_BACKUPS = 10;
    /** Standard-Zeiten für automatische Speicherungen. */
    public static final String DEFAULT_SAVE_TIMES = "00:00,06:00,12:00,18:00";
    /** Standard-Zeiten für automatische Neustarts. */
    public static final String DEFAULT_RESTART_TIMES = "00:00,06:00,12:00,18:00";
    
    // Config Keys
    /** Config-Schlüssel für Java-Pfad. */
    public static final String KEY_JAVA_PATH = "javaPath";
    /** Config-Schlüssel für Server-JAR. */
    public static final String KEY_SERVER_JAR = "serverJar";
    /** Config-Schlüssel für maximalen Speicher. */
    public static final String KEY_MAX_MEMORY = "maxMemory";
    /** Config-Schlüssel für minimalen Speicher. */
    public static final String KEY_MIN_MEMORY = "minMemory";
    /** Config-Schlüssel für Auto-Save. */
    public static final String KEY_AUTO_SAVE = "autoSave";
    /** Config-Schlüssel für Auto-Restart. */
    public static final String KEY_AUTO_RESTART = "autoRestart";
    /** Config-Schlüssel für Speicherzeiten. */
    public static final String KEY_SAVE_TIMES = "saveTimes";
    /** Config-Schlüssel für Neustartzeiten. */
    public static final String KEY_RESTART_TIMES = "restartTimes";
    /** Config-Schlüssel für Backup-Ordner. */
    public static final String KEY_BACKUP_FOLDER = "backupFolder";
    
    // Server
    /** Dateiname für server.properties. */
    public static final String SERVER_PROPERTIES_FILE = "server.properties";
    /** Dateiname für eula.txt. */
    public static final String EULA_FILE = "eula.txt";
    /** Dateiname für config.txt. */
    public static final String CONFIG_FILE = "config.txt";
    
    /** Privater Konstruktor um Instantiierung zu verhindern. */
    private Constants() {
    }
}
