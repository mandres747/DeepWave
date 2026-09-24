# Klangwecker — Konzept für ein kostenpflichtiges Add-on

Stand: 2026-09-23. Zweites Kauf-Add-on nach dem Rhythmus-Layer
(`docs/RHYTHMUS_LAYER_KONZEPT.md`). Idee aus dem App-Ideen-Backlog vom
23.09.: kein eigener Wecker im Store, sondern ein Add-on von DeepWave.

Entscheidungen des Nutzers (23.09.):

| Frage | Entscheidung |
|---|---|
| Modell | **Beides**: eigenständiger Wecker + Anschluss an den Sleep-Timer |
| Verkauf | **Eigenes Einmalprodukt** `wake_alarm`, 1,99 € (wie `rhythm_layer`) |
| F-Droid | **Nein**: fehlt im FOSS-Flavour wie Rhythmus, WAV-Export, Mixer |
| Beenden | **Vollbild über dem Sperrbildschirm** mit „Aus“ und „Schlummern“ |
| Produktname | **Klangwecker / Sound Alarm** |
| Preis außerhalb DE | wie `rhythm_layer`: DE 1,99 € Endpreis, übrige Länder per Play-Umrechnung |
| Zeitumstellung | Lücke (Frühjahr) → eine Stunde später; doppelte Stunde (Herbst) → nur das erste Mal |
| Schlummern / Ende | 9 Min. ohne neue Rampe; Auto-Ende 15 Min. nach der Weckzeit |
| Exakte-Alarme-Berechtigung | erst beim ersten Einschalten eines Weckers: Erklärung → Systemeinstellung → Wecker wird danach aktiv |
| Weckzeit näher als Rampe | Rampe startet sofort, auf die Restzeit gestaucht; unter 3 Min. nur Weckklang |
| Aus-Geste (Vollbild) | Wischen zum Ausschalten; Schlummern als großer Knopf darüber |
| Weckklang | synthetisierte Klangschale, alle ~20 s, langes Ausklingen |
| Vibration | pro Wecker schaltbar, Standard aus |
| Vollbild-Inhalt | Uhrzeit + langsam pulsierender Atemkreis (vorhandener BreathingGuide) |
| Nicht-Käufer | Icon sichtbar; Sheet zeigt Beschreibung, Hörprobe der Klangschale, „Freischalten – 1,99 €“ |
| Wecker-Sheet | Liste + Editor-Dialog (Uhrzeit, Tage, Rampe, Dauer, Lautstärke, Ambient, Vibration) |
| Neuer Wecker | 07:00 Mo–Fr, Rampe Frisch, 20 Min., 70 %, Ambient Bach 40 %, Vibration aus |
| Probehören | 10-s-Zeitraffer der Rampe + ein Klangschalen-Schlag, Medien-Kanal |
| Pulsform (ganze App) | Rechteckpuls mit 10-ms-Kosinusflanken statt hartem Schalten |
| Tonhöhen | Träger G4 392 Hz, Klangschale C5 523 Hz (reine Quarte) |
| Mischung zur Weckzeit | Pulsspur in 10 s auf 30 % zurück, Klangschale + Ambient tragen, Summe ≤ 0,9 |
| Rauigkeit | Pulstiefe 50 %, Bänder gleiten statt zu springen |
| Sleep-Timer-Zeile | unter den Timer-Chips; vorhandenen Wecker anzeigen, sonst einmalig; Nicht-Käufer: dezenter Hinweis |
| Vollbild entzogen | Wecker klingelt trotzdem, Aus/Schlummern über Benachrichtigung; Hinweis im Wecker-Sheet |

---

## 1. Was der Klangwecker ist

Ein Wecker, der nicht mit einem Klingelton weckt, sondern **mit einer Rampe
aus dem Schlaf herausführt**: 15–30 Minuten vor der Weckzeit beginnt leise
eine Session von Theta über Alpha nach Beta, dazu wird eine Ambient-Schicht
eingeblendet und die Lautstärke steigt langsam an. Zur Weckzeit kommt ein
synthetisierter Klang (Klangschale/Glocke) dazu, der sich bis zum Abschalten
wiederholt.

Die Nische: Sanfte Wecker-Apps haben meist Licht oder Vogelstimmen, aber
keine Frequenzrampe. Binaural-Apps haben keinen Wecker. DeepWave hat mit
Sleep-Presets, Sleep-Timer und Ambient-Mixer schon den Abend. Der
Klangwecker ergänzt den Morgen.

### 1.1 Zwei Einstiege, ein Mechanismus

1. **Eigenständig:** Weckzeit + Wochentage + Rampe wählen, App schließen.
2. **Aus dem Sleep-Timer:** Beim **Stellen** des Sleep-Timers erscheint
   darunter die Zeile „Morgen mit Weckrampe wecken 07:00 [Schalter]“.
   (Korrigiert 24.09.: der erste Entwurf fragte beim *Ablaufen* des Timers
   – dann schläft man und sieht die Frage nie.) Klingelt innerhalb von 18 h
   schon ein Wecker, zeigt die Zeile nur ihn an; sonst schaltet sie einen
   einmaligen Wecker (eigene Id `sleep_timer`, Einstellungen vom zuletzt
   angelegten Wecker, Uhrzeit = zuletzt verwendete). Nicht-Käufer sehen
   einen dezenten Hinweis aufs Klangwecker-Sheet.

**Über Nacht läuft nichts.** Die Einschlaf-Session endet wie bisher mit dem
Sleep-Timer, der Dienst beendet sich. Erst `AlarmManager` startet morgens
die App neu. Ein stiller 8-Stunden-Vordergrunddienst wäre schlecht für den
Akku und verstößt gegen den Sinn von `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
(die Play-Erklärung dazu ist erst seit dem 10./11.09. durch).

---

## 2. Umfang v1

**Drin:**

1. **Wecker-Liste**: Mehrere Wecker, je Uhrzeit, Wochentage (oder einmalig),
   an/aus, Weckrampe, Lautstärke.
2. **Weckrampen**: 3 fertige Rampen (siehe 4.2), Dauer 10/20/30 Min.
3. **Ambient-Einblendung**: optional eine Schicht aus dem Mixer (Bach, Wind,
   Regen …) mit eigener Lautstärke.
4. **Weckklang ab der Weckzeit**: synthetisiert (kein Asset, keine Lizenz),
   Wiederholung alle ~20 s, steigende Lautstärke.
5. **Vollbild-Weckbildschirm**: Uhrzeit, „Aus“ (Wischen), „Schlummern 9 Min.“
6. **Automatisches Ende**: 15 Min. nach der Weckzeit ohne Reaktion ist
   Schluss, damit ein vergessenes Handy nicht stundenlang weiterläuft.
7. **Sleep-Timer-Anschluss** (1.1, Einstieg 2).
8. **Nächster Wecker** in der oberen Leiste des MainScreen („⏰ 06:30“).

**Bewusst draußen (v1):**

- Eigene Klingeltöne / Musik aus der Mediathek (Dateiberechtigungen, Formate)
- Schlafphasen-Erkennung über Sensoren/Mikrofon („Smart Wake“)
- Aufgaben zum Abschalten (Rechnen, Schütteln)
- Wear-OS-Anbindung

---

## 3. Android-Plattform: was ein Wecker braucht

Das ist der eigentlich neue Teil. Bisher läuft Ton in DeepWave nur, solange
die App ihn gestartet hat. Ein Wecker muss eine beendete App zu einer
exakten Uhrzeit wieder starten.

| Baustein | Warum | Anmerkung |
|---|---|---|
| `AlarmManager.setAlarmClock()` | Exakt, auch im Doze-Modus; Android zeigt das Wecker-Symbol in der Statusleiste | Ausnahme von den Startbeschränkungen für Vordergrunddienste im Hintergrund |
| `SCHEDULE_EXACT_ALARM` | Pflicht für `setAlarmClock` | Seit Android 14 bei Neuinstallation **standardmäßig verweigert** → Erklärschritt + `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` |
| ~~`USE_EXACT_ALARM`~~ | würde ohne Nachfrage gewährt | **Nicht nehmen**: Play erlaubt das nur Apps, deren *Kernfunktion* ein Wecker ist. DeepWave ist das nicht → Ablehnungsrisiko |
| `USE_FULL_SCREEN_INTENT` | Vollbild über dem Sperrbildschirm | Android 14+: nur für Wecker-/Anruf-Apps automatisch; **Play-Erklärung** nötig; Fallback: Heads-up-Benachrichtigung |
| `RECEIVE_BOOT_COMPLETED` | Nach Neustart sind alle Alarme gelöscht | Receiver plant aus DataStore neu |
| `ACTION_TIME_CHANGED` / `TIMEZONE_CHANGED` | Uhr/Zeitzone geändert | ebenfalls neu planen |
| Benachrichtigungskanal `wake_alarm` | `IMPORTANCE_HIGH`, eigener Kanal | Der bestehende Kanal ist `IMPORTANCE_LOW` und dafür ungeeignet |
| **`AudioAttributes.USAGE_ALARM`** | Alarm-Lautstärke, wird bei „Nicht stören“ durchgelassen | **Alle drei Engines setzen heute fest `USAGE_MEDIA`**, siehe 5.2 |

**Zwei Alarme je Termin (Stand 24.09., umgesetzt):** `AlarmClockInfo` hat
nur *eine* Zeit, die zugleich Auslöse- und Anzeigezeit ist. Deshalb:
`setAlarmClock` auf die **Weckzeit** (Statusleiste zeigt 06:30) und
`setExactAndAllowWhileIdle` auf den **Rampenstart**. Im Emulator belegt
(Android 16): Beide geben 10 s FGS-Freigabe
(`Background started FGS: Allowed … ALARM_MANAGER_WHILE_IDLE`). Fällt der
Rampen-Alarm aus, klingelt der Wecker trotzdem pünktlich, nur ohne Rampe.

**Risiko Android 16 „Audio Hardening“:** Beim Start aus dem Hintergrund
protokolliert das System `AudioHardening background playback would be muted …
level: full`. Aktuell ist das nur ein Probelauf (`mutedState:none`, Ton ist
zu hören). Wird die Durchsetzung scharf geschaltet, könnte eine Rampe bei
dunklem Bildschirm stumm bleiben. Absicherung: die Vollbild-`WakeActivity`
(sichtbares Fenster) zur Weckzeit. Bei jeder neuen Android-Version prüfen.

**Ohne `SCHEDULE_EXACT_ALARM`** lässt sich kein Wecker aktivieren. Der
Schalter führt dann in die Systemeinstellung. Einen „ungefähren“ Wecker mit
`setAndAllowWhileIdle` (kann mehrere Minuten daneben liegen) bieten wir nicht
an. Ein Wecker, der zu spät klingelt, ist schlimmer als keiner.

**Bekannte Grenze v1:** Startet das Telefon nachts neu und wird nicht
entsperrt, bleibt der Wecker stumm, weil die Wecker im verschlüsselten
DataStore erst nach dem ersten Entsperren lesbar sind. Kein `directBootAware`
in v1.

**Zu prüfen auf echten Geräten:** Samsung (Galaxy A54 des Nutzers) hat eine
eigene Energiesparverwaltung („Apps im Tiefschlaf“). `setAlarmClock` ist dort
ausgenommen, das muss aber einmal über Nacht belegt werden.

---

## 4. Die Weckrampe

### 4.1 Isochron statt binaural

Binaurale Beats entstehen erst im Kopf, aus zwei verschiedenen Signalen auf
dem linken und dem rechten Ohr. Über den Lautsprecher, und morgens liegen die
Kopfhörer meist nicht mehr im Ohr, gibt es keine Schwebung. **Die Rampen
verwenden `ToneType.ISOCHRONIC`**, das auch über den Lautsprecher wirkt. Ein
Umschalter „Ich schlafe mit Kopfhörern“ kann später binaural erlauben.

### 4.2 Fertige Rampen (Vorschlag, Dauer 20 Min.)

| Rampe | Phasen | Charakter |
|---|---|---|
| Sanft | 4 Hz 6′ → 7 Hz 7′ → 10 Hz 7′ | für langsames Aufwachen |
| Frisch | 6 Hz 5′ → 10 Hz 7′ → 14 Hz 8′ | Standard |
| Energisch | 8 Hz 4′ → 12 Hz 6′ → 18 Hz 10′ | für Tage mit frühem Termin |

Bei 10 oder 30 Minuten werden die Phasen anteilig gestreckt. Die Rampen sind
normale `List<Phase>` und laufen durch den vorhandenen `BinauralGenerator`.
**`Phase` wird nicht erweitert**, aus demselben Grund wie beim Rhythmus
(geteilte Links, `CustomPreset`).

### 4.2a Klangdesign-Prüfung (24.09.)

Nach dem ersten Hören auf dem Galaxy A54 („klingt nicht schön“) gemessen:

| Befund | vorher | nachher |
|---|---|---|
| Klick-Energie der Pulsflanken (weit außerhalb des Tons) | −23 dB | −71 dB (App), −80 dB (Wecker) |
| Träger / Schale | 200 / 262 Hz, 467 Cent, unter dem Lautsprecher-Arbeitsbereich | 392 / 523 Hz, 500 Cent (Quarte) |
| Schwebung der Schale | zwei gleich laute Sinus, Auslöschung bis 0 | Partner 30 %, ~5 dB Schimmern |
| Summe der Spuren | bis 1,69 (Begrenzer verzerrt) | ≤ 0,9 (`WakeRamps.levels`) |

**App-weiter Physikfehler dabei gefunden:** Generator und WAV-Export rechneten
`sin(2π·f(t)·t)`. Die gehörte Frequenz ist aber die Ableitung der Phase,
`f + t·f′`; bei jeder Modulation (BREATHING, SWEEP, PULSE, DYNAMIC) lief die
Schwebung mit der Session-Zeit davon (10-Hz-SWEEP nach 20 Min.: ±950 Hz).
Behoben durch Phasen-Akkumulatoren in `audio/ToneVoice.kt`, die Generator,
Export und Hörprobe gemeinsam nutzen.

### 4.3 Lautstärkekurve

Die Lautstärke steigt über die Rampe von ~0 auf die eingestellte
Wecklautstärke. Linear klingt sie zu Beginn zu schnell laut, weil das Gehör
logarithmisch hört. Deshalb eine Potenzkurve `v(t) = vMax · (t/T)^k` mit
k ≈ 2–3. Umgesetzt über `fadeScale`, das alle Engines schon haben und der
Sleep-Timer heute **abwärts** benutzt (`AudioPlaybackService.fadeStep`). Der
Wecker benutzt es **aufwärts**.

---

## 5. Architektur-Schnitt

### 5.1 Neue Dateien

| Datei | Rolle | Vorbild im Repo |
|---|---|---|
| `data/WakeAlarm.kt` | `@Serializable WakeAlarm(id, hour, minute, days, rampKey, rampMinutes, volume, ambient, enabled)` | `data/RhythmProgram.kt` |
| `data/WakeAlarmRepository.kt` | DataStore-Persistenz (JSON) | `data/PresetRepository.kt` |
| `alarm/WakeSchedule.kt` | **reine Funktion** `nextTrigger(alarm, now, zone)` → Instant | `ui/ReviewPromptDecision.kt` |
| `alarm/WakeRamps.kt` | Rampen-Definitionen + Streckung auf 10/20/30 Min. | `data/Preset.kt` |
| `alarm/AlarmScheduler.kt` | `setAlarmClock`/`cancel`, plant immer nur den **nächsten** Termin | — |
| `alarm/AlarmReceiver.kt` | `BroadcastReceiver`: Rampe starten, Folgetermin planen | — |
| `alarm/RescheduleReceiver.kt` | BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED | — |
| `ui/WakeActivity.kt` | Vollbild, `showWhenLocked`/`turnScreenOn`, Aus/Schlummern | — |
| `ui/screens/WakeAlarmSheet.kt` | Wecker-Liste + Bearbeiten | `ui/screens/RhythmSheet.kt` |
| `audio/ChimeVoice.kt` | Glockenklang synthetisieren | `audio/RhythmVoice.kt` (ClickVoice) |

### 5.2 Änderungen an bestehenden Dateien

- **`audio/BinauralGenerator.kt`, `AmbientEngine.kt`, `RhythmEngine.kt`**:
  `AudioAttributes` als Parameter von `start()` statt fest `USAGE_MEDIA`.
  Default bleibt `USAGE_MEDIA`, damit sich für alle bisherigen Aufrufer nichts
  ändert.
- **`service/AudioPlaybackService.kt`**: `startWakeRamp(alarm)`, Fade-**in**
  über einen zweiten Runnable neben `fadeStep`, Weckklang ab Weckzeit,
  Auto-Ende nach 15 Min., `snooze()`/`dismiss()`. `maybeExitForeground()`
  zählt den Weckzustand mit. Einem schon laufenden Wecker wird die Session
  nicht unter den Füßen weggezogen: Läuft beim Auslösen noch eine Session
  (Nutzer ist wach), gewinnt der Wecker und die Session stoppt.
- **`billing/Entitlements.kt`** + beide Impl: `wakeAlarmOwned`,
  `wakeAlarmPrice`, `purchaseWakeAlarm()`, `PRODUCT_WAKE_ALARM = "wake_alarm"`.
  Die Premium-Impl fragt beide Produkte in **einer** `queryProductDetails`-
  Anfrage ab.
- **`FeatureFlags.kt`** + beide Impl: `wakeAlarmAvailable` (premium true,
  foss false).
- **`AndroidManifest.xml`**: Das Manifest liegt heute nur in `main`. Die neuen
  Berechtigungen und Receiver gehören in ein **`premium`-Manifest**
  (`app/src/premium/AndroidManifest.xml`, wird von Gradle zusammengeführt).
  Sonst fragt der F-Droid-Build Wecker-Berechtigungen für ein Feature an, das
  er nicht enthält. Gegenprobe wie beim Billing: zusammengeführte Manifeste
  beider Flavours vergleichen.
- **Sleep-Timer** (`BinauralViewModel`, `onSleepTimerFinished`): Angebot
  „Morgen mit Weckrampe wecken“, nur wenn gekauft **und** die Berechtigung
  für exakte Alarme vorliegt.
- **`MainScreen.kt`**: Wecker-Icon in der zweiten Zeile der Werkzeugleiste
  (dort ist seit dem Rhythmus-Umbau Platz), mit Anzeige des nächsten Weckers.

### 5.3 Reine Funktionen mit Unit-Tests

- `nextTrigger(alarm, now, zone)`: Wochentage, „heute schon vorbei“ →
  nächster passender Tag, einmaliger Wecker, **Zeitumstellung** (am
  29.03./25.10. gibt es 02:30 einmal nicht bzw. zweimal).
- `rampStart = trigger − rampMinutes`, auch über Mitternacht (Wecker 00:10,
  Rampe 20 Min. → Start am Vortag 23:50).
- `stretch(ramp, minutes)`: Summe der Phasen = gewünschte Dauer, keine Phase 0.
- Lautstärkekurve `volumeAt(elapsed, total, k)`: monoton, 0 am Anfang,
  vMax am Ende.

**Wichtig:** Der Alarm wird auf den **Rampenstart** gestellt, nicht auf die
Weckzeit. Android zeigt in der Statusleiste und auf dem Sperrbildschirm aber
die Zeit aus `AlarmClockInfo` an. Dort muss deshalb die **Weckzeit** stehen,
nicht der Rampenstart. Sonst sieht die Nutzerin oder der Nutzer „06:10“,
obwohl der Wecker auf 06:30 gestellt ist.

---

## 6. Kauf, Store, Richtlinien

- **Produkt `wake_alarm`**, Einmalkauf, nicht verbrauchbar, 1,99 € (DE als
  Endpreis im Länderfeld eintragen, nicht über den Bulk-Editor, siehe
  Lehre vom 11.09.).
- **Reihenfolge wie beim Rhythmus:** Produkt lässt sich erst anlegen, wenn ein
  Build mit BILLING auf einem Track liegt. Das ist seit 1.3.0 der Fall, also
  kann `wake_alarm` **sofort** angelegt werden, noch vor dem Build.
- **Bestandskunden:** Nicht geschenkt. Der `firstSeenVersionCode`-Marker war
  für Käufer von 1.x und den Rhythmus gedacht. Für den Wecker gibt es keine
  Zusage.
- **Play-Erklärungen im Zuge des Releases:** (a) Vollbild-Intent-Erklärung,
  (b) Datensicherheit unverändert (alles lokal), (c) IARC nicht neu nötig
  (In-App-Käufe sind schon angegeben). Exakte Alarme brauchen mit
  `SCHEDULE_EXACT_ALARM` keine Erklärung.
- **Store-Text:** In der Kurzbeschreibung steht nichts von „Wecker“. Der
  Klangwecker ist ein neues Suchwort-Feld („sanfter Wecker“, „Weckrampe“). Das
  in de-DE **und** en-US ergänzen (siehe `international-verkaufen`).

---

## 7. Reihenfolge der Umsetzung

1. `WakeAlarm` + `nextTrigger` + `stretch` + `volumeAt` mit Tests (reine
   Kotlin-Logik, kein Gerät nötig)
2. `AudioAttributes` in den drei Engines parametrisieren (Default unverändert)
3. Scheduler + Receiver + Reschedule, zuerst mit einem Debug-Knopf
   „in 2 Min. wecken“
4. `WakeActivity` (Vollbild, Aus, Schlummern), Auto-Ende
5. `WakeAlarmSheet` + Icon + Kaufschranke + `premium`-Manifest
6. Sleep-Timer-Anschluss
7. Übernacht-Test auf dem Galaxy A54 (Doze, Samsung-Energiesparen, Neustart
   in der Nacht)
8. Version 1.4.0, Store-Texte, Screenshots neu, Produkt, Einreichung

**Test-Falle** aus dem Rhythmus: `adb -s emulator-5554` verwenden, das Galaxy
A54 hängt zeitweise mit am adb. Doze im Emulator erzwingen:
`adb shell dumpsys deviceidle force-idle`.
