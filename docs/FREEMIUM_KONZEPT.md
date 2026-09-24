# Freemium — Umstellung von Kauf-App auf Gratis-Download mit Einmalkäufen

Stand: 2026-09-24. Anlass: DeepWave ist seit ~10.09. als Kauf-App (3,99 €)
in Produktion und hatte am 21.09. zwei Installationen. Eine Bezahl-App ohne
Bewertungen kommt in Play nicht aus dem Sichtbarkeitsloch; dazu stand im
Store „3,99 €“ *und* „enthält In-App-Käufe“ — eine doppelte Schranke, hinter
der die Add-ons niemand sieht, der nicht schon gezahlt hat.

Markenversprechen bleibt: **kein Abo, keine Werbung, kein Konto, offline.**

## Entscheidungen (24.09.)

| Frage | Entscheidung |
|---|---|
| Modell | Freemium: Gratis-Download + Einmalkäufe |
| Gratis-Umfang | wie der F-Droid-Build: Standard-Presets, Atemführung, Journal, 3 eigene Presets; **kein** Mixer, **kein** Sleep-Timer, kein WAV-Export, keine Statistik, keine Premium-Presets |
| Premium | Einmalkauf `premium`, 3,99 € (= bisheriger App-Preis): Premium-Presets, Mixer + Sleep-Timer, WAV-Export, Statistik, unbegrenzt eigene Presets |
| Add-ons | `rhythm_layer`, `wake_alarm` je 1,99 €, **auch ohne Premium** kaufbar |
| Komplettpaket | `complete`, 5,99 € (Einzelpreise zusammen 7,97 €); nur angeboten, solange man noch nichts davon besitzt — Play kann einen Paketpreis nicht um Gekauftes mindern |
| Gesperrtes | bleibt sichtbar, mit Schloss; Antippen öffnet die Freischalt-Karte |
| Bestandskunden | behalten Premium (sie haben dafür bezahlt) — Add-ons nur, wenn gekauft |
| Release | 1.4.0 (Code 9) bleibt im internen Test; Klangwecker + Freemium gehen zusammen als **1.4.0 (Code 10)** |

## Bestandskunden erkennen

`firstSeenVersionCode` (seit 1.2.1, `data/FirstRunMarker.kt`) wurde genau
dafür eingebaut. Jede Premium-Installation, die **vor** dem Freemium-Release
zum ersten Mal lief, stammt aus der Kauf-App-Zeit (oder von einem Tester):

- `PRE_MARKER` (0) → vor 1.2.1 installiert → Bestandskunde
- `1 … FREEMIUM_VERSION_CODE-1` → Bestandskunde
- `≥ FREEMIUM_VERSION_CODE` → neu, Gratis-Umfang

Grenze: Wer die Kauf-App installiert, aber nie geöffnet hat und direkt auf
die Freemium-Version aktualisiert, oder die App nach der Umstellung neu
installiert, wird als neu eingestuft (Play gibt den App-Kauf nicht über die
Billing-API heraus). Bei zwei Installationen vertretbar; Support-Weg: per
Mail, Freischaltung über Gutscheincode des Produkts `premium`.

## Architektur

- **FeatureFlags** (Build-Zeit) bleibt die Antwort auf „ist das im Build?“.
  Im Premium-Flavour ist alles enthalten, im FOSS-Flavour nichts davon.
- **Access** (neu, Laufzeit) beantwortet „darf dieser Nutzer?“:
  `premium = besitzt premium ∨ besitzt complete ∨ Bestandskunde`,
  `rhythm = besitzt rhythm_layer ∨ complete`, `wake = besitzt wake_alarm ∨ complete`.
  Reine Funktion mit Tests.
- **Entitlements** kennt vier Produkte (`premium`, `complete`, `rhythm_layer`,
  `wake_alarm`), weiterhin eine gemeinsame Abfrage.
- Eine gemeinsame **Freischalt-Karte** für Premium (mit Paket-Angebot),
  geöffnet von jedem gesperrten Element.

## Reihenfolge der Veröffentlichung (zwingend)

1. Produkte `premium` (3,99 €) und `complete` (5,99 €) in der Console anlegen.
2. Release 1.4.0 (Code 10) mit Laufzeit-Sperre und Bestandskunden-Erkennung
   durch interne Tests → Produktion, **live abwarten**.
3. **Erst dann** den App-Preis in der Console auf „kostenlos“ stellen.
   Umgekehrt bekäme jeder Neuinstallierer die volle Version geschenkt.
4. **Nicht umkehrbar:** Eine kostenlose Play-App kann nie wieder
   kostenpflichtig werden.
