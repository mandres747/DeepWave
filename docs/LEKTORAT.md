# Endlektorat DeepWave

## 2026-09-26 – Version 1.5.0 (12)

**Umfang:** `app/src/main/res/values/strings.xml` (de), `values-en/strings.xml` (en),
`fastlane/metadata/android/{de-DE,en-US}/changelogs/12.txt` (Play-Versionshinweise),
`playstore/STORE_LISTING.md` (Store-Texte de/en, Freemium). Nur geprüft, nicht geändert:
Bildtexte in `playstore/make_marketing_screenshots.py` (CAPTIONS) und
`playstore/make_store_graphics.py` (Feature-Grafik), `playstore/privacy_policy_{de,en}.html`.
Abgleich der Aussagen gegen `billing/Access.kt`, `FeatureFlagsImpl` (foss/premium),
`docs/FREEMIUM_KONZEPT.md`, UI-Code (Sperren in `MainScreen.kt`, `SleepTimerWakeRow`)
und das gemergte Release-Manifest.

### Änderungen

**strings.xml (de) – 22**

| Schlüssel | alt | neu | Grund |
|---|---|---|---|
| wav_export | WAV Export | WAV-Export | Zusammensetzung mit Abkürzung |
| share_subject | DeepWave Session | DeepWave-Session | Zusammensetzung mit Eigenname |
| notif_channel_name | DeepWave Wiedergabe | DeepWave-Wiedergabe | dto. |
| modified_preset_name | %1$s (Angepasst) | %1$s (angepasst) | Adjektiv, kein Satzanfang |
| stat_avg_rating | Ø Rating | Ø Bewertung | ein Begriff: sonst überall „bewerten“ |
| time_format | %1$dh %2$dm | %1$d h %2$d min | „m“ ist Meter; Einheit mit Abstand |
| stats_hours_short | %1$dh %2$dm | %1$d h %2$d min | dto. |
| preset_full_night | 8h Schlafzyklus | 8-h-Schlafzyklus | Durchkopplung Zahl + Einheit + Wort |
| preset_wbtb | WBTB Klartraum | WBTB-Klartraum | Zusammensetzung mit Abkürzung |
| cat_premium_focus | Premium: Focus-Bundle | Premium: Fokus-Bundle | sonst überall „Fokus“ |
| cat_isochronic | Isochronische Töne | Isochrone Töne | ein Begriff, wie Store-Eintrag (Duden: isochron) |
| guide_gamma_trigger | »Ich bin wach im Traum.« | „Ich bin wach im Traum.“ | einheitlich „deutsche Anführungszeichen“ (wie wake_exact_*) |
| guide_wbtb_intent | »Ich werde im Traum bewusst.« | „…“ | dto. |
| guide_mind_awake | »Mind Awake, Body Asleep.« | „…“ | dto. |
| guide_vibrations | »nach oben rollen« | „nach oben rollen“ | dto. |
| guide_deep_cycle1 | Tiefschlaf Zyklus 1. | Tiefschlafzyklus 1. | Zusammenschreibung |
| guide_deep_cycle2 | Tiefschlaf Zyklus 2. | Tiefschlafzyklus 2. | dto. |
| guide_gamma_wbtb | ~4.5h | ~4,5 h | Dezimalkomma, Abstand vor Einheit |
| onboarding_3_body | unabhängig zur Session | unabhängig von der Session | Rektion |
| rhythm_locked_body | keine Abos | kein Abo | einheitlich mit wake_locked_body/premium_intro |
| sleep_wake_existing | Wecker %1$s ist gestellt | Wecker um %1$s ist gestellt | Satz vollständig (Belegung „Wecker 07:00 ist …“) |
| premium_promise | mit demselben Konto | mit demselben Google-Konto | Widerspruch zu „kein Konto“ im selben Blatt aufgelöst |

**strings.xml (en) – 10**

| Schlüssel | alt | neu | Grund |
|---|---|---|---|
| guide_gamma_trigger, guide_wbtb_intent, guide_mind_awake, guide_vibrations (4) | "…" | “…” | **Fehler:** unmaskierte `"` entfernt aapt beim Bauen – die Anführungszeichen fehlten in der App |
| onboarding_1_body | Headphones are what make it work. | It works best with headphones. | gleiche Fakten wie de („am besten“); isochrone Töne gehen auch ohne |
| preset_jet_lag | Jetlag Reset | Jet Lag Reset | engl. Schreibung |
| wake_days | Weekdays | Days | „weekdays“ = Mo–Fr, kollidiert mit wake_weekdays „Mon–Fri“ |
| wake_days_none_hint | Without a weekday the alarm rings once … | With no day selected, the alarm rings once … | dto. |
| wake_locked_body | Multiple alarms with weekdays, …, fading ambient sound and … | Multiple alarms on chosen days, …, an ambient fade-in and … | dto.; Begriff wie wake_ambient „Ambient fade-in“ |
| premium_promise | same account | same Google account | wie de |

**changelogs/12.txt – de 0, en 1**

- en: „Sound alarm: stopping it during the wake-up ramp no longer rings again at the wake time“
  → „Sound alarm: tapping “Stop” during the wake-up ramp now also cancels the alarm at the wake time“
  (bezugsloses Gerundium; Knopf heißt in der App „Stop“). Längen: de 284, en 260 Zeichen.

**STORE_LISTING.md – 6**

- de Klangwecker: „Beim Stellen des Sleep-Timers direkt „Morgen wecken““ → „Mit Premium direkt beim Sleep-Timer „Morgen wecken““
  (die Zeile sitzt im Sleep-Timer-Blatt, und das ist Premium; der Klangwecker selbst ist ohne Premium kaufbar).
- de: „mit demselben Konto“ → „mit demselben Google-Konto“.
- en: Überschrift „INCLUDED“ → „INCLUDED FREE“ (gleiche Aussage wie „KOSTENLOS ENTHALTEN“).
- en: „Multiple alarms with weekdays“ → „Multiple alarms on chosen days“.
- en: „"Wake me tomorrow" right where …“ → „With Premium, “Wake me tomorrow” right where …“.
- en: „same account“ → „same Google account“.

Längen unverändert im Rahmen: Titel 24, Kurzbeschreibung de 78 / en 77 Zeichen.

### Inhaltlich geprüft und korrekt

- Gratis-Umfang = F-Droid-Umfang: 35 Standard-Presets („über 30“ stimmt), Atemführung (4 Muster),
  Journal, Teilen per Link, 3 eigene Presets (`Access.FREE_CUSTOM_PRESETS`).
- Premium: Premium-Presets, Mixer mit 8 Klängen + Sleep-Timer, WAV-Export, Statistik, unbegrenzt eigene
  Presets – deckt sich mit `requirePremium()`-Sperren. Add-ons ohne Premium kaufbar, Paket nur solange
  nichts besessen wird („günstiger als einzeln“: 5,99 € < 7,97 €). Bestandskunden-Regel stimmt.
- Keine Preise in App-Texten (kommen von Play als `%1$s`), keine Heilversprechen im Store-Text.

### Bewusst belassen

- Abschnittstitel (*_header) in normaler Schreibung – Vorgabe.
- Anglizismen als App-Wortschatz: Session, Preset, Sleep-Timer, Ambient, „Reset“ (10-sp-Knopf, „Zurücksetzen“
  wäre deutlich länger), „Carrier-Frequenz“ (Fachwort wäre „Trägerfrequenz“ – Geschmacksfrage, einheitlich verwendet).
- „DeepWave Premium“ (Produktname), englische Preset-Titel (Deep Focus, Flow State, Power Nap …).
- „min“ (Sleep-Timer, Phasen) neben „Min.“/„Std.“ (Klangwecker) – beide korrekt, je Bereich einheitlich.
- Englisch: gemischte Groß-/Kleinschreibung in Knöpfen/Labels (Title Case vs. sentence case) – kein Fehler,
  aber uneinheitlich; Vereinheitlichung wäre eine eigene Runde.
- „Ruhige Stirn“ / „Quiet Mind“ – freie, sinngleiche Übertragung.

### Offen (nicht in diesen Dateien lösbar oder Entscheidung nötig)

1. **Plural ohne `<plurals>` (braucht Kotlin-Änderung, `getQuantityString`):** `phases_info` („1 Phasen“ – eine Phase
   ist möglich, `removePhase` lässt 1 übrig), `journal_entries` („1 Einträge“), `stars_desc` („1 Sterne“, Screenreader),
   `rhythm_program_total` („1 Schritte“), Serie in `StatisticsSheet` („1 Tage“ aus Zahl + `stats_days`). Englisch
   gleichermaßen („1 phases“, „1 entries“, „1 stars“, „1 steps“, „1 days“).
2. **Onboarding stellt Premium-Funktionen ohne Hinweis vor:** Seite 3 (Ambient-Sounds und Sleep-Timer) und Seite 4
   („Journal und Statistik“) – Mixer, Sleep-Timer und Statistik sind seit 1.4.0 Premium. Vorschlag: Titel
   „Ambient-Sounds und Sleep-Timer (Premium)“ bzw. im Text „Mit Premium: …“.
3. **Preset-Namen „ADHS Deep Work“ / „ADHS Beruhigung“ (en „ADHD …“)** nennen weiter eine Diagnose, obwohl
   aa263a4 medizinische Namen neutralisiert hat; außerdem fehlt der Bindestrich („ADHS-Beruhigung“). Vorschlag:
   „Fokus Deep Work“/„Innere Ruhe“ o. Ä. – Entscheidung nötig.
4. **„Tonart“ (tone_type_label)** heißt musikalisch Dur/Moll; gemeint ist die Klangform. Vorschlag „Klangart“
   (auch in der Screenshot-Unterzeile 03).
5. **en guide_solf_963 „Connection with the Higher.“** klingt unidiomatisch; Vorschlag „Connection with the higher self.“
6. **„Premium: Fokus-Bundle“** – „Bundle“ kann mit dem Kauf-„Komplettpaket“ verwechselt werden (en „Focus Bundle“ vs.
   „Complete bundle“); Vorschlag „Premium: Fokus-Set“ / „Premium: Focus Set“.

**Bildtexte (nur gemeldet, Dateien werden parallel bearbeitet)**

- `make_marketing_screenshots.py`, en 05: „A breathing guide\nthat runs along“ – unidiomatisch.
  Vorschlag: „A breathing guide\nthat keeps pace“.
- en 02: „Sleep, focus, meditation, creativity, sport and more“ – Store-Text sagt „sports“ (US).
  Vorschlag: „… creativity, sports and more“.
- de 07: „Kurzer Rundgang beim Start – jederzeit überspringbar“ – der Rundgang kommt nur beim ersten Start (en sagt
  „on first launch“). Vorschlag: „Kurzer Rundgang beim ersten Start – jederzeit überspringbar“.
- de 03 / en 03: „Tonart“ / „tone“ – siehe Offen 4.
- de 06: „finde, was wirklich wirkt“ – leichte Wirkungsbehauptung; App sagt „was für dich wirklich funktioniert“.
  Vorschlag: „finde, was dir wirklich guttut“ (en „what actually works for you“).
- Übrige Zeilen stimmen: „Mehr als 30 Presets“ (35 gratis), Mixer und Statistik als „Premium:“ gekennzeichnet.
- `make_store_graphics.py`: „Binaurale Beats zum Einschlafen und Fokussieren“ / „Binaural beats for sleep and focus“ – in Ordnung.

**Datenschutzerklärung (nur gemeldet) – inhaltlich veraltet, vor dem Upload zu aktualisieren**

Stand „Version 1.0, 26. Mai 2026“; seit Freemium/Klangwecker stimmen mehrere Aussagen nicht mehr:

1. „keine INTERNET-Berechtigung“ / „Es werden keine Berechtigungen für Internet … angefordert“ / „kommuniziert mit
   keinem Server“ – **falsch:** das gemergte Release-Manifest enthält `INTERNET` und `ACCESS_NETWORK_STATE` (aus der
   Play Billing Library) sowie `com.android.vending.BILLING`. Vorschlag: „Die App selbst sendet keine Daten. Für Käufe
   nutzt sie Google Play Billing; die Abwicklung erfolgt durch Google (Datenschutzerklärung von Google). Die dafür
   nötigen Berechtigungen INTERNET, ACCESS_NETWORK_STATE und BILLING bringt die Billing-Bibliothek mit.“
2. Berechtigungstabelle unvollständig: es fehlen `SCHEDULE_EXACT_ALARM` (pünktlicher Klangwecker),
   `USE_FULL_SCREEN_INTENT` (Vollbild-Wecker), `RECEIVE_BOOT_COMPLETED` (Wecker nach Neustart), `WAKE_LOCK`, `VIBRATE`,
   `BILLING`, `INTERNET`, `ACCESS_NETWORK_STATE`.
3. Lokal gespeicherte Daten unvollständig: Wecker (Zeiten, Wochentage, Einstellungen), Rhythmus-Programme,
   Premium-Status/Kaufinformationen von Google Play, erster App-Start (Versionsnummer für die Bestandskunden-Regel).
4. Neuer Abschnitt „Käufe“: Einmalkäufe über Google Play; Zahlungsdaten erhält nur Google, nicht der Entwickler.
5. Untertitel „Brain Entrainment“ – App nennt es „Brainwave Entrainment“.
6. Anrede „Sie“ in der Erklärung gegenüber „du“ in App und Store – bei Rechtstexten vertretbar, sonst angleichen.
7. Datum/Version aktualisieren. Folgeprüfung: `playstore/DATA_SAFETY.md` begründet „Not applicable … keine
   INTERNET-Berechtigung“ – ebenfalls überholt (Antwort im Formular ggf. unverändert, Begründung anpassen).

### Nacharbeiten zu den offenen Punkten (26.09.2026, gleiche Version)

- **Plural:** `phases_info`, `stars_desc`, `journal_entries`, `rhythm_program_total` und die Serie („1 Tag“/„5 Tage“) sind jetzt `<plurals>` und werden mit `pluralStringResource` aufgelöst (de/en). `stats_days` entfällt.
- **Premium im Onboarding:** Seite 3 heißt jetzt „Ambient-Sounds und Sleep-Timer (Premium)“, auf Seite 4 steht „Die Statistik gehört zu Premium.“ (en entsprechend).
- **Gesundheitsbezug:** „ADHS Deep Work“ → „Deep Work intensiv“ (en: „Intense Deep Work“), „ADHS Beruhigung“ → „Innere Ruhe“ (en: „Inner Calm“). Das folgt der Linie des Commits aa263a4. Die internen Schlüssel bleiben.
- **„Tonart“ → „Klangart“**, en „Tone Type“ → „Sound type“.
- **963 Hz:** „Stille Versenkung.“ / „Quiet contemplation.“ statt „Verbindung mit dem Höheren“ / „Connection with the Higher“.
- **Bildtexte** (make_marketing_screenshots.py): Die Vorschläge wurden übernommen, dazu ein „Premium:“-Vorsatz bei Mixer und Statistik.
- **Datenschutzerklärung** de/en komplett neu (Version 1.5, 26.09.2026):
  - Käufe über Google Play Billing inklusive INTERNET/ACCESS_NETWORK_STATE/BILLING.
  - Alle zehn Berechtigungen aus dem Release-Manifest.
  - Lokale Daten: Wecker, Rhythmus-Programme, Versionsmarke.
  - Android-Sicherung (`allowBackup`).
  - Der Quellcode-Link zeigte auf ein nicht existierendes Repo (404) und führt jetzt auf github.com/mandres747/DeepWave.
  - Die Erklärung duzt jetzt wie die App.
  - `DATA_SAFETY.md` begründet „Not applicable“ jetzt richtig. Die Antwort im Formular bleibt „No“.
- **Layout:** Die Journal-Zahl im Werkzeugknopf brach ab 10 Einträgen auf zwei Zeilen um (Emulator, 19 Einträge). Behoben: `maxLines = 1`, weniger Innenabstand.

Bewusst belassen: „static“ als Modulationsname in der Phasenliste (Fachwert), englisches Datumsformat im Journal (tt.mm.jj), die übrigen Solfeggio-Bezeichnungen (traditionelle Zuschreibungen, kein Heilversprechen).
