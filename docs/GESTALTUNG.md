# Gestaltung DeepWave: „Schwebung“

Festgelegt am 25.09.2026 (Nutzerentscheidung, Richtung A). Leitfaden: `../../_gestaltung/LEITFADEN.md`.
Entwurfsseite: https://claude.ai/artifact/GVxpMryCEVocFGZh2S1ZEX · Canva-Ordner „Apps · Gestaltung / DeepWave“ (Stimmung DAHWPXpO_y8).
Status: **umgesetzt in 1.5.0 (12)**, 26.09.2026. Interner Test sofort, Produktion nach Freigabe von 1.4.0.

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

## Umsetzung (1.5.0)
- [x] `ui/theme/Theme.kt`: Palette hell/dunkel, `warning` als eigene Signalfarbe, `TitleFont`, Material-Typografie für Titel
- [x] Fraunces als fester Schnitt `res/font/fraunces_soft_semibold.ttf` (opsz 72, wght 600, SOFT 100), OFL in `assets/licenses/`
- [x] Abschnittstitel in Fraunces und normaler Schreibung statt gesperrter Versalien, Ecken 18/20/24 dp, Sheets 28 dp
- [x] Runder Start-Knopf mit zwei Hüllringen, gestrichelte Schwebungshülle in der Wellenanzeige
- [x] windowBackground und Splash (v31) Pflaume
- [x] Icon: Adaptive + Monochrom aus derselben Geometrie, Benachrichtigungssymbol `ic_stat_wave`; Store-Icon und Feature-Grafik per `playstore/make_store_graphics.py` (Wellenband aus Canva-Entwurf DAHWPXpO_y8 = KI-Asset)
- [x] Emulator hell und dunkel geprüft, 2 × 8 Store-Screenshots neu (dunkel), Endlektorat `docs/LEKTORAT.md`
- [ ] Am A54 ansehen (nach Installation aus dem internen Test)
- [ ] KI-Kennzeichnung in der Console, sobald die Feature-Grafik hochgeladen ist
