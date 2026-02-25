package me.justsid.mcservergui;

/**
 * Enum für die verschiedenen Zustände des Minecraft-Servers.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public enum ServerState {
    /** Server ist gestoppt. */
    OFFLINE("OFFLINE", "#808080"),
    /** Server wird gestartet. */
    STARTING("STARTING", "#FFA500"),
    /** Server läuft und ist erreichbar. */
    ONLINE("ONLINE", "#00FF00"),
    /** Server wird gestoppt. */
    STOPPING("STOPPING", "#FFA500"),
    /** Backup wird erstellt. */
    BACKUP("BACKUP", "#00BFFF"),
    /** Server wird neugestartet. */
    RESTARTING("RESTARTING", "#FFA500"),
    /** Server ist in einem Fehlerzustand. */
    ERROR("ERROR", "#FF0000");

    private final String displayName;
    private final String color;

    ServerState(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Gibt den Anzeigenamen zurück.
     * 
     * @return Der formatierte Anzeigename.
     */
    public String getDisplayName() {
        return "[" + displayName + "]";
    }

    /**
     * Gibt die Farbe für diesen Zustand zurück.
     * 
     * @return Die Farbe als Hex-String.
     */
    public String getColor() {
        return color;
    }
}
