package me.justsid.mcservergui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RollbackManager {
    private static final Logger logger = LoggerFactory.getLogger(RollbackManager.class);

    private static final String[] WORLD_LOAD_ERRORS = {
        "Failed to load level",
        "Exception loading chunk",
        "Failed to read chunk",
        "Corrupt world data",
        "java.io.IOException",
        "Could not load level",
        "Error loading world",
        "Invalid world data"
    };

    private final ServerOperationCoordinator coordinator;
    private final ConfigManager configManager;
    private volatile boolean isEnabled = false;
    private volatile boolean rollbackInProgress = false;

    public RollbackManager(ServerProcessManager serverProcessManager,
                          ServerOperationCoordinator coordinator,
                          ConfigManager configManager) {
        this.coordinator = coordinator;
        this.configManager = configManager;
    }

    public void start() {
        isEnabled = true;
        logger.info("Rollback-Überwachung gestartet.");
    }

    public void stop() {
        isEnabled = false;
        logger.info("Rollback-Überwachung gestoppt.");
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void checkForWorldErrors(String logLine) {
        if (!isEnabled || rollbackInProgress) {
            return;
        }

        for (String error : WORLD_LOAD_ERRORS) {
            if (logLine.contains(error)) {
                logger.warn("Welt-Ladefehler erkannt: {}. Starte Rollback...", logLine);
                performRollback();
                break;
            }
        }
    }

    private void performRollback() {
        if (rollbackInProgress) {
            return;
        }
        
        rollbackInProgress = true;

        try {
            File latestBackup = findLatestBackup();
            if (latestBackup == null) {
                logger.error("Kein Backup gefunden für Rollback!");
                return;
            }

            logger.info("Stelle Backup wieder her: {}", latestBackup.getName());

            String worldFolder = getWorldFolderName();
            Path worldPath = Paths.get(worldFolder);

            if (Files.exists(worldPath) && !Files.isSymbolicLink(worldPath)) {
                deleteDirectory(worldPath.toFile());
            } else if (Files.isSymbolicLink(worldPath)) {
                logger.warn("Rollback abgebrochen: World-Pfad ist ein symbolischer Link: {}", worldPath);
                return;
            }

            extractBackup(latestBackup.toPath(), Paths.get("."));

            logger.info("Rollback abgeschlossen. Server wird neu gestartet...");
            coordinator.startServer();

        } catch (Exception e) {
            logger.error("Rollback fehlgeschlagen: {}", e.getMessage(), e);
        } finally {
            rollbackInProgress = false;
        }
    }

    private File findLatestBackup() {
        String backupFolderPath = configManager.getConfigValue("backupFolder", "backup");
        File backupFolder = new File(backupFolderPath);

        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            return null;
        }

        File[] backups = backupFolder.listFiles((dir, name) -> 
            name.startsWith("backup_") && name.endsWith(".zip")
        );

        if (backups == null || backups.length == 0) {
            return null;
        }

        Arrays.sort(backups, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return backups[0];
    }

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

    private void deleteDirectory(File directory) throws IOException {
        // Use walkFileTree to avoid following symbolic links
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Do not follow symbolic links into subdirectories
                if (attrs.isSymbolicLink()) {
                    Files.delete(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void extractBackup(Path zipPath, Path destination) throws IOException {
        Path canonicalDestination = destination.toRealPath();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Zip-Slip protection: resolve and normalize, then verify entry stays inside destination
                Path entryPath = canonicalDestination.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(canonicalDestination)) {
                    logger.error("Zip-Slip-Angriff abgeblockt: '{}' liegt außerhalb des Zielordners.", entry.getName());
                    throw new IOException("Ungültiger ZIP-Eintrag außerhalb des Zielordners: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipFile.getInputStream(entry), entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
