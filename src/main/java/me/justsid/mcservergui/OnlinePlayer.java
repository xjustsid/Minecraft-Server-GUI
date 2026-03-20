package me.justsid.mcservergui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * JavaFX-Datenmodell für einen online befindlichen Minecraft-Spieler.
 */
public class OnlinePlayer {
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty("Online");

    public OnlinePlayer(String name, String uuid) {
        this.name.set(name);
        this.uuid.set(uuid);
    }

    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    public String getUUID() { return uuid.get(); }
    public void setUUID(String value) { uuid.set(value); }
    public StringProperty uuidProperty() { return uuid; }

    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }
}
