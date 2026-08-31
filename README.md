# ICS Filter

Ein Maven-/JavaFX-Desktop-Tool, das mehrere ICS-Kalender (iCalendar) aus dem Internet lädt, deren Termine filtert und sie sowohl in einer **Monatskalender-Ansicht** als auch in einer **sortierten Terminliste** anzeigt.

## Download

Die fertig gebaute, eigenständig lauffähige Windows-EXE findest du unter den [Releases](https://github.com/Xeratul/ics-filter/releases/latest).

<img width="1920" height="1153" alt="image" src="https://github.com/user-attachments/assets/89ace7c7-061d-40e4-b009-7cd30f44926d" />

## Voraussetzungen

- JDK 21+ (getestet mit JDK 26, `release 26`)
- Maven (oder die mitgelieferte Wrapper-Datei `mvnw`)

## Features

- **Kalenderquellen verwalten:** Quellen (Name + URL) als schaltbare Kacheln in der oberen Leiste; hinzufügen, bearbeiten, entfernen, einzeln aktivieren/deaktivieren und neu laden. Hinzufügen/Bearbeiten erfolgt über einen Dialog.
- **ICS herunterladen & parsen:** Download über `HTTP/HTTPS`, Parsen mit `ical4j`.
- **Wiederkehrende Termine:** Eigene RFC-5545-Expansion für `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` mit `INTERVAL`, `COUNT`, `UNTIL`, `BYDAY`, `BYMONTH`, `BYMONTHDAY`.
- **Darstellung:** Monatsraster mit farbigen Termin-Chips (Farbe je Quelle) und eine nach Startzeit sortierte Tabelle.

## Build

```bash
./mvnw clean test
./mvnw compile
```

## Ausführbare EXE bauen (ohne Installer, ohne WiX)

Erzeugt mit jpackage einen eigenständigen, startbaren Ordner samt `.exe`:
**kein Installer, kein WiX**.

```bash
./mvnw clean package -Papp-image
```

Ausgabe: `target/dist/ICS Filter/ICS Filter.exe` — der gesamte Ordner
`target/dist/ICS Filter` muss beieinander bleiben.

## Starten

```bash
./mvnw javafx:run
```

## Struktur

```
src/main/java/com/icsfilter
├── App.java                 # JavaFX-Einstiegs- und Verkabelungsklasse
├── model
│   ├── CalendarSource.java  # Quelle (Name + URL)
│   └── CalendarEvent.java   # Einzelner Termin
├── ical
│   ├── Recurrence.java      # RFC-5545-Wiederholungs-Expansion
│   └── EventLoader.java     # Download + Parse + Expansion
└── ui
    ├── SourceTilesBar.java  # Quellen-Kacheln oben (Dialog für Hinzufügen/Bearbeiten)
    ├── CalendarGrid.java
    ├── EventListPane.java
    ├── EventDetailPane.java
    └── UiPalette.java
```

## Hinweise

- Die Wiederholungs-Expansion ist auf ein begrenztes Zeitfenster beschränkt (Standard: 1 Jahr zurück bis 2 Jahre voraus) und läuft tageweise; für typische Feeds ist das ausreichend.
- Zu den Quellen gehören auch öffentliche ICS-URLs (z. B. Feiertage, Webcal/ICS-Endpunkte).
