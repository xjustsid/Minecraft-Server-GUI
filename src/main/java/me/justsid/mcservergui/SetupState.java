package me.justsid.mcservergui;

import java.util.LinkedHashMap;
import java.util.Map;

public record SetupState(
    String serverJar,
    String javaPath,
    String minMemory,
    String maxMemory,
    String backupFolder,
    String serverDirectory
) {
    public static SetupState fromConfig(ConfigManager configManager) {
        return new SetupState(
            configManager.getConfigValue(Constants.KEY_SERVER_JAR, "server.jar"),
            configManager.getConfigValue(Constants.KEY_JAVA_PATH, "java"),
            configManager.getConfigValue(Constants.KEY_MIN_MEMORY, "1024M"),
            configManager.getConfigValue(Constants.KEY_MAX_MEMORY, "2048M"),
            configManager.getConfigValue(Constants.KEY_BACKUP_FOLDER, "./backups"),
            configManager.getServerDirectory()
        );
    }

    public Map<String, String> toConfigMap() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(ConfigManager.KEY_CONFIG_VERSION, String.valueOf(ConfigManager.CURRENT_CONFIG_VERSION));
        values.put(ConfigManager.KEY_SERVER_DIRECTORY, serverDirectory == null || serverDirectory.isBlank() ? "." : serverDirectory);
        values.put(Constants.KEY_SERVER_JAR, serverJar);
        values.put(Constants.KEY_JAVA_PATH, javaPath);
        values.put(Constants.KEY_MIN_MEMORY, minMemory);
        values.put(Constants.KEY_MAX_MEMORY, maxMemory);
        values.put(Constants.KEY_BACKUP_FOLDER, backupFolder == null || backupFolder.isBlank() ? "./backups" : backupFolder);
        return values;
    }

    public boolean hasRequiredFields() {
        return serverJar != null && !serverJar.isBlank()
            && javaPath != null && !javaPath.isBlank()
            && minMemory != null && !minMemory.isBlank()
            && maxMemory != null && !maxMemory.isBlank();
    }
}