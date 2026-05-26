# Google Play Store – Release Checklist

## Voraussetzungen

- [ ] Google Play Developer-Konto registriert (25 USD Einmalgebühr)
      → https://play.google.com/console/signup
- [x] Repo auf public geschaltet → https://github.com/mandres747/DeepWave

---

## Phase 1: Assets vorbereiten

### 1.1 Hi-Res App-Icon (512×512 PNG)
- [x] Icon generieren → DeepWave v9 (Headphones + Concentric Arcs)
- [x] Datei: `playstore/ic_launcher-playstore.png` (512×512)
- Play Store erwartet: 512×512px, PNG, 32-bit, max 1024 KB

### 1.2 Feature Graphic (1024×500 PNG)
- [x] Feature Graphic erstellt (DeepWave Icon + Waveforms + Feature Tags)
- [x] Datei: `playstore/feature_graphic.png` (1024×500)
- Wird prominent im Store angezeigt

### 1.3 Screenshots (mind. 2, empf. 6–8)
- [ ] Bereits vorhanden in `fastlane/metadata/android/*/images/phoneScreenshots/`
- [ ] Prüfen ob aktuell (Premium-Features sichtbar?)
- Formate: 16:9 oder 9:16, min. 320px, max. 3840px pro Seite

---

## Phase 2: Privacy Policy hosten

### Option A: GitHub Pages (empfohlen)
```bash
# Im Repo:
git checkout -b gh-pages
cp playstore/privacy_policy_en.html index.html
cp playstore/privacy_policy_de.html datenschutz.html
git add . && git commit -m "Add privacy policy pages"
git push -u origin gh-pages
```
→ URL wird: `https://mandres747.github.io/BinauralBeatsApp/`
→ Deutsch: `https://mandres747.github.io/BinauralBeatsApp/datenschutz.html`

### Option B: GitHub Gist
- Gist erstellen mit HTML-Inhalt
- URL in Play Console eintragen

### Option C: Eigene Website
- HTML-Dateien auf eigenen Server hochladen

**Gewählte URLs:**
- EN: https://mandres747.github.io/DeepWave/
- DE: https://mandres747.github.io/DeepWave/datenschutz.html

---

## Phase 3: Release Build signieren

### 3.1 Keystore erstellen (einmalig!)
- [x] Keystore erstellt: `playstore/deepwave-release.jks`
- [x] Alias: `deepwave`
- [x] `.gitignore` enthält `*.jks`

**WICHTIG:**
- Keystore SICHER aufbewahren (NICHT im Git-Repo!)
- Passwort separat sichern
- Verlust = App kann nie mehr aktualisiert werden

### 3.2 Signing Config in build.gradle.kts
- [x] `signingConfigs` in `app/build.gradle.kts` konfiguriert
- [x] Env-Variablen: `KEYSTORE_PASSWORD`, `KEY_PASSWORD`

### 3.3 AAB (Android App Bundle) bauen
- [x] Premium Release AAB erfolgreich gebaut
- Output: `app/build/outputs/bundle/premiumRelease/app-premium-release.aab`

Build-Kommandos:
```powershell
$env:KEYSTORE_PASSWORD = "<passwort>"
$env:KEY_PASSWORD = "<passwort>"
.\gradlew.bat bundlePremiumRelease
```

### 3.4 Release-Tag erstellen
```bash
git tag -a v1.0.0 -m "Release 1.0.0 – Initial Play Store release"
git push origin v1.0.0
```

---

## Phase 4: Play Console einrichten

### 4.1 App erstellen
- [ ] Play Console → "Create app"
- [ ] App name: **DeepWave: Binaural Beats**
- [ ] Default language: **Deutsch (de-DE)**
- [ ] App or Game: **App**
- [ ] Free or Paid: **Paid** (oder Free mit IAP – siehe Monetarisierungsstrategie)

### 4.2 Store Listing ausfüllen
- [ ] **Title**: "DeepWave: Binaural Beats" (24 Zeichen)
- [ ] **Short description**: Aus `fastlane/metadata/android/de-DE/short_description.txt`
- [ ] **Full description**: Aus `fastlane/metadata/android/de-DE/full_description.txt`
- [ ] **Screenshots**: Aus `fastlane/metadata/android/de-DE/images/phoneScreenshots/`
- [ ] **Hi-res icon**: `playstore/ic_launcher-playstore.png` (512×512)
- [ ] **Feature graphic**: `playstore/feature_graphic.png` (1024×500)
- [ ] Dasselbe für EN-Lokalisierung

### 4.3 Content Rating
- [ ] Fragebogen ausfüllen (Antworten: `playstore/CONTENT_RATING.md`)
- [ ] Erwartetes Rating: **PEGI 3 / USK 0 / Everyone**

### 4.4 Data Safety
- [ ] Formular ausfüllen (Antworten: `playstore/DATA_SAFETY.md`)
- [ ] "No data collected" + "No data shared"

### 4.5 Privacy Policy
- [ ] URL eintragen (aus Phase 2)

### 4.6 App-Kategorisierung
- [ ] **Category**: Health & Fitness
- [ ] **Tags**: Meditation, Binaural Beats, Sleep, Focus, Relaxation

### 4.7 Kontaktdaten
- [ ] E-Mail: man7477@gmail.com
- [ ] Website: https://github.com/mandres747/BinauralBeatsApp (optional)

---

## Phase 5: Testing (PFLICHT für neue Developer-Konten)

### Closed Testing (14 Tage + 12 Tester)
Seit November 2023 verlangt Google für neue Developer-Konten:
1. **Closed Testing Track** erstellen
2. **Mindestens 12 Tester** einladen (Google-Accounts)
3. **14 Tage** Testphase durchlaufen
4. Erst danach Freischaltung für Production möglich

- [ ] Closed Testing Track "Internal" oder "Alpha" erstellen
- [ ] AAB hochladen
- [ ] 12+ Tester-E-Mail-Adressen sammeln und einladen
- [ ] 14 Tage warten
- [ ] Tester-Feedback einarbeiten

### Pre-Launch Report
- [ ] Play Console → "Pre-launch report" prüfen
- [ ] Automatische Tests auf Abstürze, Accessibility, Security checken

---

## Phase 6: Production Release

- [ ] Release Review abwarten (typisch 1–7 Tage, manchmal länger)
- [ ] Bei Ablehnung: Feedback lesen, fixen, erneut einreichen

---

## Monetarisierungsstrategie

### Option A: Einmaliger Kaufpreis
- Preis: 2,99–4,99 €
- Einfachste Variante, kein IAP-Code nötig
- Google-Provision: 15% (< 1M USD/Jahr)

### Option B: Freemium + IAP Premium-Unlock
- Kostenloser Download, Premium-Upgrade als In-App-Kauf
- Erfordert Google Play Billing Library Integration
- Höhere Reichweite, komplexerer Code

**Empfehlung für v1.0:** Option A (Kaufpreis), da kein zusätzlicher Code nötig
und die App bereits eine vollständige Premium-Erfahrung bietet.

---

## Dateien-Übersicht

```
playstore/
├── privacy_policy_de.html     ← Datenschutzerklärung (DE)
├── privacy_policy_en.html     ← Privacy Policy (EN)
├── DATA_SAFETY.md             ← Data Safety Formular-Antworten
├── CONTENT_RATING.md          ← IARC-Fragebogen-Antworten
├── RELEASE_CHECKLIST.md       ← Diese Datei
├── ic_launcher-playstore.png  ← 512×512 Hi-Res Icon (TODO)
├── feature_graphic.png        ← 1024×500 Feature Graphic (TODO)
└── binaural-release.jks       ← Keystore (TODO, NICHT committen!)

fastlane/metadata/android/
├── de-DE/
│   ├── title.txt
│   ├── short_description.txt
│   ├── full_description.txt
│   ├── changelogs/1.txt       ← Release Notes v1.0
│   └── images/phoneScreenshots/01-06.png
└── en-US/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    ├── changelogs/1.txt       ← Release Notes v1.0
    └── images/phoneScreenshots/01-06.png
```
