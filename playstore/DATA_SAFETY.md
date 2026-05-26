# Google Play Data Safety Formular – Antworten

> Dieses Dokument enthält die exakten Antworten für das Data Safety Formular
> in der Google Play Console. Übertrage die Antworten 1:1 in das Formular.

## Übersicht

Die App erhebt und teilt **keine** Nutzerdaten. Alle Daten werden ausschließlich
lokal auf dem Gerät gespeichert und verlassen es niemals.

---

## Abschnitt 1: Datenerhebung und Datensicherheit

### "Does your app collect or share any of the required user data types?"
**→ No**

### "Is all of the user data collected by your app encrypted in transit?"
**→ Not applicable** (keine Datenübertragung, keine INTERNET-Berechtigung)

### "Do you provide a way for users to request that their data is deleted?"
**→ Yes** — Daten werden bei Deinstallation automatisch gelöscht.
Einzelne Journal-Einträge und Custom Presets können in der App gelöscht werden.

---

## Abschnitt 2: Datentypen (alle "No")

| Datentyp | Erhoben? | Geteilt? |
|----------|----------|----------|
| Location | No | No |
| Personal info (name, email, etc.) | No | No |
| Financial info | No | No |
| Health and fitness | No | No |
| Messages | No | No |
| Photos and videos | No | No |
| Audio files | No | No |
| Files and docs | No | No |
| Calendar | No | No |
| Contacts | No | No |
| App activity | No | No |
| Web browsing | No | No |
| App info and performance | No | No |
| Device or other IDs | No | No |

---

## Abschnitt 3: Sicherheitspraktiken

### "Is your app designed to comply with the Families Policy?"
**→ No** (App ist nicht speziell für Kinder konzipiert)

### "Does your app allow users to create an account?"
**→ No**

### "Does your app use any third-party SDKs?"
**→ No** (keine Drittanbieter-SDKs, reine AndroidX/Jetpack-Bibliotheken)

---

## Abschnitt 4: Lokale Datenspeicherung (zur eigenen Dokumentation)

Folgende Daten werden **ausschließlich lokal** via Android DataStore Preferences
gespeichert. Google Play klassifiziert lokal gespeicherte Daten, die das Gerät
nie verlassen, NICHT als "collected data":

| Lokale Daten | Inhalt |
|-------------|--------|
| Custom Presets | Name, Emoji, Phasen-Konfiguration, Erstellungsdatum |
| Session Journal | Preset-Name, Dauer, Bewertung (1-5 Sterne), Stimmung, Notizen, Zeitstempel |
| Einstellungen | Theme-Modus (System/Hell/Dunkel), Sprach-Tag |

---

## Zusammenfassung für das Play Console Dashboard

```
Data collected:     None
Data shared:        None
Security practices: Data encrypted in transit — N/A (no network)
                    Data can be deleted — Yes (uninstall or in-app)
```
