# Gestaltung DeepWave: „Schwebung“

Festgelegt am 25.09.2026 (Nutzerentscheidung, Richtung A). Leitfaden: `../../_gestaltung/LEITFADEN.md`.
Entwurfsseite: https://claude.ai/artifact/GVxpMryCEVocFGZh2S1ZEX · Canva-Ordner „Apps · Gestaltung / DeepWave“ (Stimmung DAHWPXpO_y8).
Status: **festgelegt, nicht umgesetzt.** Umsetzung erst nach Freigabe von 1.4.0 (nicht in eine laufende Prüfung).

| Feld | Festlegung |
|---|---|
| **Leitmotiv** | Zwei Sinuswellen (links/rechts), deren Überlagerung als ruhig atmende Hülle erscheint, also das Prinzip der binauralen Schwebung. Ersetzt das Kopfhörer-Motiv. |
| **Palette dunkel (Standard)** | Grund Pflaumennacht `#1D0E1D`, Karten `#2F182F`, Akzent Orchidee `#E07BDE` (301°, 7,2 : 1 auf Grund), Text Mondweiß `#F1E9F4`, gedämpft `#BFA9C2`. |
| **Palette hell** | Grund Rosé-Grau `#F8F0F7`, Akzent-Textstufe `#8A2E8A` (6,7 : 1), Orchidee nur für Flächen, Text `#2A1530`. |
| **Schrift** | **Fraunces** (OFL, SOFT-Achse 100) für Titel und große Frequenzwerte; feste Schnitte per `fontTools.varLib.instancer` erzeugen. Fließtext System. |
| **Formen** | Weich und rund: Karten 24 dp ohne Rand, Wellenlinie statt Trennstrich, großer runder Start-Knopf mit zwei Hüllringen, Auswahl-Chips als Pillen. |
| **Icon** | Zwei verschränkte Wellen (Orchidee und Mondweiß) auf Pflaume; Monochrom-Icon = Wellenpaar. |

**Warum:** Violett liest sich als Nacht und ist der einzige ganz freie Farbton. Das Motiv erklärt, was die App tut.
Bisherige Farben (Navy `#1E3C72` = Kopfkarte-Nachtblau, Mint ≈ Nenne-drei-Grün) entfallen.

## Umsetzung (offen)
- [ ] `ui/theme/Theme.kt` nach `_gestaltung/vorlage/Theme.kt.vorlage` (BinauralColors-Werte ersetzen, Material-Schema anpassen, eigene Knopf-Hüllen)
- [ ] Fraunces bündeln + OFL-Lizenz, Lizenzansicht
- [ ] windowBackground hell/dunkel, Benachrichtigungsfarben (`PrimaryDark`/`SurfaceDark`)
- [ ] Icon (Canva-Motiv + Skript), Feature-Grafik 1024 × 500, KI-Kennzeichnung in der Console
- [ ] Am Gerät hell+dunkel prüfen, Store-Screenshots neu (fastlane de-DE/en-US), Endlektorat
