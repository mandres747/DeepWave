# DeepWave – Ideen-Log

Herkunft: App-Radar-Session vom 2026-08-18
(Artifact: https://claude.ai/code/artifact/e7300171-4bb6-4b0a-9bb0-3bb8ea0aba78)

## 1. Schlaf-/Ambient-Mixer + Sleep-Timer (DeepWave-Ausbau) — UMGESETZT 2026-08-19

**Kern:** Kein neues Projekt, sondern der schnellste Win. Der 307-Punkte-Thread
des Jahres auf r/androidapps
(https://www.reddit.com/r/androidapps/comments/1pihsuc/) beschreibt exakt diese
Lücke: Calm/BetterSleep verlangen 60–80 €/Jahr für statische Audiodateien.
Sound-Mixer + Sleep-Timer als DeepWave-Erweiterung, Differenzierung über die
vorhandene Binaural-Engine.

- Nachfrage: **sehr hoch** (direkt belegt)
- Aufwand: **minimal** (Audio-Basis existiert)
- Risiko: viele neue FOSS-Konkurrenten

**Einordnung im App-Radar (Empfehlungen):**
> Nebenbei: DeepWave um Sound-Mixer + Sleep-Timer erweitern — geringster
> Aufwand, direkt belegte Riesen-Nachfrage, kein neues Projekt.

**Umsetzungsentscheidungen (2026-08-19):**
- Ambient-Sounds werden **prozedural synthetisiert** (wie die Binaural-Engine,
  keine Audio-Assets → APK bleibt klein, F-Droid-kompatibel).
- Feature-Flag `soundMixerEnabled`: Premium = an, FOSS = aus (konsistent mit
  WAV-Export/Statistik-Gating; FOSS-Freischaltung bleibt offene Entscheidung).
- Sleep-Timer läuft im `AudioPlaybackService` (überlebt Screen-off) und blendet
  Binaural- UND Ambient-Wiedergabe über ~15 s aus.
- Ambient-Mixer läuft unabhängig von der Binaural-Session und ist mit ihr
  kombinierbar.

## Weitere Radar-Ideen (Kontext, andere Projekte)

- **Strategisch #1:** Entscheidungsbuch-Player (Burggraben: Content-Nachschub
  aus dem BPF) → eigenes Projekt `eb-player`, Plan-Doc existiert.
- **Kommerziell #2:** Oberstufen-Kursmanager oder Medikamenten-Reminder als
  zweites Projekt evaluieren.
