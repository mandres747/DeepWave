# Rhythmus-Layer — Konzept für ein kostenpflichtiges Add-on

Stand: 2026-09-09. Ausgangspunkt: der Einzelprojekt-Prototyp
`Apps/Kadenz-Audio-Generator` (PWA für Lauftraining) soll nicht als Ganzes in
DeepWave wandern, sondern nur sein überlappender Kern — eine hörbare
Taktvorgabe auf der bestehenden Session-Zeitleiste. Verkauft als einmaliges
In-App-Produkt.

Dieses Dokument beschreibt den Schnitt durch die vorhandene Architektur, nicht
die fertige Implementierung. Es ist als Vorlage für die 2.0-Planung gedacht,
**nachdem** 1.2.0 live ist und ein paar Wochen Nutzungsdaten vorliegen.

---

## 1. Warum das überhaupt zusammenpasst

DeepWave und der Kadenzgenerator sind im Kern dieselbe Sache: *eine Zeitleiste
aus Phasen, jede mit einem Audiocharakter und optionaler gesprochener Führung,
offline erzeugt.* Drei Dinge im Repo belegen das:

- **`ToneType.ISOCHRONIC` ist bereits ein Metronom.** In
  `BinauralGenerator.generateAudio()` steht
  `val pulse = if (sin(2 * PI * beatFreq * t) > 0) 1f else 0f` — ein hart
  getakteter Puls mit `beatFreq` Hz. Eine Laufkadenz von 180 Schritten/min
  sind 3 Hz. Der Unterschied zum Rhythmus-Layer ist nur, dass dieser Puls
  aktuell einen Trägerton *gated*, statt einen eigenen Klick zu erzeugen.
- **`AmbientEngine` ist die fertige Vorlage für eine zweite Klangquelle.**
  Eigener `AudioTrack`, eigener Thread, `fadeScale` für den Sleep-Timer,
  `hasActiveLayer()`, läuft unabhängig von einer Session oder darüber gelegt.
  Ein `RhythmEngine` ist dessen Geschwister, nicht ein Eingriff in bestehenden
  Code.
- **`BreathingPattern(inhale, hold1, exhale, hold2)`** in
  `ui/components/BreathingGuide.kt` liefert bereits Sekundenwerte. Daraus eine
  hörbare Taktung abzuleiten ist Arithmetik, keine neue Domäne — und es ist
  das Feature, das am wenigsten nach „aus einer anderen App geliehen" aussieht.

Die Nische, die daraus entsteht: **Entrainment für Kopf und Körper auf einer
Zeitleiste, offline, ohne Konto, ohne Abo.** Meditations-Apps haben kein
Metronom, Lauf-Apps kein Entrainment.

---

## 2. Umfang v1

**Drin:**

1. **Puls-Spur** — Klick/Woodblock/weicher Tick, BPM 40–200 einstellbar,
   eigene Lautstärke, wahlweise mit Betonung auf jedem n-ten Schlag. Läuft mit
   oder ohne laufende Session.
2. **Rhythmus-Programm** — Liste aus Schritten (BPM + Dauer in Minuten),
   analog zu den Phasen einer Session. Warm-up 110, Gehen 120, Auslaufen 100.
3. **Atemtaktung** — BPM aus dem gewählten `BreathingPattern` abgeleitet, so
   dass die sichtbare Atemführung und der hörbare Takt übereinstimmen.
4. **Ansagen** — Android `TextToSpeech` bei Schrittwechseln und in Intervallen
   („Tempo 120", „noch zwei Minuten"). Keine proprietäre Abhängigkeit.

**Bewusst draußen (das bleibt beim Kadenzgenerator bzw. entfällt):**

- GPX-Import, Streckenprofile, Garmin-Optimierung
- MP3-Export (bräuchte LAME als native Lib — kollidiert mit F-Droids
  reproduzierbaren Builds und bläht die App auf; WAV gibt es schon)
- Intervallpläne mit Wiederholungslogik, Coach-Modus, Affirmationen

Wenn sich später zeigt, dass die Lauf-Zielgruppe trägt, gehört das in eine
**eigene App mit gemeinsamem `:audio-engine`-Modul** — nicht in DeepWave. Der
Preis dafür ist bekannt: eine neue App heißt neuer Closed Test, wieder 12
Tester, wieder 14 Tage.

---

## 3. Architektur-Schnitt

### 3.1 Kein Eingriff in `Phase`

`Phase` ist `@Serializable` und wird an zwei Stellen persistiert bzw.
weitergegeben: in `CustomPreset` (DataStore) und in geteilten Session-Links
(`MainActivity.handleDeepLink` → `viewModel.importFromUri`). Ein neues Feld
wäre mit Default zwar abwärtskompatibel, aber eine ältere App-Version würde
einen geteilten Link stillschweigend ohne Rhythmus öffnen — der Empfänger
bekäme etwas anderes zu hören als der Absender.

**Deshalb: der Rhythmus ist eine eigene Spur, kein Phasen-Attribut.** Das hält
das Teilen kompatibel und macht die Kaufschranke sauber — ein nicht gekaufter
Layer ist schlicht eine Spur, die nicht läuft.

### 3.2 Neue Dateien

| Datei | Rolle | Vorbild im Repo |
|---|---|---|
| `audio/RhythmEngine.kt` | eigener `AudioTrack`, Klicksynthese, `fadeScale`, `hasActiveLayer()` | `audio/AmbientEngine.kt` |
| `data/RhythmProgram.kt` | `RhythmStep(bpm, durationMinutes, accentEvery)` + `@Serializable` | `data/Preset.kt` |
| `data/RhythmRepository.kt` | DataStore-Persistenz der Programme | `data/PresetRepository.kt` |
| `audio/SpokenCues.kt` | `TextToSpeech`-Wrapper, Ansagen bei Schrittwechsel | — |
| `ui/screens/RhythmSheet.kt` | Bedienoberfläche | `ui/screens/MixerSheet.kt` |
| `ui/RhythmTimeline.kt` | reine Funktion `stepAt(elapsedSeconds)` | `ui/ReviewPromptDecision.kt` |
| `billing/Entitlement.kt` | Interface im `main`-SourceSet | `FeatureFlags.kt` |

### 3.3 Änderungen an bestehenden Dateien

- **`service/AudioPlaybackService.kt`** — dritter Member `val rhythm = RhythmEngine()`
  neben `generator` und `ambient`. Der Sleep-Timer muss dessen `fadeScale`
  mitziehen (`SleepTimerTask`), und `maybeExitForeground()` muss den Layer
  mitzählen, sonst endet der Vordergrunddienst, während der Puls noch läuft.
- **`ui/BinauralViewModel.kt`** — `showRhythm`, Programmzustand,
  Entitlement-Zustand. Muster wie beim Mixer.
- **`ui/screens/MainScreen.kt`** — Icon in der oberen Leiste, wie das
  Mond-Icon des Mixers; sichtbar nur bei `features.rhythmLayerEnabled`.
- **`audio/WavExporter.kt`** — der Export rendert die Session offline; damit
  ein Export das enthält, was man gehört hat, muss die Puls-Spur mitgemischt
  werden. *Nebenbei:* `WavExporter` dupliziert `applyModulation` aus
  `BinauralGenerator` bereits heute. Bevor ein dritter Aufrufer dazukommt,
  sollte diese Funktion in eine gemeinsame Datei wandern.
- **`FeatureFlags.kt`** + beide `FeatureFlagsImpl` — `billingEnabled`,
  `rhythmLayerEnabled`.

### 3.4 Zeitleisten-Logik als reine Funktion

Wie bei `ReviewPromptDecision`: die Entscheidung „welcher Schritt gilt bei
Sekunde *n*" gehört in eine testbare Funktion ohne Android-Laufzeit.

```kotlin
fun stepAt(program: List<RhythmStep>, elapsedSeconds: Int): RhythmStep?
```

Damit sind Randfälle (leeres Programm, Überlauf am Ende, Schritt mit Dauer 0)
im Unit-Test festgenagelt statt im Audio-Thread.

---

## 4. Kauf und Berechtigung

### 4.1 Produktform

Ein **einmaliges, nicht verbrauchbares In-App-Produkt** (`INAPP`), z. B.
`rhythm_layer`. Kein Abo — das passt nicht zu einer App, die als Kauf-App ohne
Konto positioniert ist.

Preis: offene Entscheidung. Die App kostet 3,99 €; ein Add-on in der
Größenordnung 2–3 € wirkt stimmig, ist aber Sache des Portfolios
(`wunderkammer/docs/PORTFOLIO_ERLOESE_2026-09.md`).

### 4.2 Flavour-Trennung ist Pflicht

Die Play Billing Library ist proprietär. Sie darf **nicht** in den
FOSS-Flavour, sonst lehnt F-Droids Scanner den Build ab. Das Muster steht seit
1.2.0 bereits im Repo — `FeatureFlags.storeUrl` ist im FOSS-Build `null`:

```
main/    Entitlement (Interface)
premium/ EntitlementImpl → BillingClient, queryPurchasesAsync
foss/    EntitlementImpl → immer gewährt, keine Billing-Abhängigkeit
```

F-Droid-Nutzer bezahlen ohnehin nie; ihnen den Layer zu schenken kostet nichts
und hält den Build sauber.

### 4.3 Wiederherstellen

`queryPurchasesAsync` **ist** die Wiederherstellung — Play kennt den Kauf auf
jedem Gerät desselben Kontos. Ein sichtbarer Eintrag „Kauf wiederherstellen"
in den Einstellungen ist trotzdem sinnvoll, weil Nutzer das erwarten und sonst
den Support suchen.

### 4.4 Bestandskäufer nicht vor den Kopf stoßen

Wer 3,99 € bezahlt hat und danach eine zweite Bezahlschranke sieht, schreibt
das gern in eine Rezension. Zwei Gegenmittel:

1. Den Layer als **eigenständigen neuen Bereich** auftreten lassen, nicht als
   Beschneidung von etwas Vorhandenem. Nichts, was heute funktioniert, darf
   hinter die Schranke wandern.
2. Käufern der 1.x-Versionen den Layer **schenken**.

Für (2) gibt es ein Problem: Play Billing sagt nicht, *wann* jemand die App
gekauft hat, und `firstInstallTime` aus dem `PackageManager` ist unzuverlässig
(Neuinstallation, Gerätewechsel).

> **Erledigt am 09.09.2026 (versionCode 4).** `data/FirstRunMarker.kt` +
> `SettingsRepository.ensureFirstSeenVersionCode()` schreiben den Marker
> einmalig beim ersten Start und rühren ihn danach nie wieder an. Aufgerufen
> im `init` des ViewModels, **vor** allem anderen, was den Settings-Store
> berührt — der Marker erkennt „Neuinstallation" daran, dass der Store leer
> ist.
>
> Der knifflige Fall ist der Upgrade-Pfad: Wer eine frühe Version installiert
> hat und erst viel später direkt auf die Add-on-Version springt, kommt dort
> **ohne** Marker an. Würde man ihm dann den aktuellen versionCode
> schreiben, wäre ein Bestandskunde als Neukunde einsortiert. Deshalb gilt:
> kein Marker + vorhandene Einstellungen ⇒ `PRE_MARKER` (= 0), was unter jedem
> echten versionCode liegt. Festgenagelt in `FirstRunMarkerTest`.

### 4.5 Play-Console-Folgen

- In-App-Produkt anlegen (Monetarisierung → Produkte → In-App-Produkte).
- Der Store-Eintrag bekommt automatisch das Badge „In-App-Käufe".
- Der **Content-Rating-Fragebogen fragt nach Käufen** — er muss neu beantwortet
  werden, sonst blockiert das die Veröffentlichung. Antworten pflegen in
  `playstore/CONTENT_RATING.md`.
- `playstore/RELEASE_CHECKLIST.md` widerspricht dem Vorhaben derzeit
  ausdrücklich („Option A (Kaufpreis) — kein IAP-Code nötig"). Der Abschnitt
  muss umgeschrieben werden, sonst führt er künftig in die Irre.

---

## 5. Risiken

| Risiko | Gegenmaßnahme |
|---|---|
| Billing-Abhängigkeit gerät in den FOSS-Build | Flavour-SourceSets, wie bei `storeUrl`; F-Droid-Build vor jedem Release lokal prüfen |
| Dritter `AudioTrack` + Thread kostet Akku | `AmbientEngine` zeigt, dass zwei tragbar sind; den dritten messen, bevor er ausgeliefert wird |
| Zwei Zielgruppen in einem Store-Eintrag | Screenshots und Beschreibung bleiben bei Schlaf/Fokus/Meditation; der Rhythmus ist ein Nebensatz, kein zweites Versprechen |
| Metronom im Kopfhörer draußen | Ein Hinweis beim ersten Start des Layers — die App darf nicht dazu einladen, im Straßenverkehr abgeschottet zu laufen |
| Geteilte Session-Links werden inkompatibel | Genau deshalb kein neues Feld in `Phase` (siehe 3.1) |

---

## 6. Reihenfolge

1. ~~**Jetzt (1.2.x):** `firstSeenVersionCode`-Marker setzen.~~ **Erledigt**
   in versionCode 4 — geht mit dem nächsten Release raus.
2. **Datenlage abwarten:** Wird die Preset-Kategorie „Sport & Training"
   überhaupt benutzt? Wenn sie tot bleibt, baust du das Add-on für niemanden.
3. **Dann Schritt für Schritt:** `RhythmEngine` + `stepAt` mit Tests →
   `RhythmSheet` ohne Kaufschranke intern testen → Entitlement +
   Billing → Play-Console-Produkt → Content-Rating neu → Release.

Schritt 1 war der einzige zeitkritische: Jeder Nutzer, der DeepWave vor dem
Marker installiert, ist später nur noch über die `PRE_MARKER`-Regel als
Bestandskunde erkennbar — und die greift nur, wenn er den Store bereits
benutzt hat.

---

Verwandt: [`IDEEN.md`](IDEEN.md), [`../playstore/RELEASE_CHECKLIST.md`](../playstore/RELEASE_CHECKLIST.md)
