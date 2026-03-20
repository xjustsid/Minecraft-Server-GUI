package me.justsid.mcservergui;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet Ban-Liste und Whitelist des Minecraft-Servers.
 * Liest/Schreibt die Standard JSON-Dateien (banlist.json, whitelist.json) im Server-Verzeichnis.
 */
public class PlayerAdminManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerAdminManager.class);

    private final Path serverDirectory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private List<BanEntry> banList = new ArrayList<>();
    private List<WhitelistEntry> whitelist = new ArrayList<>();

    public static class BanEntry {
        public String uuid;
        public String name;
        public String created;
        public String reason;
        public String source;
        public String expires; // null = permanent

        public BanEntry(String uuid, String name, String reason, String source) {
            this.uuid = uuid;
            this.name = name;
            this.created = Instant.now().toString();
            this.reason = reason;
            this.source = source;
            this.expires = null;
        }
    }

    public static class WhitelistEntry {
        public String uuid;
        public String name;
        public String created;
        public String addedBy;

        public WhitelistEntry(String uuid, String name, String addedBy) {
            this.uuid = uuid;
            this.name = name;
            this.created = Instant.now().toString();
            this.addedBy = addedBy;
        }
    }

    public PlayerAdminManager(Path serverDirectory) {
        this.serverDirectory = serverDirectory;
        loadBanList();
        loadWhitelist();
    }

    private void loadBanList() {
        try {
            Path banFile = serverDirectory.resolve("banlist.json");
            if (Files.exists(banFile)) {
                String content = Files.readString(banFile);
                JsonArray array = JsonParser.parseString(content).getAsJsonArray();
                banList.clear();
                for (JsonElement elem : array) {
                    banList.add(gson.fromJson(elem, BanEntry.class));
                }
                logger.info("Ban-Liste geladen: {} Eintraege", banList.size());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Konnte Ban-Liste nicht laden", e);
        }
    }

    private void loadWhitelist() {
        try {
            Path wlFile = serverDirectory.resolve("whitelist.json");
            if (Files.exists(wlFile)) {
                String content = Files.readString(wlFile);
                JsonArray array = JsonParser.parseString(content).getAsJsonArray();
                whitelist.clear();
                for (JsonElement elem : array) {
                    whitelist.add(gson.fromJson(elem, WhitelistEntry.class));
                }
                logger.info("Whitelist geladen: {} Eintraege", whitelist.size());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Konnte Whitelist nicht laden", e);
        }
    }

    public void addBan(String uuid, String name, String reason, String source) throws IOException {
        if (banList.stream().anyMatch(b -> b.uuid.equals(uuid))) {
            throw new IllegalArgumentException("Spieler bereits gebannt: " + name);
        }
        banList.add(new BanEntry(uuid, name, reason, source));
        saveBanList();
        logger.info("Spieler gebannt: {} ({})", name, reason);
    }

    public void removeBan(String uuid) throws IOException {
        banList.removeIf(b -> b.uuid.equals(uuid));
        saveBanList();
        logger.info("Ban entfernt fuer UUID: {}", uuid);
    }

    private void saveBanList() throws IOException {
        Path banFile = serverDirectory.resolve("banlist.json");
        Files.writeString(banFile, gson.toJson(banList),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.debug("Ban-Liste gespeichert");
    }

    public void addToWhitelist(String uuid, String name, String addedBy) throws IOException {
        if (whitelist.stream().anyMatch(w -> w.uuid.equals(uuid))) {
            throw new IllegalArgumentException("Spieler bereits auf Whitelist: " + name);
        }
        whitelist.add(new WhitelistEntry(uuid, name, addedBy));
        saveWhitelist();
        logger.info("Spieler whitelisted: {}", name);
    }

    public void removeFromWhitelist(String uuid) throws IOException {
        whitelist.removeIf(w -> w.uuid.equals(uuid));
        saveWhitelist();
        logger.info("Von Whitelist entfernt: {}", uuid);
    }

    private void saveWhitelist() throws IOException {
        Path wlFile = serverDirectory.resolve("whitelist.json");
        Files.writeString(wlFile, gson.toJson(whitelist),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.debug("Whitelist gespeichert");
    }

    public List<BanEntry> getBanList() { return new ArrayList<>(banList); }
    public List<WhitelistEntry> getWhitelist() { return new ArrayList<>(whitelist); }

    public void reloadFromDisk() {
        loadBanList();
        loadWhitelist();
        logger.info("Ban-/Whitelist neu vom Disk geladen");
    }
}
