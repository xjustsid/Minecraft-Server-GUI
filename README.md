# Minecraft Server GUI

**Minecraft Server GUI** ist eine Windows-basierte JavaFX-Desktop-Anwendung zur Verwaltung von Minecraft Java Edition Servern über eine grafische Oberfläche.

Das Projekt richtet sich an Nutzer, die einen Minecraft-Server lokal auf einem Windows-Rechner betreiben möchten – beispielsweise auf einem alten PC oder Laptop – ohne auf kostenpflichtige Webpanel-Hostinglösungen angewiesen zu sein.

---

## Plattform

* Betriebssystem: **Windows (Windows-spezifische Implementierung)**
* Technologie: Java / JavaFX
* Architektur: Desktop-Anwendung
* Kompatibilität: Jede Minecraft `server.jar` (Vanilla, Paper, Spigot etc.)

Die Anwendung ist derzeit **nicht plattformunabhängig**, da Teile hart auf Windows implementiert sind.

---

## Zielgruppe

Die Anwendung richtet sich an:

* Nutzer, die einen Minecraft-Server lokal auf einem Windows-Rechner betreiben möchten
* Besitzer älterer PCs oder Laptops, die als Server verwendet werden sollen
* Kleine private Freundesgruppen
* Anwender, die kein kostenpflichtiges Webpanel nutzen möchten
* Nutzer, die eine grafische Oberfläche gegenüber reiner Konsolenbedienung bevorzugen

---

## Hauptfunktionen

## 1. Server-Steuerung

* Start
* Stop
* Restart
* Manuelles Backup („Save“)
* Server-Statusanzeige (OFFLINE, STARTING, ONLINE, STOPPING, BACKUP, RESTARTING, ERROR)
* Echtzeit-Spieleranzeige
* Anzeige letzter / nächster Auto-Save
* Anzeige letzter / nächster Auto-Restart

---

## 2. Echtzeit-Konsole

* Live-Konsolenausgabe (Read-Only)
* Eingabefeld für Server-Befehle
* Direkte Kommandoausführung (Button oder ENTER)

---

## 3. Backup-System

* Manuelle Backups
* Geplante Backups (einmalig oder wiederkehrend)
* Backup als ZIP-Datei
* Konfigurierbarer Backup-Ordner
* Rollback-Funktion bei Weltkorruption

---

## 4. Automatisierte Neustarts

* Bis zu 4 definierbare Neustart-Zeiten pro Tag
* Auto-Restart aktivierbar
* Keep-Alive-System (Überprüfung alle 5 Sekunden bei Absturz)

---

## 5. Geplante Befehle (Command Automation)

Server-Befehle können automatisch ausgelöst werden durch:

1. Keine Spieler online
2. Weniger als X Spieler
3. Mehr als X Spieler
4. Zu einer bestimmten Uhrzeit
5. Alle X Minuten

Optional wiederholbar oder einmalig.

---

## 6. Datei- und Direktzugriffe

* Projektordner öffnen
* Backup-Ordner öffnen
* Logs-Ordner öffnen
* `server.properties` direkt öffnen
* Whitelist-Reload


---

## Erstkonfiguration

Beim ersten Start erscheint ein Setup-Dialog:

* Server-JAR-Pfad
* Java-Pfad
* Minimaler RAM
* Maximaler RAM
* Backup-Ordner

---

## Installation

1. ZIP-Datei entpacken
2. `MC-Server-GUI.exe`, `runtime/` und `app/` in den Server-Ordner kopieren
3. `MC-Server-GUI.exe` starten

Eine separate Java-Installation ist nicht erforderlich (Runtime enthalten).

---

## Status

Version: 0.1.0 (Pre-Release)

Das Projekt befindet sich in aktiver Entwicklung.
Monitoring-Features und weitere Erweiterungen sind geplant.

---

## Lizenz

Der Quellcode ist öffentlich verfügbar.
Eine Lizenz ist derzeit noch nicht festgelegt.

---

## Mitwirkung

Pull Requests, Verbesserungsvorschläge und Feature-Ideen sind willkommen.

---




# Minecraft Server GUI

**Minecraft Server GUI** is a Windows-based JavaFX desktop application for managing Minecraft Java Edition servers through a graphical interface.

It is designed for users who host a Minecraft server locally on a Windows machine — for example on an old PC or laptop — without relying on paid web hosting panels.

---

## Platform

* Operating System: **Windows (Windows-specific implementation)**
* Technology: Java / JavaFX
* Type: Desktop application
* Compatible with any Minecraft `server.jar` (Vanilla, Paper, Spigot, etc.)

The application is currently **not cross-platform**, as several components are implemented using Windows-specific logic.

---

## Target Audience

This application is intended for:

* Users hosting a Minecraft server locally on a Windows machine
* Individuals using older PCs or laptops as dedicated servers
* Small private friend groups
* Users who prefer not to rent paid web hosting panels
* Users who want a graphical interface instead of pure console management

---

## Core Features

### 1. Server Control

* Start
* Stop
* Restart
* Manual backup (“Save”)
* Server state display (OFFLINE, STARTING, ONLINE, STOPPING, BACKUP, RESTARTING, ERROR)
* Real-time player count
* Last / next auto-save display
* Last / next auto-restart display

---

### 2. Real-Time Console

* Live server output (read-only)
* Command input field
* Instant command execution (button or ENTER)

---

### 3. Backup System

* Manual backups
* Scheduled backups (one-time or recurring)
* ZIP-based backup creation
* Configurable backup directory
* Rollback functionality in case of world corruption

---

### 4. Automated Restarts

* Up to 4 restart times per day
* Optional auto-restart
* Keep-alive crash detection (checked every 5 seconds)

---

### 5. Scheduled Commands

Server commands can be triggered automatically by:

1. No players online
2. Less than X players
3. More than X players
4. At a specific time
5. Every X minutes

Commands can be one-time or recurring.

---

### 6. Direct File Access

* Open project folder
* Open backup folder
* Open logs folder
* Open `server.properties`
* Reload whitelist

---

## First-Time Setup

On first launch, a configuration dialog appears:

* Server JAR path
* Java path
* Minimum RAM
* Maximum RAM
* Backup folder

---

## Installation

1. Extract the ZIP file
2. Copy all contents into the folder containing your `server.jar`
3. Launch `MC-Server-GUI.exe`

No separate Java installation required (runtime included).

---

## Status

Version: 0.1.0 (Pre-release)

The project is under active development.
Monitoring features and further enhancements are planned.

---

## License

Source code is publicly available.
No license has been defined yet.
