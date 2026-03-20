# Minecraft Server GUI

**Windows-basierte JavaFX-Desktop-Anwendung zur Verwaltung von Minecraft Java Edition Servern**

[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-23.0.2-blue)](https://openjfx.io/)
[![Status](https://img.shields.io/badge/Status-Aktive%20Weiterentwicklung-brightgreen)]()
[![Version](https://img.shields.io/badge/Version-0.3.0-informational)]()

Die Minecraft Server GUI entstand aus der Motivation, keine zufriedenstellende lokale Verwaltungssoftware mit Backup- und Restart-Funktionen gefunden zu haben. Die Anwendung ermöglicht es Nutzern, ihre Minecraft-Server über eine intuitive grafische Oberfläche zu steuern — ohne Kommandozeile. Besonders geeignet für Besitzer älterer PCs/Laptops, die als Server dienen, sowie für kleine Freundesgruppen.

---

## Inhalt

- [Features](#features)
- [Systemanforderungen](#systemanforderungen)
- [Installation (Release-Version)](#installation-release-version)
- [Installation (Quellcode / Entwickler)](#installation-quellcode--entwickler)
- [Erster Start & Einrichtung](#erster-start--einrichtung)
- [Konfiguration](#konfiguration)
- [Projektstruktur](#projektstruktur)
- [Kompatibilität](#kompatibilität)
- [Roadmap](#roadmap)
- [GitHub](#github)

---

## Features

### Server-Steuerung
- Start, Stop, Restart per Knopfdruck
- Manuelles Backup
- Server-Statusanzeige (Online/Offline)
- Echtzeit-Spieleranzeige

### Echtzeit-Konsole
- Live-Konsolenausgabe mit Scroll-Funktion
- Eingabefeld für direkte Serverkommandos
- Sofortige Ausführung ohne Kommandozeile

### Backup-System
- Manuelle und geplante Backups
- ZIP-Archivierung des Weltordners
- Rollback-Funktion (Wiederherstellung aus Backup)

### Automatisierte Neustarts
- Keep-Alive: automatischer Neustart bei Absturz
- Auto-Restart: bis zu 4 konfigurierbare Zeiten pro Tag

### Command Automation
- Geplante Befehle mit konfigurierbaren Triggern
- Zeitbasierte Ausführung
- Trigger abhängig von Spieleranzahl / TPS

### Datei- & Direktzugriff
- Schnellzugriff auf Projekt-, Backup- und Logs-Ordner
- `server.properties` direkt öffnen
- Whitelist-Reload per Knopfdruck

---

## Systemanforderungen

| Anforderung         | Mindest                         | Empfohlen                        |
|---------------------|---------------------------------|----------------------------------|
| Betriebssystem      | Windows 10 (64-bit)             | Windows 10/11 (64-bit)           |
| Java                | JDK 17                          | JDK 21 (LTS)                     |
| RAM                 | 2 GB (Server + GUI)             | 4–8 GB                           |
| Speicherplatz       | 500 MB + Weltgröße + Backups    | mind. 5 GB frei                  |
| Minecraft-Server    | Vanilla / Paper / Spigot jar    | Paper (empfohlen für Performance)|

> **Wichtig:** `jpackage` (Teil des JDK seit JDK 14) wird nur zum **Bauen** des Installers benötigt, nicht zur normalen Nutzung.

---

## Installation (Release-Version)

### Option A: MSI-Installer (empfohlen)

1. Gehe zu [Releases](https://github.com/xjustsid/Minecraft-Server-GUI/releases)
2. Lade die neueste `McServerGUI-x.x.x.msi` herunter
3. Doppelklick auf die `.msi`-Datei → Installationsassistent folgen
4. Die App wird unter `C:\Program Files\McServerGUI\` installiert
5. Verknüpfung im Startmenü und Desktop werden automatisch angelegt
6. Starte **McServerGUI** und folge dem [Einrichtungsassistenten](#erster-start--einrichtung)

### Option B: Portable (JAR direkt)

Voraussetzung: JDK 21+ und JavaFX 23 müssen installiert und im PATH sein.

```powershell
# Im Verzeichnis mit McServerGUI.jar:
java --module-path "C:\Path\To\javafx-sdk-23\lib" `
     --add-modules javafx.controls,javafx.fxml `
     -jar McServerGUI.jar
```

---

## Installation (Quellcode / Entwickler)

### Voraussetzungen

- [JDK 21+](https://adoptium.net/) — Java-Laufzeitumgebung
- [Apache Maven 3.8+](https://maven.apache.org/download.cgi) — Build-Tool  
  *(alternativ: das mitgelieferte `mvnw.cmd` verwenden, kein separates Maven nötig)*
- [Git](https://git-scm.com/) — zum Klonen des Repositories

### Schritt 1: Repository klonen

```powershell
git clone https://github.com/xjustsid/Minecraft-Server-GUI.git
cd Minecraft-Server-GUI
```

### Schritt 2: Projekt bauen

```powershell
# Im Projektverzeichnis (wo pom.xml liegt):
.\mvnw.cmd clean package
```

Die fertige JAR liegt danach unter `target\McServerGUI.jar`.

### Schritt 3: Anwendung starten

```powershell
.\mvnw.cmd javafx:run
```

### Schritt 4 (optional): Windows-Installer bauen

Voraussetzung: JDK 17+ mit `jpackage` im PATH, sowie [WiX Toolset 3.x](https://wixtoolset.org/) für `.msi`-Erzeugung.

```powershell
# Im Projektverzeichnis:
$env:GITHUB_REF_NAME = "v0.3.0"
$version = $env:GITHUB_REF_NAME -replace "^v", ""

.\mvnw.cmd clean package

New-Item -ItemType Directory -Force .\staging | Out-Null
New-Item -ItemType Directory -Force .\dist    | Out-Null
Copy-Item .\target\McServerGUI.jar .\staging\

jpackage `
  --type msi `
  --name McServerGUI `
  --input .\staging `
  --main-jar McServerGUI.jar `
  --dest .\dist `
  --app-version $version `
  --vendor xjustsid `
  --win-shortcut `
  --win-menu
```

Der fertige Installer liegt unter `dist\McServerGUI-0.3.0.msi`.

> **Hinweis:** Das Skript `build-scripts\package-windows.sh` erfordert eine Bash-Umgebung (z.B. Git Bash). Die obigen PowerShell-Befehle sind das plattformnative Äquivalent.

---

## Erster Start & Einrichtung

Beim ersten Start öffnet sich automatisch der **Einrichtungsassistent**:

1. **Server-JAR auswählen** — z.B. `server.jar` (Vanilla), `paper.jar` oder `spigot.jar`
2. **Java-Executable festlegen** — Pfad zur `java.exe` deines JDK, z.B.:  
   `C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe`
3. **Serververzeichnis wählen** — Ordner, wo der Server liegen soll
4. **RAM konfigurieren** — Min/Max Heap-Speicher (z.B. 2G / 4G)
5. **EULA akzeptieren** — Minecraft's End User License Agreement bestätigen

Nach der Einrichtung wird automatisch eine `config.txt` im Serververzeichnis erstellt.

---

## Konfiguration

Die Hauptkonfiguration ist `config.txt` im installierten/geklonten Verzeichnis (Properties-Format):

```properties
# Java & Server
java.executable=C:\Program Files\Java\jdk-21\bin\java.exe
server.jar=server.jar
server.memory.min=2G
server.memory.max=4G

# Verzeichnisse
server.directory=.
backup.directory=./backups

# Automatisierung
autosave.enabled=true
autosave.interval=300
autorestart.enabled=true
autorestart.times=00:00,06:00,12:00,18:00

# Backups
backup.retention.count=20
backup.retention.days=30

# Netzwerk
rcon.port=25575
rcon.password=
query.port=25565
```

Geplante Befehle werden in `scheduled_commands.json` gespeichert:

```json
[
  {
    "command": "say Server startet in 5 Minuten neu",
    "trigger": "OnTime",
    "triggerValue": "23:55",
    "repeat": true,
    "enabled": true
  }
]
```

---

## Projektstruktur

```
McServerGUI/
├── src/main/java/         # Java-Quellcode
├── src/main/resources/    # FXML-Views, CSS, Icons
├── src/test/java/         # Unit Tests
├── build-scripts/         # Packaging-Skripte (sh / ps1)
├── pom.xml                # Maven Build-Definition
├── config.txt             # App-Konfiguration
├── scheduled_commands.json
└── target/
    └── McServerGUI.jar    # Gebaute Anwendung
```

---

## Kompatibilität

| Server-Typ  | Kompatibel |
|-------------|------------|
| Vanilla     | ✅          |
| Paper       | ✅          |
| Spigot      | ✅          |
| Andere `.jar` | ✅        |

---

## Roadmap

| Status | Version     | Feature                              |
|--------|-------------|--------------------------------------|
| ✅      | 0.1.x       | Grundfunktionalität (Pre-Release)    |
| ✅      | 0.3.x       | Backup-System, Command Automation    |
| 🔲      | geplant     | Benutzerverwaltung & Berechtigungen  |
| 🔲      | geplant     | Erweiterte Backup-Optionen           |
| 🔲      | geplant     | Plugin-Management                    |
| 🔲      | geplant     | Dashboard-Ansicht mit Metriken       |
| 🔲      | geplant     | Multi-Server-Profile                 |
| 🔲      | geplant     | RCON-Unterstützung                   |

---

## GitHub

Quellcode, Issues und Releases:  
[https://github.com/xjustsid/Minecraft-Server-GUI](https://github.com/xjustsid/Minecraft-Server-GUI/tree/master)

---

*© 2026 Justin W. (JustSid). Alle Rechte vorbehalten.*
