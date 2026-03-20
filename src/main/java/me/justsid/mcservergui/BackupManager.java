package me.justsid.mcservergui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Arrays;

/**
 * Verwaltet Backups des Minecraft-Servers.
 * Erstellt komprimierte ZIP-Backups des World-Ordners.
 * 
 * @author McServerGUI
 * @version 1.0
 */
public class BackupManager {

    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    private final ConfigManager configManager;

    /**
     * Konstruktor für BackupManager.
     *
     * @param configManager Instanz des ConfigManager für Konfigurationen.
     */
    public BackupManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Startet ein asynchrones Backup.
     *
     * @return CompletableFuture<Boolean>, das den Erfolg des Backups signalisiert.
     */
    public CompletableFuture<Boolean> triggerBackupAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                triggerBackup();
                return true;
            } catch (Exception e) {
                logger.error("Backup fehlgeschlagen: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Führt ein Backup synchron aus.
     *
     * @throws IOException Wenn beim Backup ein Fehler auftritt.
     */
    public void triggerBackup() throws IOException {
        String backupFolderPath = configManager.getConfigValue("backupFolder");
        ensureBackupFolderExists(backupFolderPath); // Sicherstellen, dass der Backup-Ordner existiert
        String worldFolderPath = getWorldFolderName(); // Aus server.properties lesen

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File backupFile = new File(backupFolderPath, "backup_" + timestamp + ".zip");

        logger.info("Starte Backup. Zielpfad: {}", backupFile.getAbsolutePath());
        zipDirectory(Paths.get(worldFolderPath), backupFile.toPath());
        logger.info("Backup erfolgreich erstellt: {}", backupFile.getAbsolutePath());
    }

    /**
     * Liest den World-Namen aus server.properties.
     *
     * @return Der World-Name oder "world" als Fallback.
     */
    private String getWorldFolderName() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
            return props.getProperty("level-name", "world");
        } catch (IOException e) {
            logger.warn("Konnte server.properties nicht lesen, verwende 'world' als Fallback", e);
            return "world";
        }
    }

    /**
     * Überprüft, ob der Backup-Ordner existiert, und erstellt ihn bei Bedarf.
     *
     * @param backupFolderPath Der Pfad zum Backup-Ordner.
     * @throws IOException Wenn der Ordner nicht erstellt werden kann.
     */
    private void ensureBackupFolderExists(String backupFolderPath) throws IOException {
        File backupDir = new File(backupFolderPath);
        if (!backupDir.exists()) {
            if (backupDir.mkdirs()) {
                logger.info("Backup-Ordner erstellt: {}", backupFolderPath);
            } else {
                throw new IOException("Fehler beim Erstellen des Backup-Ordners: " + backupFolderPath);
            }
        }
    }

    /**
     * Komprimiert ein Verzeichnis in eine ZIP-Datei.
     *
     * @param sourceDirPath Der Pfad zum zu sichernden Verzeichnis.
     * @param zipFilePath   Der Pfad zur resultierenden ZIP-Datei.
     * @throws IOException Wenn beim Erstellen der ZIP-Datei ein Fehler auftritt.
     */
    private void zipDirectory(Path sourceDirPath, Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDirPath).forEach(path -> {
                try {
                    String zipEntryName = sourceDirPath.relativize(path).toString();
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    if (Files.isRegularFile(path)) {
                        Files.copy(path, zos);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /**
     * Löscht alte Backups, wenn eine maximale Anzahl überschritten wird.
     * Kann genutzt werden, um Speicherplatz zu sparen.
     *
     * @param maxBackups Die maximale Anzahl an Backups, die behalten werden sollen.
     */
    public void deleteOldBackups(int maxBackups) {
        String backupFolderPath = configManager.getConfigValue("backupFolder");
        File backupFolder = new File(backupFolderPath);

        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            logger.warn("Backup-Ordner existiert nicht oder ist kein Verzeichnis: {}", backupFolderPath);
            return;
        }

        File[] backups = backupFolder.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".zip"));
        if (backups != null && backups.length > maxBackups) {
            logger.info("Zu viele Backups gefunden. Lösche alte Backups...");
            // Backups nach Alter sortieren
            Arrays.sort(backups, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

            for (int i = 0; i < backups.length - maxBackups; i++) {
                if (backups[i].delete()) {
                    logger.info("Altes Backup gelöscht: {}", backups[i].getName());
                } else {
                    logger.warn("Konnte Backup nicht löschen: {}", backups[i].getName());
                }
            }
        }
    }
}
