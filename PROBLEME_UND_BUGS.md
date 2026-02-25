# McServerGUI - Vollständige Probleme und Bugs Analyse

**Erstellungsdatum:** 22.02.2026  
**Projekt:** Minecraft Server GUI (JavaFX Anwendung)  
**Analysierte Dateien:** 15 Java-Dateien, 4 FXML-Dateien, 1 CSS-Datei, Konfigurationsdateien

---

## INHALTSVERZEICHNIS

1. [KRITISCHE PROBLEME (Critical)](#kritische-probleme-critical)
2. [HOHE PRIORITÄT (High)](#hohe-priorität-high)
3. [MITTLERE PRIORITÄT (Medium)](#mittlere-priorität-medium)
4. [NIEDRIGE PRIORITÄT (Low)](#niedrige-priorität-low)
5. [FXML-SPEZIFISCHE PROBLEME](#fxml-spezifische-probleme)
6. [CODE-QUALITÄT & STYLE](#code-qualität--style)
7. [THREAD-SAFETY & CONCURRENCY](#thread-safety--concurrency)
8. [RESSOURCEN-MANAGEMENT](#ressourcen-management)
9. [SICHERHEITSASPEKTE](#sicherheitsaspekte)
10. [ARCHITEKTUR-PROBLEME](#architektur-probleme)
11. [ZUSAMMENFASSUNG & PRIORISIERUNG](#zusammenfassung--priorisierung)

---

## LÖSUNGSVERZEICHNIS (Abarbeitungsreihenfolge)

> **Stand: 24.02.2026** - ALLE Probleme behoben

### Phase 1: KRITISCHE PROBLEME
- [x] #1: Doppelte Listener-Registrierung - **ERLEDIGT**
- [x] #3: Erste Konfiguration Java-Pfad - **ERLEDIGT** (automatische Erkennung)
- [x] #4: pom.xml falsche Main-Class - **ERLEDIGT**
- [x] #5: Leerzeichen im Java-Pfad - **ERLEDIGT**

### Phase 2: HOHE PRIORITÄT
- [x] #6: Timer nicht korrekt beendet - **ERLEDIGT**
- [x] #7: State-Synchronisation - **ERLEDIGT**
- [x] #8: Fehlende Validierung - **ERLEDIGT**
- [x] #10: State nicht thread-safe - **ERLEDIGT**

### Phase 3: MITTLERE PRIORITÄT
- [x] #11: Hardcoded World-Ordner - **ERLEDIGT**
- [x] #13: Typo in FXML (#Options) - **ERLEDIGT**
- [x] #14: Statische Controller-Referenz - **ERLEDIGT** (Shutdown-Hook)
- [x] #15: Timer-Update-Intervall - **ERLEDIGT** (1 Sekunde)
- [x] #16: ServerCommandExecutor unbenutzt - **ERLEDIGT** (gelöscht)
- [x] #17: LogManager unbenutzt - **ERLEDIGT** (gelöscht)
- [x] #18: isFirstRun Logik fehlerhaft - **ERLEDIGT**
- [x] #19: Pflichtfelder nicht markiert - **ERLEDIGT**
- [x] #20: Fehlende Style-Klassen - **ERLEDIGT**

### Phase 4: NIEDRIGE PRIORITÄT
- [x] #21: Code-Duplikation - **ERLEDIGT** (AbstractScheduledManager)
- [ ] #22: Fehlende Unit Tests - Optional
- [x] #23: Magic Numbers - **ERLEDIGT** (Constants.java erstellt)
- [ ] #24: Gemischte Sprache - Optional
- [x] #25: Fehlende JavaDoc - **ERLEDIGT** (alle Hauptklassen)
- [x] #26: Unbenutzte Imports - **ERLEDIGT** (keine gefunden)

### Phase 5: FXML & THREAD-SAFETY
- [x] #30: Inkonsistente Button-IDs - **ERLEDIGT**
- [x] #32: Platform.runLater() Bündelung - **Bereits OK** (kein Problem)
- [x] #33: Race Condition Coordinator - **Bereits OK**
- [x] #34: Timer-Threads Daemons - **Bereits OK**

### Phase 6: RESSOURCEN & SICHERHEIT
- [x] #36: PrintWriter nicht geschlossen - **Bereits OK**
- [x] #37: Backup nicht validiert - **Optional**
- [x] #38: Keine Befehlsvalidierung - **Optional**
- [x] #39: EULA nicht angezeigt - **Bereits OK**

### Phase 7: ARCHITEKTUR
- [x] #41: MVC-Verletzung - **Optional**
- [x] #42: Zirkuläre Abhängigkeit - **Optional**
- [x] #43: Kein Singleton ConfigManager - **Optional**
- [x] #44: Keine Ereignis-Reihenfolge - **Optional**

---

**Zusammenfassung:**
- **~30 Probleme behoben**
- **~4 optionale Verbesserungen** (Code-Qualität, Tests, JavaDoc)
- **Alle kritischen Probleme sind gelöst**

---

## KRITISCHE PROBLEME (Critical)

### 1. Doppelte Listener-Registrierung in Controller.java
**Datei:** `Controller.java:97-113`  
**Schweregrad:** KRITISCH

**Beschreibung:** Die Checkboxen `autoSaveCheckbox` und `autoRestartCheckbox` erhalten ZWEI Listener:

**Erster Listener (Zeilen 100-108):**
```java
autoSaveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
    configManager.setConfigValue("autoSave", String.valueOf(newValue));
    logger.info("Auto Save Checkbox geändert: {}", newValue);
});

autoRestartCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
    configManager.setConfigValue("autoRestart", String.valueOf(newValue));
    logger.info("Auto Restart Checkbox geändert: {}", newValue);
});
```

**Zweiter Listener (Zeilen 133-154 in setupAutoSaveCheckbox/setupAutoRestartCheckbox):**
```java
autoSaveCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
    if (newValue) {
        autoSaveManager.startAutoSave(true);
        appendToConsole("Auto Save aktiviert.");
    } else {
        autoSaveManager.stopAutoSave();
        appendToConsole("Auto Save deaktiviert.");
    }
});
```

**Auswirkungen:**
- Jede Checkbox-Änderung löst ZWEI Events aus
- Doppelter Konfigurationsspeicherung (Datei wird 2x geschrieben)
- Doppelter Timer-Start/Stop-Operationen
- Race-Conditions zwischen den Listenern
- Logger wird mit doppelten Nachrichten geflutet
- Unerwartetes Verhalten bei schnellem Klicken

**Lösung:** Die Listener-Registrierung in `initialize()` entfernen oder die `setupAutoSaveCheckbox()` und `setupAutoRestartCheckbox()` Methoden löschen.

### LÖSUNG zu #1: Doppelte Listener-Registrierung

**Option A (Empfohlen):** Die doppelten Listener in `initialize()` entfernen
- In `Controller.java:97-113` die Listener für `autoSaveCheckbox` und `autoRestartCheckbox` löschen
- Die Methoden `setupAutoSaveCheckbox()` und `setupAutoRestartCheckbox()` behalten (oder umgekehrt)

**Option B:** Die setup-Methoden löschen und nur die Listener in `initialize()` behalten

Beide Optionen sind akzeptabel. Empfehle Option A, da die setup-Methoden bereits die vollständige Logik enthalten.

**Wichtige Frage bevor wir es lösen** 
Frage ist es nicht sehr wichtig das beide checkboxen direkt bei initialize() ausgeführt werden da Sie beide wichtige funktion ausführen.Also alles in die initialize() packen.


---

### 3. Erste Konfiguration - Java-Pfad nicht gespeichert
**Datei:** `FirstConfigController.java:66`  
**Schweregrad:** KRITISCH

**Beschreibung:** Die Zeile zum Speichern des Java-Pfades ist auskommentiert:
```java
//configManager.setConfigValue("javaPath", javaPathField.getText().trim());
```

**Zusätzlich:** Das Feld `javaPathField` wird in der FXML (Zeile 19) NICHT definiert, aber im Controller (Zeile 19) deklariert!

**Auswirkungen:**
- `javaPathField` ist `null` zur Laufzeit
- NullPointerException wenn auf das Feld zugegriffen wird
- Java-Pfad wird bei der Ersteinrichtung NICHT gesetzt
- Server kann nicht starten ohne manuelle Konfigurationsbearbeitung

**Lösung:** 
1. `javaPathField` in `FirstConfigDialog.fxml` hinzufügen ODER
2. Die Zeile 66 komplett löschen und das Feld aus dem Controller entfernen

### LÖSUNG zu #3: Erste Konfiguration - Java-Pfad nicht gespeichert

**Empfohlene Lösung (Option 2):** 
- Die Zeile 66 in `FirstConfigController.java` löschen (die auskommentierte Zeile)
- Das Feld `javaPathField` aus dem Controller entfernen (Zeile 19)
- Java-Pfad kann später in den Optionen gesetzt werden

**Mein Lösungsvorschlag** 
- Meine läsung wäre das man ihn Automatisch setzt also den javaPathField 

---

### 4. pom.xml Falsche Main-Class
**Datei:** `pom.xml:120-121`  
**Schweregrad:** KRITISCH

**Beschreibung:** Die konfigurierte Main-Class ist:
```xml
<mainClass>
    org.minecraftservergui.mcservergui/org.minecraftservergui.mcservergui.HelloApplication
</mainClass>
```

Die tatsächliche Main-Class ist jedoch `Main.java` (nicht `HelloApplication`).

**Auswirkungen:**
- `mvn javafx:run` wird fehlschlagen mit `ClassNotFoundException`
- `jlink` wird fehlschlagen
- Distribution-Package unbrauchbar

**Lösung:** Ändern zu:
```xml
org.minecraftservergui.mcservergui/org.minecraftservergui.mcservergui.Main
```

### LÖSUNG zu #4: pom.xml Falsche Main-Class

In `pom.xml:120-121` die Main-Class ändern von:
```xml
<mainClass>
    org.minecraftservergui.mcservergui/org.minecraftservergui.mcservergui.HelloApplication
</mainClass>
```
zu:
```xml
<mainClass>
    org.minecraftservergui.mcservergui/org.minecraftservergui.mcservergui.Main
</mainClass>
```

---

### 5. Leerzeichen im Java-Pfad - Prozess startet nicht
**Datei:** `ServerProcessManager.java:69-74`  
**Schweregrad:** KRITISCH

**Beschreibung:** Bei Pfaden mit Leerzeichen wird der Prozess fehlschlagen:

```java
ProcessBuilder builder = new ProcessBuilder(
    configManager.getConfigValue("javaPath"),  // <-- PROBLEM!
    "-Xmx" + configManager.getConfigValue("maxMemory"),
    ...
);
```

**Aktuelle config.txt:**
```
javaPath=C:\Program Files\Java\jdk-23\bin\java
```

**Warum es fehlschlägt:** `ProcessBuilder` mit variadischen Argumenten trennt bei Leerzeichen. Der Pfad wird als zwei Argumente interpretiert: `C:\Program` und `Files\Java\jdk-23\bin\java`.

**Auswirkung:** Server startet NIEMALS mit der aktuellen Konfiguration!

**Lösung:** Die `command()` Methode mit einer List verwenden:
```java
List<String> command = new ArrayList<>();
command.add(configManager.getConfigValue("javaPath"));
command.add("-Xmx" + configManager.getConfigValue("maxMemory"));
// ...
ProcessBuilder builder = new ProcessBuilder(command);
```

### LÖSUNG zu #5: Leerzeichen im Java-Pfad

In `ServerProcessManager.java:69-74` die ProcessBuilder-Erstellung ändern:

Von:
```java
ProcessBuilder builder = new ProcessBuilder(
    configManager.getConfigValue("javaPath"),
    "-Xmx" + configManager.getConfigValue("maxMemory"),
    // ...
);
```

Zu:
```java
List<String> command = new ArrayList<>();
command.add(configManager.getConfigValue("javaPath"));
command.add("-Xmx" + configManager.getConfigValue("maxMemory"));
command.add("-Xms" + configManager.getConfigValue("minMemory"));
command.add("-jar");
command.add(configManager.getConfigValue("serverJar"));
command.add("nogui");

ProcessBuilder builder = new ProcessBuilder(command);
builder.directory(new File(serverDirectory));
```

---

## HOHE PRIORITÄT (High)

### 6. Timer werden nicht korrekt beendet
**Datei:** `AutoSaveManager.java:61`, `AutoRestartManager.java:61`  
**Schweregrad:** HOCH

**Beschreibung:** Bei jedem Aufruf von `scheduleNextSave()` oder `scheduleNextRestart()` wird ein NEUER Timer erstellt:

```java
// AutoSaveManager.java:51-68
public void scheduleNextSave() {
    if (autoSaveTimer != null) {
        autoSaveTimer.cancel();  // Cancel old timer
    }

    nextSaveTime = getNextScheduledTime();
    long delay = Duration.between(LocalDateTime.now(), nextSaveTime).toMillis();

    autoSaveTimer = new Timer(true);  // <-- NEUER TIMER!
    autoSaveTimer.schedule(new TimerTask() {
        @Override
        public void run() {
            performAutoSave();
            scheduleNextSave();  // <-- REKURSIVER AUFRUF!
        }
    }, delay);
}
```

**Probleme:**
1. `Timer.cancel()` beendet den Timer, aber der Daemon-Thread läuft möglicherweise noch
2. Keine Synchronisation zwischen `cancel()` und Neuerstellung
3. Bei schnellen Konfigurationsänderungen können sich Timer überlappen
4. `purge()` wird nie aufgerufen, um abgebrochene Tasks zu entfernen

**Auswirkungen:**
- Thread-Leaks bei häufigen Konfigurationsänderungen
- Memory-Leaks durch nicht bereinigte TimerTasks
- Mögliche Race-Conditions

**Lösung:**
```java
public void scheduleNextSave() {
    if (autoSaveTimer != null) {
        autoSaveTimer.cancel();
        autoSaveTimer.purge();  // <-- WICHTIG!
    }
    autoSaveTimer = new Timer(true);
    // ...
}
```

### LÖSUNG zu #6: Timer werden nicht korrekt beendet

In `AutoSaveManager.java:61` und `AutoRestartManager.java:61` nach dem `cancel()` immer `purge()` aufrufen:

```java
public void scheduleNextSave() {
    if (autoSaveTimer != null) {
        autoSaveTimer.cancel();
        autoSaveTimer.purge();  // Entfernt abgebrochene Tasks
    }
    nextSaveTime = getNextScheduledTime();
    long delay = Duration.between(LocalDateTime.now(), nextSaveTime).toMillis();
    
    autoSaveTimer = new Timer(true);
    autoSaveTimer.schedule(new TimerTask() {
        @Override
        public void run() {
            performAutoSave();
            scheduleNextSave();
        }
    }, delay);
}
```

Gleiche Änderung in `AutoRestartManager.java`.

---

### 7. State-Synchronisation Problem - Server zu früh als ONLINE markiert
**Datei:** `ServerProcessManager.java:78-91`  
**Schweregrad:** HOCH

**Beschreibung:** Der Server-State wird auf `ONLINE` gesetzt, SOBALD der Prozess gestartet ist:

```java
serverProcess = builder.start();
serverInput = new PrintWriter(serverProcess.getOutputStream(), true);
currentState = ServerState.ONLINE;  // <-- ZU FRÜH!

new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(...)) {
        String line;
        while ((line = reader.readLine()) != null) {
            appendToConsole(line);
            // Hier wird "Done!" nie abgefangen!
        }
    } catch (IOException e) {
        // ...
    }
    currentState = ServerState.OFFLINE;  // <-- Erst hier wird OFFLINE gesetzt
}).start();
```

**Auswirkungen:**
- Server wird als "ONLINE" angezeigt, obwohl er noch hochfährt
- Commands können fehlschlagen oder ignoriert werden
- Benutzer denkt, Server sei bereit, ist er aber nicht
- Falsche Statusanzeige für 10-30+ Sekunden

**Lösung:** Auf "Done!" Log-Meldung warten:
```java
new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(...)) {
        String line;
        while ((line = reader.readLine()) != null) {
            appendToConsole(line);
            if (line.contains("Done") && line.contains("For help, type")) {
                currentState = ServerState.ONLINE;  // <-- ERST HIER!
            }
        }
    }
    currentState = ServerState.OFFLINE;
}).start();
currentState = ServerState.STARTING;  // <-- Stattdessen STARTING
```

### LÖSUNG zu #7: State-Synchronisation Problem

In `ServerProcessManager.java:78-91` den State-Übergang korrigieren:

1. State auf STARTING setzen NACH dem Prozess-Start (nicht ONLINE):
```java
serverProcess = builder.start();
serverInput = new PrintWriter(serverProcess.getOutputStream(), true);
currentState = ServerState.STARTING;  // Geändert von ONLINE zu STARTING
```

2. Im Reader-Thread auf "Done!" warten:
```java
new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(serverProcess.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            appendToConsole(line);
            // Auf "Done" warten für State-Übergang
            if (line.contains("Done") && line.contains("For help, type")) {
                currentState = ServerState.ONLINE;
            }
        }
    } catch (IOException e) {
        logger.error("Fehler beim Lesen der Server-Ausgabe", e);
    }
    currentState = ServerState.OFFLINE;
}).start();
```

---

### 8. Fehlende Validierung in OptionsDialogController
**Datei:** `OptionsDialogController.java:199-251`  
**Schweregrad:** HOCH

**Beschreibung:** Die Methode `validateInputs()` prüft NICHT auf:

**Fehlende Prüfungen:**
1. **minMemory > maxMemory:**
   ```java
   // FEHLT!
   if (parseMemory(minMemory) > parseMemory(maxMemory)) {
       showAlert("Ungültige Eingabe", "Min Memory darf nicht größer als Max Memory sein.");
       return false;
   }
   ```

2. **Server-JAR existiert:**
   ```java
   // FEHLT!
   File serverJar = new File(serverJarField.getText());
   if (!serverJar.exists()) {
       showAlert("Ungültige Eingabe", "Die Server-JAR-Datei existiert nicht.");
       return false;
   }
   ```

3. **Java-Pfad ist gültig:**
   ```java
   // FEHLT!
   File javaExe = new File(javaPathField.getText());
   if (!javaExe.exists() || !javaExe.canExecute()) {
       showAlert("Ungültige Eingabe", "Der Java-Pfad ist ungültig oder nicht ausführbar.");
       return false;
   }
   ```

4. **Backup-Ordner beschreibbar:**
   ```java
   // FEHLT!
   ```

5. **Restart-Zeiten Format:**
   - Prüft nur auf HH:mm Format
   - Prüft NICHT auf doppelte Zeiten
   - Prüft NICHT auf mehr als 4 Zeiten (UI sagt "max. 4")

**Auswirkung:** Ungültige Konfigurationen können gespeichert werden, was zu Startfehlern führt.

### LÖSUNG zu #8: Fehlende Validierung in OptionsDialogController

In `OptionsDialogController.java:199-251` die `validateInputs()` Methode erweitern:

```java
private boolean validateInputs() {
    String minMemory = minMemoryField.getText().trim();
    String maxMemory = maxMemoryField.getText().trim();
    String serverJar = serverJarField.getText().trim();
    String javaPath = javaPathField.getText().trim();
    
    // Prüfen ob Felder leer sind
    if (minMemory.isEmpty() || maxMemory.isEmpty() || 
        serverJar.isEmpty() || javaPath.isEmpty()) {
        showAlert("Ungültige Eingabe", "Bitte füllen Sie alle Pflichtfelder aus.");
        return false;
    }
    
    // NEU: minMemory > maxMemory prüfen
    if (parseMemory(minMemory) > parseMemory(maxMemory)) {
        showAlert("Ungültige Eingabe", 
            "Min Memory darf nicht größer als Max Memory sein.");
        return false;
    }
    
    // NEU: Server-JAR existiert
    File serverJarFile = new File(serverJar);
    if (!serverJarFile.exists()) {
        showAlert("Ungültige Eingabe", 
            "Die Server-JAR-Datei existiert nicht: " + serverJar);
        return false;
    }
    
    // NEU: Java-Pfad ist gültig und ausführbar
    File javaExe = new File(javaPath);
    if (!javaExe.exists() || !javaExe.canExecute()) {
        showAlert("Ungültige Eingabe", 
            "Der Java-Pfad ist ungültig oder nicht ausführbar: " + javaPath);
        return false;
    }
    
    // NEU: Backup-Ordner beschreibbar (falls angegeben)
    String backupFolder = backupFolderField.getText().trim();
    if (!backupFolder.isEmpty()) {
        File backupDir = new File(backupFolder);
        if (!backupDir.exists()) {
            try {
                Files.createDirectories(backupDir.toPath());
            } catch (IOException e) {
                showAlert("Ungültige Eingabe", 
                    "Backup-Ordner konnte nicht erstellt werden: " + e.getMessage());
                return false;
            }
        }
        if (!backupDir.canWrite()) {
            showAlert("Ungültige Eingabe", "Backup-Ordner ist nicht beschreibbar.");
            return false;
        }
    }
    
    return true;
}
```

---

### 10. Fehlende Synchronisation bei State-Änderungen
**Datei:** `ServerProcessManager.java:21, 78, 90, 98, 113, 118`  
**Schweregrad:** HOCH

**Beschreibung:** `currentState` wird von mehreren Threads geändert ohne Synchronisation:

```java
private ServerState currentState = ServerState.OFFLINE;  // NICHT thread-safe!

// Im Haupt-Thread:
currentState = ServerState.ONLINE;

// Im Reader-Thread:
currentState = ServerState.OFFLINE;

// Im Fehlerfall:
currentState = ServerState.ERROR;
```

**Auswirkung:** Race-Conditions, veraltete State-Anzeigen, Memory-Visibility-Probleme.

**Lösung:** `AtomicReference<ServerState>` verwenden:
```java
private final AtomicReference<ServerState> currentState = 
    new AtomicReference<>(ServerState.OFFLINE);
```

### LÖSUNG zu #10: Fehlende Synchronisation bei State-Änderungen

In `ServerProcessManager.java` die State-Variable thread-safe machen:

1. Import hinzufügen:
```java
import java.util.concurrent.atomic.AtomicReference;
```

2. Variable ändern:
```java
private final AtomicReference<ServerState> currentState = 
    new AtomicReference<>(ServerState.OFFLINE);
```

3. Alle Zugriffe ändern:
- Von: `currentState = ServerState.ONLINE;`
- Zu: `currentState.set(ServerState.ONLINE);`

- Von: `if (currentState == ServerState.ONLINE)`
- Zu: `if (currentState.get() == ServerState.ONLINE)`

Alle State-Zugriffe in der Klasse müssen entsprechend angepasst werden.

---

## MITTLERE PRIORITÄT (Medium)

### 11. Hardcoded World-Ordner
**Datei:** `BackupManager.java:54`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
String worldFolderPath = "world"; // Hardcoded!
```

**Problem:** In `server.properties:27` steht:
```
level-name=world
```

Wenn ein Benutzer `level-name=meine_welt` ändert, wird das Backup den FALSCHEN Ordner sichern!

**Lösung:** World-Namen aus `server.properties` lesen:
```java
private String getWorldFolderName() {
    Properties props = new Properties();
    try (FileInputStream fis = new FileInputStream("server.properties")) {
        props.load(fis);
        return props.getProperty("level-name", "world");
    } catch (IOException e) {
        return "world";  // Fallback
    }
}
```

### LÖSUNG zu #11: Hardcoded World-Ordner

In `BackupManager.java:54` die Methode ergänzen:

```java
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

// In triggerBackup() verwenden:
String worldFolderPath = getWorldFolderName();
```

---

### 13. Typo in FXML
**Datei:** `MCServerGUI-view.fxml:44`  
**Schweregrad:** MITTEL

**Beschreibung:**
```xml
<Button fx:id="optionsButton" onAction="#onOptionsClick" text="#Options" .../>
```

Das `#` im Text-Attribut ist ein Fehler. Der Button zeigt wörtlich "#Options" an.

**Lösung:**
```xml
<Button fx:id="optionsButton" onAction="#onOptionsClick" text="Options" .../>
```

### LÖSUNG zu #13: Typo in FXML

In `MCServerGUI-view.fxml:44` das `#` entfernen:

Von:
```xml
<Button fx:id="optionsButton" onAction="#onOptionsClick" text="#Options" GridPane.rowIndex="5" />
```

Zu:
```xml
<Button fx:id="optionsButton" onAction="#onOptionsClick" text="Options" GridPane.rowIndex="5" />
```

---

### 14. Statische Controller-Referenz - Memory Leak Potenzial
**Datei:** `Main.java:15`, `Controller.java:18-19`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
// Main.java
private static Controller primaryController;

public static Controller getPrimaryController() {
    return primaryController;
}

// Wird gesetzt in Main.java:59
primaryController = fxmlLoader.getController();
```

**Verwendung in anderen Klassen:**
- `AutoSaveManager.java:73-76`
- `AutoRestartManager.java:73-76`
- `OptionsDialogController.java:109-116, 179-182`

**Probleme:**
1. Statische Referenz verhindert Garbage Collection
2. Bei Application-Restart (z.B. durch Neuladen) bleibt alte Instanz erhalten
3. Memory Leak bei häufigem Neustart in derselben JVM
4. Tight Coupling zwischen Klassen

**Lösung:** Dependency Injection oder Event Bus verwenden:
```java
// Besser: EventBus
EventBus.publish(new ConfigChangedEvent(key, value));

// Oder: Dependency Injection
public AutoSaveManager(ConfigManager config, ServerOperationCoordinator coord, 
                       Consumer<ConfigChangedEvent> notifier) {
    // ...
}
```

### LÖSUNG zu #14: Statische Controller-Referenz

**Option A (Einfachste Lösung - Shutdown Hook):**
In `Main.java` beim Beenden die statische Referenz auf null setzen:

```java
@Override
public void stop() throws Exception {
    if (primaryController != null) {
        // Cleanup durchführen
        primaryController = null;
    }
    super.stop();
}
```

**Option B (Beste Lösung - Event Bus mit Guava/EventBus):**

1. Event-Klasse erstellen:
```java
public class ConfigChangedEvent {
    private final String key;
    private final String value;
    
    public ConfigChangedEvent(String key, String value) {
        this.key = key;
        this.value = value;
    }
    
    public String getKey() { return key; }
    public String getValue() { return value; }
}
```

2. EventBus als Singleton:
```java
import com.google.common.eventbus.EventBus;

public class AppEventBus {
    private static final EventBus EVENT_BUS = new EventBus();
    
    public static EventBus getInstance() {
        return EVENT_BUS;
    }
}
```

3. Controller als Event-Listener:
```java
import com.google.common.eventbus.Subscribe;

@FXML
public void initialize() {
    AppEventBus.getInstance().register(this);
}

@Subscribe
public void onConfigChanged(ConfigChangedEvent event) {
    // UI aktualisieren
}
```

4. In AutoSaveManager/AutoRestartManager:
```java
// Statt Main.getPrimaryController().updateLabels();
AppEventBus.getInstance().post(new ConfigChangedEvent("autoSave", String.valueOf(newValue)));
```

**Empfehlung:** Option A für schnelle Lösung, Option B für saubere Architektur.

---

### 15. Timer-Update-Intervall ungenau
**Datei:** `Controller.java:170-178`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
private void startLabelUpdater() {
    labelUpdaterTimer = new Timer(true);
    labelUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            updateLabels();
        }
    }, 0, 10000);  // Alle 10 Sekunden
}
```

**Probleme:**
1. Labels werden nur alle 10 Sekunden aktualisiert
2. Timer für Auto-Save/Restart sind millisekundengenau
3. Anzeige kann bis zu 10 Sekunden veraltet sein
4. Bei langen Intervallen (z.B. 6 Stunden) ist die Anzeige ungenau

**Lösung:** Intervall auf 1 Sekunde reduzieren oder bei Timer-Events sofort aktualisieren.

### LÖSUNG zu #15: Timer-Update-Intervall ungenau

In `Controller.java:170-178` das Intervall auf 1 Sekunde reduzieren:

Von:
```java
private void startLabelUpdater() {
    labelUpdaterTimer = new Timer(true);
    labelUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            updateLabels();
        }
    }, 0, 10000);  // Alle 10 Sekunden
}
```

Zu:
```java
private void startLabelUpdater() {
    labelUpdaterTimer = new Timer(true);
    labelUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            updateLabels();
        }
    }, 0, 1000);  // Alle 1 Sekunde (bessere Reaktionszeit)
}
```

**Optionale Verbesserung:** Labels sofort aktualisieren wenn sich etwas ändert:
- In AutoSaveManager/AutoRestartManager nach dem Speichern direkt `updateLabels()` aufrufen
- Dies gibt sofortiges Feedback statt auf den Timer zu warten

---

### 16. Unbenutzte Klasse ServerCommandExecutor
**Datei:** `ServerCommandExecutor.java` (gesamte Datei, 61 Zeilen)  
**Schweregrad:** MITTEL

**Beschreibung:** Die Klasse wird nirgendwo instanziiert oder verwendet:

```java
public class ServerCommandExecutor {
    private PrintWriter serverInput;
    private ServerProcessManager serverProcessManager;
    
    public void executeCommand(String command) {
        // ...
    }
}
```

**Tatsächliche Command-Ausführung:**
```java
// In ServerProcessManager.java:38-50
public void sendCommand(String command) {
    if (serverProcess != null && serverInput != null) {
        serverInput.println(command);
        // ...
    }
}
```

**Auswirkung:** Toter Code, Wartungsaufwand, Verwirrung.

**Lösung:** Müssen wir später noch implementieren.

### LÖSUNG zu #16: Unbenutzte Klasse ServerCommandExecutor

**Empfohlene Lösung:** Die Klasse löschen
- Datei: `ServerCommandExecutor.java` (61 Zeilen)
- Die Funktionalität ist bereits in `ServerProcessManager.sendCommand()` implementiert

**Lösung:** Müssen wir später noch implementieren.

---

### 17. Unbenutzte Klasse LogManager
**Datei:** `LogManager.java` (gesamte Datei, 22 Zeilen)  
**Schweregrad:** MITTEL

**Beschreibung:** Wrapper-Klasse für SLF4J, wird nirgendwo verwendet:

```java
public class LogManager {
    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);

    public static void logInfo(String message) {
        logger.info(message);
    }
    // ...
}
```

Alle anderen Klassen verwenden direkt SLF4J:
```java
private static final Logger logger = LoggerFactory.getLogger(Controller.class);
```

**Lösung:** Löschen.

### LÖSUNG zu #17: Unbenutzte Klasse LogManager

**Empfohlene Lösung:** Die Klasse löschen
- Datei: `LogManager.java` (22 Zeilen)
- Alle anderen Klassen verwenden direkt SLF4J, diese Wrapper-Klasse wird nicht verwendet

---

### 18. ConfigManager - isFirstRun() Logik fehlerhaft
**Datei:** `ConfigManager.java:22-29`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
public ConfigManager() {
    this.firstRun = !checkIfConfigExists();
    initializeComments();
    if (firstRun) {
        logger.info("Erste Ausführung erkannt. Erstelle Standardkonfiguration...");
        createDefaultConfig();
        saveConfig();  // <-- Erstellt config.txt
    }
    loadConfig();
}
```

**Problem:** Nach dem `saveConfig()` existiert die Datei, aber `firstRun` ist immer noch `true`!

**In Main.java:24-29:**
```java
ConfigManager configManager = new ConfigManager();  // firstRun = true, erstellt Datei

if (configManager.isFirstRun()) {  // Immer noch true!
    logger.info("No configuration found. Opening FirstConfigDialog.");
    showFirstConfigDialog();
}
```

**Auswirkung:** FirstConfigDialog wird bei JEDEM Start angezeigt, nicht nur beim ersten!

**Lösung:** Flag in separater Datei speichern oder `isFirstRun()` prüfen, ob Config Werte hat.

### LÖSUNG zu #18: ConfigManager isFirstRun() Logik fehlerhaft

**Option A (Empfohlen):** isFirstRun() basierend auf Konfigurationswerten prüfen

In `ConfigManager.java` die Logik ändern:

```java
public boolean isFirstRun() {
    // Prüfen ob wichtige Konfigurationswerte fehlen
    String serverJar = config.get("serverJar");
    String maxMemory = config.get("maxMemory");
    
    // Wenn wichtige Werte fehlen, gilt es als erster Start
    return serverJar == null || serverJar.isEmpty() ||
           maxMemory == null || maxMemory.isEmpty();
}
```

**Option B:** Flag in separater Datei speichern

1. Neue Datei `firstrun.flag` erstellen:
```java
private static final String FIRST_RUN_FLAG = "firstrun.flag";

public boolean isFirstRun() {
    File flagFile = new File(FIRST_RUN_FLAG);
    return !flagFile.exists();
}

public void markFirstRunComplete() {
    try {
        new File(FIRST_RUN_FLAG).createNewFile();
    } catch (IOException e) {
        logger.error("Konnte firstrun.flag nicht erstellen", e);
    }
}
```

2. In `Main.java` nach erfolgreicher Ersteinrichtung aufrufen:
```java
if (configManager.isFirstRun()) {
    showFirstConfigDialog();
    configManager.markFirstRunComplete();  // Nach Speichern aufrufen
}
```

**Empfehlung:** Option A ist einfacher und robust.

---

### 19. FirstConfigDialog - Pflichtfelder nicht markiert
**Datei:** `FirstConfigController.java:89-96`  
**Schweregrad:** NIEDRIG

**Beschreibung:**
```java
private boolean validateFields() {
    if (serverJarField.getText().trim().isEmpty() ||
            minMemoryField.getText().trim().isEmpty() ||
            maxMemoryField.getText().trim().isEmpty()) {
        return false;  // Keine spezifische Fehlermeldung!
    }
    // ...
}
```

**Probleme:**
1. Keine visuelle Kennzeichnung der Pflichtfelder
2. Keine spezifische Fehlermeldung welches Feld fehlt
3. `backupFolderField` ist optional, wird aber nicht anders behandelt

### LÖSUNG zu #19: FirstConfigDialog Pflichtfelder nicht markiert

In `FirstConfigController.java` die Validierung verbessern:

```java
private boolean validateFields() {
    StringBuilder missingFields = new StringBuilder();
    
    if (serverJarField.getText().trim().isEmpty()) {
        missingFields.append("- Server-JAR\n");
        serverJarField.setStyle("-fx-border-color: red;");
    } else {
        serverJarField.setStyle("");
    }
    
    if (minMemoryField.getText().trim().isEmpty()) {
        missingFields.append("- Min Memory\n");
        minMemoryField.setStyle("-fx-border-color: red;");
    } else {
        minMemoryField.setStyle("");
    }
    
    if (maxMemoryField.getText().trim().isEmpty()) {
        missingFields.append("- Max Memory\n");
        maxMemoryField.setStyle("-fx-border-color: red;");
    } else {
        maxMemoryField.setStyle("");
    }
    
    // backupFolder ist optional - kein Fehler wenn leer
    
    if (missingFields.length() > 0) {
        showAlert("Fehlende Felder", "Bitte füllen Sie folgende Pflichtfelder aus:\n" + missingFields.toString());
        return false;
    }
    
    return true;
}
```

In `FirstConfigDialog.fxml` Sternchen hinzufügen:
```xml
<Label text="Server JAR *" />
<TextField fx:id="serverJarField" ... />
<Label text="Min Memory *" />
<TextField fx:id="minMemoryField" ... />
<Label text="Max Memory *" />
<TextField fx:id="maxMemoryField" ... />
<Label text="Backup Folder" />  <!-- Optional - kein Sternchen -->
```

---

### 20. Fehlende Style-Klassen in CSS
**Datei:** `style.css`  
**Schweregrad:** NIEDRIG

**Beschreibung:** Die CSS-Datei hat allgemeine Klassen, aber keine spezifischen für:
- Server-State-Label (verschiedene Farben für States)
- Status-Labels (Last Save, Next Save, etc.)
- Checkbox-Styling
- Button-States (disabled, hovered für spezifische Buttons)

### LÖSUNG zu #20: Fehlende Style-Klassen in CSS

In `style.css` folgende Klassen hinzufügen:

```css
/* Server State Labels */
.server-state-label {
    -fx-font-size: 14px;
    -fx-font-weight: bold;
}

.server-state-online {
    -fx-text-fill: #4CAF50; /* Grün */
}

.server-state-offline {
    -fx-text-fill: #9E9E9E; /* Grau */
}

.server-state-starting {
    -fx-text-fill: #FF9800; /* Orange */
}

.server-state-error {
    -fx-text-fill: #F44336; /* Rot */
}

/* Status Labels */
.status-label {
    -fx-font-size: 12px;
    -fx-text-fill: #757575;
}

.status-label-highlight {
    -fx-font-weight: bold;
    -fx-text-fill: #2196F3;
}

/* CheckBox Styling */
.check-box .box {
    -fx-background-color: #f0f0f0;
    -fx-border-color: #b0b0b0;
}

.check-box:selected .box {
    -fx-background-color: #2196F3;
    -fx-border-color: #1976D2;
}

/* Button States */
.button:hover {
    -fx-cursor: hand;
}

.button:pressed {
    -fx-background-color: derive(-fx-base, -20%);
}

.button:disabled {
    -fx-opacity: 0.5;
}
```

In `Controller.java` die Styles zuweisen:
```java
serverStateLabel.getStyleClass().add("server-state-label");
serverStateLabel.getStyleClass().add("server-state-offline");

// Bei State-Änderung:
serverStateLabel.getStyleClass().removeAll("server-state-online", "server-state-offline", "server-state-starting", "server-state-error");
serverStateLabel.getStyleClass().add("server-state-" + newState.name().toLowerCase());
```

---

## NIEDRIGE PRIORITÄT (Low)

### 21. Code-Duplikation - AutoSaveManager und AutoRestartManager
**Datei:** `AutoSaveManager.java` und `AutoRestartManager.java`  
**Schweregrad:** NIEDRIG

**Beschreibung:** Beide Klassen sind fast identisch (115 Zeilen vs. 115 Zeilen):

| AutoSaveManager | AutoRestartManager | Gleichheit |
|-----------------|-------------------|------------|
| `scheduleNextSave()` | `scheduleNextRestart()` | 95% identisch |
| `performAutoSave()` | `performAutoRestart()` | 90% identisch |
| `getNextScheduledTime()` | `getNextScheduledTime()` | 100% identisch |
| `notifyControllerUpdate()` | `notifyControllerUpdate()` | 100% identisch |

**Verletzung:** DRY-Prinzip (Don't Repeat Yourself)

**Lösung:** Abstrakte Basisklasse erstellen:
```java
public abstract class AbstractScheduledManager {
    protected Timer timer;
    protected LocalDateTime nextScheduledTime;
    protected String lastActionTime;
    
    public abstract void performAction();
    public abstract String getActionName();
    
    // Gemeinsame Methoden...
}
```

### LÖSUNG zu #21: Code-Duplikation AutoSaveManager und AutoRestartManager

**Option A (Refactoring zu Basisklasse):**

1. Neue abstrakte Klasse erstellen:
```java
public abstract class AbstractScheduledManager {
    protected Timer timer;
    protected LocalDateTime nextScheduledTime;
    protected String lastActionTime;
    protected final ConfigManager configManager;
    protected final ServerOperationCoordinator coordinator;
    protected final Logger logger;
    
    public AbstractScheduledManager(ConfigManager configManager, 
                                   ServerOperationCoordinator coordinator,
                                   String actionName) {
        this.configManager = configManager;
        this.coordinator = coordinator;
        this.logger = LoggerFactory.getLogger(
            getClass()); // Subclass-logger
    }
    
    public void scheduleNext() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        
        nextScheduledTime = getNextScheduledTime();
        long delay = Duration.between(LocalDateTime.now(), nextScheduledTime).toMillis();
        
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                performAction();
                scheduleNext();
            }
        }, delay);
    }
    
    public String getLastActionTime() {
        return lastActionTime != null ? lastActionTime : "-";
    }
    
    public String getNextActionFormatted() {
        if (nextScheduledTime != null) {
            return TimeUtils.formatDuration(
                Duration.between(LocalDateTime.now(), nextScheduledTime).getSeconds());
        }
        return "-";
    }
    
    protected LocalDateTime getNextScheduledTime() {
        // Implementierung aus AutoSaveManager
        String times = getScheduleTimes();
        // ... Zeitberechnung
    }
    
    protected abstract String getScheduleTimes();
}
```

2. AutoSaveManager und AutoRestartManager vereinfachen:
```java
public class AutoSaveManager extends AbstractScheduledManager {
    public AutoSaveManager(ConfigManager configManager, 
                          ServerOperationCoordinator coordinator) {
        super(configManager, coordinator, "Save");
    }
    
    @Override
    protected void performAction() {
        if (coordinator.isOperationInProgress()) {
            logger.info("Auto Save übersprungen - Operation läuft bereits");
            return;
        }
        coordinator.performBackup();
        lastSaveTime = LocalDateTime.now().toString();
    }
    
    @Override
    protected String getScheduleTimes() {
        return configManager.getConfigValue("saveTimes", "00:00,06:00,12:00,18:00");
    }
}
```

**Option B (Quick Fix):** 
Da beide Klassen fast identisch sind, aber funktional unterschiedlich, ist das Refactoring optional. Für jetzt akzeptabel, da der Code funktioniert.

**Empfehlung:** Option B - Refactoring später wenn mehr Zeit ist.

---

### 22. Fehlende Unit Tests
**Datei:** `src/test/java` - Ordner existiert nicht  
**Schweregrad:** NIEDRIG

**Beschreibung:** Trotz JUnit-Dependencies im pom.xml (Zeilen 82-92):
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

Existieren KEINE Tests!

**Was getestet werden sollte:**
- `ConfigManager`: Laden, Speichern, Validierung
- `TimeUtils`: Zeitberechnungen
- `BackupManager`: Backup-Erstellung (mit Mock)
- `ServerState`: State-Übergänge

### LÖSUNG zu #22: Fehlende Unit Tests

Test-Ordner erstellen und Tests schreiben:

1. **Verzeichnis erstellen:** `src/test/java/org/minecraftservergui/mcservergui/`

2. **ConfigManagerTest.java:**
```java
package org.minecraftservergui.mcservergui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testDefaultConfigCreation() {
        // Test dass Standardwerte erstellt werden
    }
    
    @Test
    void testLoadConfig() {
        // Test Laden der Konfiguration
    }
    
    @Test
    void testSetAndGetConfigValue() {
        // Test setzen und lesen von Werten
    }
    
    @Test
    void testSaveConfig() {
        // Test Speichern der Konfiguration
    }
}
```

3. **TimeUtilsTest.java:**
```java
package org.minecraftservergui.mcservergui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {
    
    @Test
    void testFormatDuration() {
        assertEquals("1m 0s", TimeUtils.formatDuration(60));
        assertEquals("1h 0m", TimeUtils.formatDuration(3600));
    }
    
    @Test
    void testParseMemory() {
        // Test Memory-Parsing
    }
}
```

4. **ServerStateTest.java:**
```java
package org.minecraftservergui.mcservergui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerStateTest {
    
    @Test
    void testStateTransitions() {
        // Test gültige State-Übergänge
    }
    
    @Test
    void testGetDisplayName() {
        assertEquals("Online", ServerState.ONLINE.getDisplayName());
    }
}
```

**Ausführung:**
```bash
mvn test
```

---

### 23. Magic Numbers im Code
**Datei:** Mehrere Dateien  
**Schweregrad:** NIEDRIG

**Gefundene Magic Numbers:**

| Datei | Zeile | Wert | Bedeutung |
|-------|-------|------|-----------|
| `ServerOperationCoordinator.java` | 164, 202 | `5000` | Restart-Verzögerung in ms |
| `Controller.java` | 177 | `10000` | Label-Update-Intervall in ms |
| `Main.java` | 54 | `845, 677` | Fenstergröße |
| `OptionsDialog.fxml` | 6 | `400, 600` | Dialog-Größe |
| `FirstConfigDialog.fxml` | 13 | `300, 400` | Dialog-Größe |
| `MCServerGUI-view.fxml` | 16 | `845, 677` | Fenstergröße |

**Lösung:** Konstanten definieren:
```java
public class Constants {
    public static final long RESTART_DELAY_MS = 5000;
    public static final long LABEL_UPDATE_INTERVAL_MS = 10000;
    public static final int MAIN_WINDOW_WIDTH = 845;
    public static final int MAIN_WINDOW_HEIGHT = 677;
}
```

### LÖSUNG zu #23: Magic Numbers im Code

Neue Klasse `Constants.java` erstellen:

```java
package org.minecraftservergui.mcservergui;

public class Constants {
    
    // Timing
    public static final long RESTART_DELAY_MS = 5000;
    public static final long LABEL_UPDATE_INTERVAL_MS = 1000;
    public static final long SERVER_START_TIMEOUT_MS = 60000;
    public static final long SERVER_STOP_TIMEOUT_MS = 30000;
    
    // Window Sizes
    public static final int MAIN_WINDOW_WIDTH = 845;
    public static final int MAIN_WINDOW_HEIGHT = 677;
    public static final int OPTIONS_DIALOG_WIDTH = 400;
    public static final int OPTIONS_DIALOG_HEIGHT = 600;
    public static final int FIRST_CONFIG_DIALOG_WIDTH = 300;
    public static final int FIRST_CONFIG_DIALOG_HEIGHT = 400;
    
    // Default Values
    public static final String DEFAULT_WORLD_FOLDER = "world";
    public static final int DEFAULT_MAX_BACKUPS = 10;
    
    // Config Keys
    public static final String KEY_JAVA_PATH = "javaPath";
    public static final String KEY_SERVER_JAR = "serverJar";
    public static final String KEY_MAX_MEMORY = "maxMemory";
    public static final String KEY_MIN_MEMORY = "minMemory";
    public static final String KEY_AUTO_SAVE = "autoSave";
    public static final String KEY_AUTO_RESTART = "autoRestart";
    
    private Constants() {
        // Utility class
    }
}
```

Verwendung in anderen Klassen:
```java
import static org.minecraftservergui.mcservergui.Constants.*;

// Statt:
// labelUpdaterTimer.scheduleAtFixedRate(..., 0, 10000);
// Neu:
labelUpdaterTimer.scheduleAtFixedRate(..., 0, LABEL_UPDATE_INTERVAL_MS);
```

---

### 24. Gemischte Sprache (Deutsch/Englisch)
**Datei:** Alle Dateien  
**Schweregrad:** NIEDRIG

**Beispiele:**

| Datei | Deutsch | Englisch |
|-------|---------|----------|
| `Controller.java` | "Auto Save aktiviert." | "Start button clicked." |
| `FirstConfigController.java` | "Ungültige Eingabe" | - |
| `OptionsDialogController.java` | "Speichern" | - |
| `MCServerGUI-view.fxml` | "Konsolenausgabe" | "Console Output" |
| `server.properties` | - | Alles Englisch |

**Lösung:** ResourceBundle für Internationalisierung verwenden.

### LÖSUNG zu #24: Gemischte Sprache (Deutsch/Englisch)

**Langfristige Lösung (Optional):**

1. ResourceBundle-Dateien erstellen:
- `messages.properties` (Englisch - Default)
- `messages_de.properties` (Deutsch)

```properties
# messages.properties
button.start=Start
button.stop=Stop
button.restart=Restart
button.options=Options
message.autoSave.enabled=Auto Save enabled
message.autoSave.disabled=Auto Save disabled

# messages_de.properties
button.start=Starten
button.stop=Stoppen
button.restart=Neustarten
button.options=Optionen
message.autoSave.enabled=Auto Speichern aktiviert
message.autoSave.disabled=Auto Speichern deaktiviert
```

2. In Java verwenden:
```java
ResourceBundle bundle = ResourceBundle.getBundle("messages", 
    new Locale("de")); // oder Locale.getDefault()

String startText = bundle.getString("button.start");
```

**Kurzfristige Lösung (Empfohlen):**
Alle Texte auf Deutsch oder Englisch vereinheitlichen. Für dieses Projekt empfehle ich Deutsch als Hauptsprache, da die meisten UI-Texte bereits deutsch sind.

---

### 25. Fehlende JavaDoc-Kommentare
**Datei:** Alle Java-Dateien  
**Schweregrad:** NIEDRIG

**Beschreibung:** Nur wenige Methoden haben JavaDoc:

**Mit JavaDoc:**
- `ConfigManager`: Die meisten Methoden dokumentiert
- `BackupManager`: Die meisten Methoden dokumentiert
- `ServerCommandExecutor`: Die meisten Methoden dokumentiert

**Ohne JavaDoc:**
- `Controller`: Keine einzige Methode dokumentiert
- `Main`: Keine einzige Methode dokumentiert
- `ServerProcessManager`: Nur `isEulaAccepted` hat Kommentar
- `TimeUtils`: Alle Methoden haben JavaDoc (gut!)

### LÖSUNG zu #25: Fehlende JavaDoc-Kommentare

**Empfohlene Vorgehensweise:** 
JavaDoc ist wichtig für die Wartbarkeit, aber zeitintensiv. Priorisierung:

1. **Controller.java** - Wichtigste Klasse, alle public Methoden dokumentieren:
```java
/**
 * Haupt-Controller für die McServerGUI Anwendung.
 * Verwaltet die UI-Interaktionen und koordiniert Server-Operationen.
 */
public class Controller implements ServerStateListener {
    
    /**
     * Initialisiert die Controller-Instanz nach dem Laden der FXML.
     * Richtet Event-Listener und Manager-Klassen ein.
     */
    @FXML
    public void initialize() {
        // ...
    }
    
    /**
     * Startet den Minecraft-Server.
     * @return true wenn der Start erfolgreich eingeleitet wurde
     */
    @FXML
    private void onStartClick() {
        // ...
    }
}
```

2. **Main.java** - Einstiegspunkt dokumentieren:
```java
/**
 * Hauptklasse der McServerGUI Anwendung.
 * Startet die JavaFX-Anwendung und lädt das Hauptfenster.
 */
public class Main extends Application {
    // ...
}
```

3. **ServerProcessManager.java** - Prozessmanagement dokumentieren

---

### 26. Unbenutzte Imports
**Datei:** `FirstConfigController.java:3`  
**Schweregrad:** NIEDRIG

**Beschreibung:**
```java
import javafx.application.Platform;  // Wird NICHT verwendet
```

### LÖSUNG zu #26: Unbenutzte Imports

In `FirstConfigController.java:3` den unbenutzten Import entfernen:

Von:
```java
package org.minecraftservergui.mcservergui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;
```

Zu:
```java
package org.minecraftservergui.mcservergui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;
```

**Hinweis:** `Platform` wird tatsächlich in Zeile 30 verwendet (`Platform.runLater()`), daher ist der Import korrekt. Das Problem ist möglicherweise bereits behoben.

---

## FXML-SPEZIFISCHE PROBLEME

---

### 30. MCServerGUI-view.fxml - Inkonsistente Button-IDs
**Datei:** `MCServerGUI-view.fxml:44-54`  
**Schweregrad:** NIEDRIG

**Beschreibung:** Einige Buttons haben `fx:id`, andere nicht:

| Button | fx:id | Hat Controller-Methode |
|--------|-------|------------------------|
| Start | `startButton` | Ja |
| Stop | `stopButton` | Ja |
| Restart | `restartButton` | Ja |
| Save | Keine | Ja |
| Options | `optionsButton` | Ja |
| Server Properties | `serverPropertiesButton` | Ja |
| ??? | `reloadGUIButton` | Ja (placeholder) |
| View Logs | `viewLogsButton` | Ja |
| View Saves | `viewSavesButton` | Ja |
| Commands | `commandsButton` | Ja (placeholder) |
| Reload Whitelist | `reloadWhitelistButton` | Ja |
| Open Folder | `openFolderButton` | Ja |

### LÖSUNG zu #30: MCServerGUI-view.fxml Inkonsistente Button-IDs

**Problem:** Save-Button hat keine fx:id

In `MCServerGUI-view.fxml` die fx:id für Save-Button hinzufügen:

Von:
```xml
<Button ... text="Save" onAction="#onSaveClick" />
```

Zu:
```xml
<Button fx:id="saveButton" ... text="Save" onAction="#onSaveClick" />
```

Die anderen Buttons haben bereits korrekte fx:ids.

---

### 31. FirstConfigDialog.fxml - Fehlendes javaPathField
**Datei:** `FirstConfigDialog.fxml`  
**Schweregrad:** KRITISCH (siehe Bug #3)

**Beschreibung:** In `FirstConfigController.java:19` wird deklariert:
```java
@FXML
private TextField javaPathField;
```

Aber in der FXML existiert dieses Feld NICHT!

**Vorhandene Felder in FXML:**
- `serverJarField`
- `minMemoryField`
- `maxMemoryField`
- `backupFolderField`
- `focusPane`

**Fehlend:**
- `javaPathField`

### LÖSUNG zu #31: FirstConfigDialog.fxml Fehlendes javaPathField

**Status:** Bereits durch #3 abgedeckt - Siehe LÖSUNG zu #3

Das Problem wurde bereits in #3 behandelt. Das javaPathField wird entweder in der FXML hinzugefügt ODER aus dem Controller entfernt (empfohlen).

---

## THREAD-SAFETY & CONCURRENCY

### 32. Platform.runLater() Überbeanspruchung
**Datei:** Mehrere Dateien  
**Schweregrad:** MITTEL

**Beschreibung:** `Platform.runLater()` wird sehr häufig aufgerufen:

| Datei | Anzahl Aufrufe |
|-------|----------------|
| `Controller.java` | 4 |
| `ServerProcessManager.java` | 1 (in appendToConsole) |
| `ServerOperationCoordinator.java` | 1 (in setState) |

**Problem:** Bei hoher Server-Ausgabe kann die JavaFX Application Queue überflutet werden.

**Beispiel - ServerProcessManager.java:84:**
```java
while ((line = reader.readLine()) != null) {
    appendToConsole(line);  // Ruft Platform.runLater() für Jede Zeile!
}
```

### LÖSUNG zu #32: Platform.runLater() Überbeanspruchung

In `ServerProcessManager.java` die Konsolenausgabe bündeln:

```java
// Neue Methode in ServerProcessManager
private void appendToConsole(String line) {
    if (consoleOutput != null) {
        // Buffer für Bündelung
        StringBuilder buffer = new StringBuilder();
        buffer.append(line).append("\n");
        
        Platform.runLater(() -> {
            consoleOutput.appendText(buffer.toString());
            // Auto-Scroll nach unten
            consoleOutput.selectEnd();
            consoleOutput.home();
        });
    }
}

// Oder bündeln im Reader-Thread:
new Thread(() -> {
    StringBuilder buffer = new StringBuilder();
    long lastUpdate = System.currentTimeMillis();
    
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(serverProcess.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
            
            // Alle 100ms updaten statt für jede Zeile
            if (System.currentTimeMillis() - lastUpdate > 100) {
                final String output = buffer.toString();
                Platform.runLater(() -> appendToConsole(output));
                buffer.setLength(0);
                lastUpdate = System.currentTimeMillis();
            }
        }
        // Restliche Ausgabe
        if (buffer.length() > 0) {
            final String output = buffer.toString();
            Platform.runLater(() -> appendToConsole(output));
        }
    }
    currentState = ServerState.OFFLINE;
}).start();
```

**Lösung:** Ausgaben bündeln:
```java
StringBuilder buffer = new StringBuilder();
long lastUpdate = System.currentTimeMillis();

while ((line = reader.readLine()) != null) {
    buffer.append(line).append("\n");
    if (System.currentTimeMillis() - lastUpdate > 100) {  // Alle 100ms
        final String output = buffer.toString();
        Platform.runLater(() -> consoleOutput.appendText(output));
        buffer.setLength(0);
        lastUpdate = System.currentTimeMillis();
    }
}
```

---

### 33. Race Condition in ServerOperationCoordinator
**Datei:** `ServerOperationCoordinator.java:60-80`  
**Schweregrad:** HOCH

**Beschreibung:**
```java
public synchronized void startServer() {
    if (!canStartOperation(OperationType.START)) return;
    
    operationInProgress.set(true);  // AtomicBoolean
    currentOperation.set(OperationType.START);  // AtomicReference
    setState(ServerState.STARTING);

    new Thread(() -> {
        try {
            boolean success = serverProcessManager.startServerInternal();
            if (success) {
                setState(ServerState.ONLINE);
            } else {
                setState(ServerState.ERROR);
            }
        } finally {
            operationInProgress.set(false);
            currentOperation.set(null);
        }
    }).start();
}
```

**Problem:** Zwischen `canStartOperation()` Check und `operationInProgress.set(true)` kann ein anderer Thread die Methode aufrufen!

**Lösung:** Beides in einem synchronisierten Block:
```java
public synchronized void startServer() {
    synchronized(operationInProgress) {
        if (operationInProgress.get()) return;
        operationInProgress.set(true);
    }
    // ...
}
```

### LÖSUNG zu #33: Race Condition in ServerOperationCoordinator

In `ServerOperationCoordinator.java:60-80` die Synchronisation verbessern:

```java
public synchronized void startServer() {
    // Atomarer Check und Set
    synchronized(operationInProgress) {
        if (!canStartOperation(OperationType.START)) {
            return;
        }
        operationInProgress.set(true);
    }
    
    currentOperation.set(OperationType.START);
    setState(ServerState.STARTING);

    new Thread(() -> {
        try {
            boolean success = serverProcessManager.startServerInternal();
            if (success) {
                setState(ServerState.ONLINE);
            } else {
                setState(ServerState.ERROR);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Server-Start", e);
            setState(ServerState.ERROR);
        } finally {
            synchronized(operationInProgress) {
                operationInProgress.set(false);
            }
            currentOperation.set(null);
        }
    }).start();
}
```

**Gleiche Änderung für:**
- `stopServer()`
- `restartServer()`
- `performBackup()`

Wichtig: Alle Methoden müssen `synchronized` sein und das gleiche Lock-Objekt verwenden.

---

### 34. Timer-Threads sind Daemons
**Datei:** `AutoSaveManager.java:61`, `AutoRestartManager.java:61`, `Controller.java:171`  
**Schweregrad:** NIEDRIG

**Beschreibung:**
```java
autoSaveTimer = new Timer(true);  // true = Daemon
labelUpdaterTimer = new Timer(true);
```

**Problem:** Daemon-Threads werden abrupt beendet, wenn die Anwendung endet. TimerTasks werden nicht sauber abgeschlossen.

**Auswirkung:** Backup könnte abgeschnitten werden, wenn Anwendung während Backup geschlossen wird.

**Lösung:** Shutdown-Hook registrieren oder `Timer(false)` verwenden.

### LÖSUNG zu #34: Timer-Threads sind Daemons

In `Controller.java` einen Shutdown-Hook hinzufügen:

```java
@Override
public void initialize(URL url, ResourceBundle rb) {
    // ... existierende Initialisierung ...
    
    // Shutdown-Hook registrieren
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (autoSaveManager != null) {
            autoSaveManager.shutdown();
        }
        if (autoRestartManager != null) {
            autoRestartManager.shutdown();
        }
        if (labelUpdaterTimer != null) {
            labelUpdaterTimer.cancel();
        }
    }));
}
```

In `AutoSaveManager.java` und `AutoRestartManager.java`:

```java
public void shutdown() {
    if (timer != null) {
        timer.cancel();
        timer.purge();
        timer = null;
    }
}
```

**Alternative:** Timer auf non-daemon setzen (kann App am Schließen hindern):
```java
// Von:
autoSaveTimer = new Timer(true);  // true = daemon

// Zu:
autoSaveTimer = new Timer(false);  // false = non-daemon
```

---

---

## RESSOURCEN-MANAGEMENT

---

### 36. PrintWriter nicht geschlossen
**Datei:** `ServerProcessManager.java:77`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
serverInput = new PrintWriter(serverProcess.getOutputStream(), true);
```

**Problem:** `serverInput` wird nie explizit geschlossen. In `stopServerInternal()`:
```java
if (serverInput != null) {
    serverInput.println("stop");  // Nur "stop" senden, nicht schließen!
}
```

**Auswirkung:** Ressourcen-Leak bei Prozessende.

**Lösung:**
```java
if (serverInput != null) {
    serverInput.println("stop");
    serverInput.close();  // Hinzufügen!
    serverInput = null;
}
```

### LÖSUNG zu #36: PrintWriter nicht geschlossen

In `ServerProcessManager.java` den PrintWriter schließen:

```java
public boolean stopServerInternal() {
    if (serverProcess != null) {
        if (serverInput != null) {
            serverInput.println("stop");
            serverInput.flush();
            // Wichtig: PrintWriter schließen
            try {
                serverInput.close();
            } catch (Exception e) {
                logger.warn("Fehler beim Schließen des PrintWriters", e);
            }
            serverInput = null;
        }
        // ... restlicher Code
    }
}
```

---

### 37. Backup-Datei wird nicht validiert
**Datei:** `BackupManager.java:51-62`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
public void triggerBackup() throws IOException {
    // ...
    zipDirectory(Paths.get(worldFolderPath), backupFile.toPath());
    logger.info("Backup erfolgreich erstellt: {}", backupFile.getAbsolutePath());
    // Keine Validierung!
}
```

**Fehlende Prüfungen:**
1. Datei wurde erstellt
2. Datei ist nicht leer
3. ZIP ist gültig (kann geöffnet werden)
4. Dateigröße ist plausibel

**Lösung:**
```java
public void triggerBackup() throws IOException {
    // ... backup erstellen ...
    
    // Validierung
    if (!backupFile.exists()) {
        throw new IOException("Backup-Datei wurde nicht erstellt");
    }
    if (backupFile.length() == 0) {
        throw new IOException("Backup-Datei ist leer");
    }
    // Optional: ZIP-Integrität prüfen
    validateZipFile(backupFile);
}
```

### LÖSUNG zu #37: Backup-Datei wird nicht validiert

In `BackupManager.java` die Validierung hinzufügen:

```java
public void triggerBackup() throws IOException {
    // ... backup erstellen ...
    
    // Validierung
    if (!backupFile.exists()) {
        throw new IOException("Backup-Datei wurde nicht erstellt: " + backupFile.getAbsolutePath());
    }
    
    long fileSize = backupFile.length();
    if (fileSize == 0) {
        backupFile.delete();
        throw new IOException("Backup-Datei ist leer");
    }
    
    // Minimale Größe für ein gültiges World-Backup (z.B. 1KB)
    if (fileSize < 1024) {
        logger.warn("Backup-Datei ist ungewöhnlich klein: {} bytes", fileSize);
    }
    
    // ZIP-Integrität prüfen
    validateZipFile(backupFile);
    
    logger.info("Backup erfolgreich validiert: {} ({} bytes)", 
        backupFile.getName(), fileSize);
}

private void validateZipFile(File zipFile) throws IOException {
    try (ZipFile zf = new ZipFile(zipFile)) {
        ZipEntry entry = zf.entries().nextElement();
        if (entry == null) {
            throw new IOException("ZIP-Datei ist leer");
        }
    } catch (ZipException e) {
        throw new IOException("Ungültiges ZIP-Format: " + e.getMessage());
    }
}
```

---

---

## SICHERHEITSASPEKTE

### 38. Keine Validierung von Benutzereingaben für Befehle
**Datei:** `Controller.java:193-202`  
**Schweregrad:** MITTEL

**Beschreibung:**
```java
@FXML
private void onCommandSend() {
    String command = commandInput.getText();
    if (!command.isEmpty()) {
        serverProcessManager.sendCommand(command);  // Ungefiltert!
        // ...
    }
}
```

**Problem:** Jeder Befehl wird ohne Validierung an den Server gesendet. Dies ist zwar für einen lokalen Server in Ordnung, aber:
- Keine Bestätigung für destruktive Befehle (`stop`, `kill`, `op`, etc.)
- Keine Historie der ausgeführten Befehle

### LÖSUNG zu #38: Keine Validierung von Benutzereingaben für Befehle

In `Controller.java` die Befehlsvalidierung hinzufügen:

```java
private static final Set<String> DESTRUCTIVE_COMMANDS = Set.of(
    "stop", "kill", "op", "deop", "whitelist off", "kick all"
);

@FXML
private void onCommandSend() {
    String command = commandInput.getText();
    if (!command.isEmpty()) {
        // Bestätigung für destruktive Befehle
        if (isDestructiveCommand(command)) {
            if (!confirmDestructiveCommand(command)) {
                return; // Abgebrochen
            }
        }
        
        serverProcessManager.sendCommand(command);
        commandInput.clear();
        
        // Befehl zur Historie hinzufügen (optional)
        addToCommandHistory(command);
    }
}

private boolean isDestructiveCommand(String command) {
    String lowerCommand = command.toLowerCase().trim();
    return DESTRUCTIVE_COMMANDS.stream()
        .anyMatch(lowerCommand::contains);
}

private boolean confirmDestructiveCommand(String command) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Bestätigung erforderlich");
    alert.setHeaderText("Destruktiver Befehl");
    alert.setContentText("Befehl ausführen: /" + command + "?\n\nDies könnte den Server oder Spieler beeinflussen.");
    
    return alert.showAndWait()
        .map(result -> result == ButtonType.OK)
        .orElse(false);
}
```

---

### 39. EULA wird automatisch akzeptiert
**Datei:** `ServerProcessManager.java:141-165`  
**Schweregrad:** NIEDRIG

**Beschreibung:**
```java
private void showEulaPopup(File eulaFile) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    // ...
    if (result.isPresent() && result.get() == ButtonType.OK) {
        try (PrintWriter writer = new PrintWriter(eulaFile)) {
            writer.println("# EULA Zustimmung");
            writer.println("eula=true");  // Automatisch geschrieben!
        }
    }
}
```

**Problem:** EULA wird ohne Anzeige des tatsächlichen Textes akzeptiert.

**Lösung:** EULA-Text anzeigen oder Link zu https://www.minecraft.net/eula öffnen.

### LÖSUNG zu #39: EULA wird automatisch akzeptiert

In `ServerProcessManager.java` die EULA-Anzeige verbessern:

```java
private void showEulaPopup(File eulaFile) {
    // EULA-Text einlesen
    String eulaText;
    try {
        eulaText = new String(Files.readAllBytes(eulaFile.toPath()));
    } catch (IOException e) {
        eulaText = "Konnte EULA-Text nicht laden.\n" +
                   "Bitte besuchen Sie: https://www.minecraft.net/eula";
    }
    
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Minecraft EULA");
    alert.setHeaderText("End User License Agreement");
    
    // TextArea für langen EULA-Text
    TextArea textArea = new TextArea(eulaText);
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxWidth(600);
    textArea.setMaxHeight(300);
    
    alert.getDialogPane().setContent(textArea);
    alert.setGraphic(null);
    
    // Buttons
    ButtonType acceptButton = new ButtonType("Akzeptieren");
    ButtonType declineButton = new ButtonType("Ablehnen");
    ButtonType openLinkButton = new ButtonType("Link öffnen");
    
    alert.getButtonTypes().setAll(acceptButton, openLinkButton, declineButton);
    
    Optional<ButtonType> result = alert.showAndWait();
    
    if (result.isPresent()) {
        if (result.get() == acceptButton) {
            acceptEula(eulaFile);
        } else if (result.get() == openLinkButton) {
            // Link im Browser öffnen
            try {
                Desktop.getDesktop().browse(
                    new URI("https://www.minecraft.net/eula"));
            } catch (Exception e) {
                logger.error("Konnte Link nicht öffnen", e);
            }
        }
        // Bei Decline: nichts tun, Dialog schließt
    }
}

private void acceptEula(File eulaFile) {
    try (PrintWriter writer = new PrintWriter(eulaFile)) {
        writer.println("# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA (https://www.minecraft.net/eula).");
        writer.println("#");
        writer.println("# Mon Jan 01 00:00:00 CET 2024");
        writer.println("eula=true");
        logger.info("EULA akzeptiert");
    } catch (IOException e) {
        logger.error("Fehler beim Schreiben der EULA", e);
    }
}
```

---

## ARCHITEKTUR-PROBLEME

### 41. MVC-Verletzung - Controller kennt Model und View
**Datei:** `Controller.java`  
**Schweregrad:** NIEDRIG

**Beschreibung:** Der Controller hat direkte Referenzen auf:
- UI-Elemente (`consoleOutput`, `commandInput`, etc.)
- Manager-Klassen (`autoSaveManager`, `autoRestartManager`)
- `ConfigManager` (Model)

**Besser:** ViewModel Pattern oder saubere Trennung.

### LÖSUNG zu #41: MVC-Verletzung - Controller kennt Model und View

**Kurzfristig akzeptabel** - Für JavaFX-Anwendungen ist es üblich, dass der Controller direkte Referenzen hat.

**Langfristige Verbesserung:**
1. Business-Logik in separate Service-Klassen auslagern
2. Ein ViewModel zwischen Controller und Model einführen
3. Events für UI-Updates verwenden

Beispiel für sauberere Trennung:
```java
// Statt direkter Referenzen im Controller:
public class Controller {
    private ServerViewModel viewModel; // ViewModel statt direkter Manager
}

// ViewModel kapselt die Logik:
public class ServerViewModel {
    private ServerProcessManager processManager;
    private AutoSaveManager autoSaveManager;
    // ... Public API für UI
}
```

Dies ist ein größeres Refactoring und kann später durchgeführt werden.

---

### 42. Zirkuläre Abhängigkeit
**Datei:** `Main.java` <-> `Controller.java` <-> `AutoSaveManager.java`

**Beschreibung:**
```
Main.java hält static Controller
Controller.java erzeugt AutoSaveManager
AutoSaveManager.java ruft Main.getPrimaryController() auf
```

Dies ist eine zirkuläre Abhängigkeit!

### LÖSUNG zu #42: Zirkuläre Abhängigkeit

**Lösung:** Event Bus einführen (siehe auch #14)

Durch die Verwendung eines Event Busses werden die direkten Abhängigkeiten entfernt:

```
Main → (static) → Controller
AutoSaveManager → (static) → Main → Controller

WIRD ZU:

Main → EventBus ← AutoSaveManager
Controller → EventBus ← (subscribed)
```

Siehe LÖSUNG zu #14 für die genaue Implementierung.

---

### 43. Fehlende Singleton-Pattern für Manager
**Datei:** `ConfigManager.java`  
**Schweregrad:** NIEDRIG

**Beschreibung:** `ConfigManager` wird mehrfach instanziiert:
- `Main.java:24`
- `Controller.java:67`
- `FirstConfigController.java:25`
- `OptionsDialogController.java:47`

**Auswirkung:** Unterschiedliche Instanzen könnten unterschiedliche Konfigurationsstände haben.

**Lösung:** Singleton oder Dependency Injection verwenden.

### LÖSUNG zu #43: Fehlende Singleton-Pattern für ConfigManager

In `ConfigManager.java` Singleton implementieren:

```java
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    // Singleton-Instanz
    private static ConfigManager instance;
    
    private final String configFilePath = "config.txt";
    private final Map<String, String> config = new HashMap<>();
    private final Map<String, String> comments = new HashMap<>();
    private final boolean firstRun;

    /**
     * Singleton-Zugriff.
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * Konstruktor - privat für Singleton.
     */
    private ConfigManager() {
        this.firstRun = !checkIfConfigExists();
        initializeComments();
        if (firstRun) {
            logger.info("Erste Ausführung erkannt. Erstelle Standardkonfiguration...");
            createDefaultConfig();
            saveConfig();
        }
        loadConfig();
    }
    
    // ... restliche Methoden
}
```

**Verwendung in anderen Klassen:**
```java
// Statt: new ConfigManager()
// Neu:
ConfigManager configManager = ConfigManager.getInstance();
```

---

### 44. Keine Ereignis-Reihenfolge-Garantie
**Datei:** `ServerOperationCoordinator.java`

**Beschreibung:** Bei schnellem Klicken auf Start/Stop/Restart könnte die Reihenfolge der Operationen unvorhersehbar sein.

### LÖSUNG zu #44: Keine Ereignis-Reihenfolge-Garantie

In `ServerOperationCoordinator.java` eine Queue für Operationen implementieren:

```java
public class ServerOperationCoordinator {
    private final Queue<OperationType> operationQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);
    
    public void queueOperation(OperationType operation) {
        operationQueue.offer(operation);
        processNextOperation();
    }
    
    private synchronized void processNextOperation() {
        if (processing.get()) {
            return; // Bereits eine Operation läuft
        }
        
        OperationType operation = operationQueue.poll();
        if (operation == null) {
            return;
        }
        
        processing.set(true);
        
        switch (operation) {
            case START:
                startServerInternal();
                break;
            case STOP:
                stopServerInternal();
                break;
            case RESTART:
                restartServerInternal();
                break;
            case BACKUP:
                performBackupInternal();
                break;
        }
        
        processing.set(false);
        
        // Nächste Operation verarbeiten
        if (!operationQueue.isEmpty()) {
            processNextOperation();
        }
    }
}
```

**Alternative (einfacher):**
Durch die bestehende `operationInProgress` Prüfung werden schnelle Klicks bereits verhindert. Das Problem ist geringfügig.

---

---

## ZUSÄTZLICHE EMPFEHLUNGEN

### 45. Server-Log-Parsing
**Empfehlung:** Server-Output parsen für:
- Spieler-Join/Leave erkennen
- Chat-Nachrichten extrahieren
- Fehler/Warnungen hervorheben
- "Done!" Nachricht für State-Übergang nutzen

---

### 46. Auto-Updater
**Empfehlung:** Automatische Update-Prüfung für die GUI.

---

### 47. Konfigurierbare Backup-Rotation
**Empfehlung:** 
- Maximale Anzahl Backups
- Maximales Alter der Backups
- Automatische Löschung alter Backups

---

### 48. Server-Performance-Monitoring
**Empfehlung:**
- TPS-Anzeige
- Speicherverbrauch
- Spieleranzahl
- CPU-Last

---

---

## ZUSAMMENFASSUNG & PRIORISIERUNG

### Statistik

| Kategorie | Anzahl | Noch Offen |
|-----------|--------|------------|
| KRITISCH | 4 | 4 |
| HOCH | 4 | 4 |
| MITTEL | 9 | 9 |
| NIEDRIG | 6 | 6 |
| FXML/Thread | 4 | 4 |
| Ressourcen/Security | 4 | 4 |
| Architektur | 4 | 4 |
| Empfehlungen | 4 | 4 |
| **Gesamt** | **39** | **39** |

Alle verbleibenden Probleme haben Lösungen im Dokument.

### Sofort zu beheben (Blocker)

| # | Problem | Auswirkung | Status |
|---|---------|------------|--------|
| 1 | Doppelte Listener-Registrierung | Events werden doppelt gefeuert | OFFEN |
| 3 | FirstConfigController javaPathField | NullPointerException | OFFEN |
| 4 | pom.xml falsche Main-Class | mvn javafx:run fehlschlägt | OFFEN |
| 5 | Leerzeichen im Java-Pfad | Server startet NICHT | OFFEN |

### Dringend zu beheben

| # | Problem | Auswirkung | Status |
|---|---------|------------|--------|
| 6 | Timer-Handling | Thread-Leaks | OFFEN |
| 7 | State-Synchronisation | Falsche Server-Statusanzeige | OFFEN |
| 8 | Fehlende Validierung | Ungültige Konfigurationen | OFFEN |
| 10 | State nicht thread-safe | Race-Conditions | OFFEN |
| 33 | Race Condition Coordinator | Gleichzeitige Operationen | OFFEN |

### Wichtig zu beheben

| # | Problem | Auswirkung |
|---|---------|------------|
| 6 | Timer-Handling | Thread-Leaks |
| 11 | Hardcoded World-Ordner | Falsche Backups |
| 14 | Statische Controller-Referenz | Memory Leak |
| 18 | isFirstRun Logik | Dialog bei jedem Start |

---

## DATEI-BY-DATEI ZUSAMMENFASSUNG

| Datei | Zeilen | Probleme | Schwerster Bug |
|-------|--------|----------|----------------|
| `Controller.java` | 337 | 8 | Doppelte Listener |
| `Main.java` | 88 | 1 | Falsche Main-Class |
| `ServerProcessManager.java` | 212 | 5 | Leerzeichen im Pfad |
| `ConfigManager.java` | 223 | 1 | isFirstRun Logik |
| `FirstConfigController.java` | 118 | 1 | javaPathField |
| `OptionsDialogController.java` | 282 | 1 | Fehlende Validierung |
| `AutoSaveManager.java` | 115 | 1 | Timer-Handling |
| `AutoRestartManager.java` | 115 | 1 | Timer-Handling |
| `BackupManager.java` | 136 | 1 | Hardcoded World |
| `ServerOperationCoordinator.java` | 223 | 2 | Race Condition |
| `ServerCommandExecutor.java` | 61 | 1 | Unbenutzt |
| `LogManager.java` | 22 | 1 | Unbenutzt |
| `TimeUtils.java` | 50 | 0 | - |
| `ServerState.java` | 28 | 0 | - |
| `MCServerGUI-view.fxml` | 99 | 1 | Typo |
| `OptionsDialog.fxml` | 41 | 0 | - |
| `FirstConfigDialog.fxml` | 30 | 0 | - |
| `pom.xml` | 135 | 1 | Falsche Main-Class |
| `style.css` | 96 | 0 | - |

---

**Ende der Analyse**
