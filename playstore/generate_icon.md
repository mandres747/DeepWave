# Play Store Icon generieren (512×512 PNG)

## Option A: Android Studio (empfohlen)

1. Android Studio öffnen → Projekt laden
2. Rechtsklick auf `app/src/main/res/` → New → Image Asset
3. "Icon Type" → **Google Play Store** wählen
4. Source Asset → `ic_launcher_foreground.xml` + `ic_launcher_background.xml`
5. Output: `ic_launcher-playstore.png` (512×512)
6. Datei nach `playstore/` kopieren

## Option B: SVG → PNG Konvertierung

Die Datei `playstore/ic_launcher-playstore.svg` enthält das Icon als SVG.
Mit Inkscape, GIMP, oder einem Online-Konverter in 512×512 PNG umwandeln:

```bash
# Inkscape (CLI):
inkscape ic_launcher-playstore.svg -w 512 -h 512 -o ic_launcher-playstore.png

# ImageMagick:
magick convert -background none -size 512x512 ic_launcher-playstore.svg ic_launcher-playstore.png
```

## Option C: Online
- https://www.svgtopng.com/ → SVG hochladen → 512×512 exportieren

## Ergebnis
- Dateiname: `playstore/ic_launcher-playstore.png`
- Format: PNG, 512×512px, 32-bit
- Max. Dateigröße: 1024 KB
