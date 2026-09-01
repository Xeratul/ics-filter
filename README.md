# ICS Filter

Ein Maven-/JavaFX-Desktop-Tool, das mehrere ICS-Kalender (iCalendar) aus dem Internet lädt, deren Termine filtert und sie sowohl in einer **Monatskalender-Ansicht** als auch in einer **sortierten Terminliste** anzeigt.

## Download

Die fertig gebaute, eigenständig lauffähige Windows-EXE findest du unter den [Releases](https://github.com/Xeratul/ics-filter/releases/latest).

<img width="1920" height="1152" alt="image" src="https://github.com/user-attachments/assets/668b29e6-1b44-4da8-9f96-5a6eca94ca91" />

<img width="382" height="297" alt="image" src="https://github.com/user-attachments/assets/df52ac6e-ffab-4317-bd8b-ad56766feb59" />

<img width="1920" height="1152" alt="image" src="https://github.com/user-attachments/assets/719b8639-1aee-421d-b6b9-007285ac74b0" />



## Voraussetzungen

- JDK 21+ (getestet mit JDK 26, `release 26`)
- Maven (oder die mitgelieferte Wrapper-Datei `mvnw`)

## Features

- **Kalenderquellen verwalten:** Quellen (Name + URL) als schaltbare Kacheln in der oberen Leiste; hinzufügen, bearbeiten, entfernen, einzeln aktivieren/deaktivieren und neu laden. Hinzufügen/Bearbeiten erfolgt über einen Dialog.
- **Termine ignorieren:** Rechtsklick auf einen Listeneintrag → „Diese Einträge ignorieren“ blendet alle gleich betitelten Termine dieser Quelle aus. Die ignorierte Titel-Liste ist im Quellen-Dialog sichtbar und lässt sich dort wieder löschen.
- **ICS herunterladen & parsen:** Download über `HTTP/HTTPS`, Parsen mit `ical4j`.
- **Wiederkehrende Termine:** Eigene RFC-5545-Expansion für `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` mit `INTERVAL`, `COUNT`, `UNTIL`, `BYDAY`, `BYMONTH`, `BYMONTHDAY`.
- **Darstellung:** Monatsraster mit farbigen Termin-Chips (Farbe je Quelle) und eine nach Startzeit sortierte Tabelle.
- **Listeneinstellungen:** Über das Zahnrad oben rechts in der Tabelle lässt sich das Startdatum einstellen, ab dem Termine angezeigt werden (ab diesem Jahr / Monat / heute).

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
│   ├── CalendarSource.java  # Quelle (Name + URL + Filter + Farbe + Ignorierte Titel)
│   ├── CalendarEvent.java   # Einzelner Termin
│   └── StartFrom.java       # Startdatum-Grenze der Liste (Jahr/Monat/heute)
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
