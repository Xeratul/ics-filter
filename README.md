# ICS Filter

Ein Maven-/JavaFX-Desktop-Tool, das mehrere ICS-Kalender (iCalendar) aus dem Internet lädt, deren Termine filtert und sie sowohl in einer **Monatskalender-Ansicht** als auch in einer **sortierten Terminliste** anzeigt.

## Voraussetzungen

- JDK 21+ (getestet mit JDK 26, `release 26`)
- Maven (oder die mitgelieferte Wrapper-Datei `mvnw`)

## Features

- **Kalenderquellen verwalten:** Quellen (Name + URL) hinzufügen, entfernen, einzeln aktivieren/deaktivieren und neu laden.
- **ICS herunterladen & parsen:** Download über `HTTP/HTTPS`, Parsen mit `ical4j`.
- **Wiederkehrende Termine:** Eigene RFC-5545-Expansion für `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` mit `INTERVAL`, `COUNT`, `UNTIL`, `BYDAY`, `BYMONTH`, `BYMONTHDAY`.
- **Filterung:** Stichwort (Titel/Beschreibung/Ort), Datumsbereich sowie Kategorien.
- **Darstellung:** Monatsraster mit farbigen Termin-Chips (Farbe je Quelle) und eine nach Startzeit sortierte Tabelle.

## Build

```bash
./mvnw clean test
./mvnw compile
```

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
├── filter
│   └── EventFilter.java     # Stichwort-/Datums-/Kategorien-Filter
└── ui
    ├── SourceManagerPane.java
    ├── FilterPane.java
    ├── CalendarGrid.java
    ├── EventListPane.java
    └── UiPalette.java
```

## Hinweise

- Die Wiederholungs-Expansion ist auf ein begrenztes Zeitfenster beschränkt (Standard: 1 Jahr zurück bis 2 Jahre voraus) und läuft tageweise; für typische Feeds ist das ausreichend.
- Zu den Quellen gehören auch öffentliche ICS-URLs (z. B. Feiertage, Webcal/ICS-Endpunkte).