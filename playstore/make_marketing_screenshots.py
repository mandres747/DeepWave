"""Turn raw device screenshots into Play Store marketing screenshots.

The closed-test feedback (2026-08-29, "App Screenshots") criticised the old
listing for showing plain device captures with no indication of what the app
does. This script wraps each capture in the DeepWave gradient, puts a headline
and a one-line benefit above it, and writes a consistent 1080x1920 set.

Usage:
    python playstore/make_marketing_screenshots.py de-DE
    python playstore/make_marketing_screenshots.py en-US
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(HERE, "screenshots", "raw")
OUT_ROOT = os.path.join(HERE, "screenshots")

CANVAS = (1080, 1920)

# "Schwebung" palette, from ui/theme/Theme.kt (surfaceDark -> primaryDark -> primaryMid).
GRADIENT = [(0x1D, 0x0E, 0x1D), (0x2F, 0x18, 0x2F), (0x3A, 0x1D, 0x3A)]
ACCENT = (0xE0, 0x7B, 0xDE)
WHITE = (0xF1, 0xE9, 0xF4)
MUTED = (0xBF, 0xA9, 0xC2)

# Headlines in the app's own title face (bundled Fraunces cut).
FONT_BOLD = os.path.join(os.path.dirname(HERE), "app", "src", "main", "res", "font", "fraunces_soft_semibold.ttf")
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

# (raw capture basename, headline, benefit line)
CAPTIONS = {
    "de-DE": [
        ("01-main", "Sessions, die dich\nrunterfahren", "Frequenzkurve, Wellenform und Phasen auf einen Blick"),
        ("02-presets", "Mehr als 30 Presets", "Schlaf, Fokus, Meditation, Kreativität, Sport und mehr"),
        ("03-editor", "Bau dir deine Session", "Jede Phase mit Frequenz, Dauer, Modulation und Klangart"),
        ("04-mixer", "Regen, Meer, Lagerfeuer", "Premium: Ambient-Mixer und Sleep-Timer, der sanft ausblendet"),
        ("05-breathing", "Atemführung, die mitläuft", "Box, 4-7-8, beruhigend oder energetisch"),
        ("06-journal", "Dein Session-Journal", "Bewerte jede Session und finde, was dir guttut"),
        ("07-onboarding", "In 30 Sekunden startklar", "Kurzer Rundgang beim ersten Start – jederzeit überspringbar"),
        ("08-stats", "Statistik über Wochen", "Premium: Sessions, Zeit, Serien und deine Top-Presets"),
    ],
    "en-US": [
        ("01-main", "Sessions that\nwind you down", "Frequency curve, waveform and phases at a glance"),
        ("02-presets", "More than 30 presets", "Sleep, focus, meditation, creativity, sports and more"),
        ("03-editor", "Build your own session", "Every phase with frequency, length, modulation and sound type"),
        ("04-mixer", "Rain, ocean, campfire", "Premium: ambient mixer and a sleep timer that fades out gently"),
        ("05-breathing", "A breathing guide\nthat keeps pace", "Box, 4-7-8, calming or energizing"),
        ("06-journal", "Your session journal", "Rate every session and find what actually works"),
        ("07-onboarding", "Ready in 30 seconds", "A short walkthrough on first launch – skippable any time"),
        ("08-stats", "Statistics over weeks", "Premium: sessions, time, streaks and your top presets"),
    ],
}


def gradient_background(size):
    """Vertical 3-stop gradient, drawn one row at a time."""
    width, height = size
    base = Image.new("RGB", (1, height))
    pixels = base.load()
    segments = len(GRADIENT) - 1
    for y in range(height):
        pos = y / (height - 1) * segments
        idx = min(int(pos), segments - 1)
        t = pos - idx
        start, end = GRADIENT[idx], GRADIENT[idx + 1]
        pixels[0, y] = tuple(int(start[c] + (end[c] - start[c]) * t) for c in range(3))
    return base.resize(size, Image.BILINEAR)


def rounded(image, radius):
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([(0, 0), image.size], radius=radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def draw_centered(draw, text, font, y, fill, line_spacing=12):
    """Draws (possibly multi-line) text centred on the canvas, returns bottom y."""
    for line in text.split("\n"):
        box = draw.textbbox((0, 0), line, font=font)
        width = box[2] - box[0]
        draw.text(((CANVAS[0] - width) // 2, y), line, font=font, fill=fill)
        y += (box[3] - box[1]) + line_spacing
    return y


def compose(raw_path, headline, benefit, out_path):
    canvas = gradient_background(CANVAS).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    headline_font = ImageFont.truetype(FONT_BOLD, 62)
    benefit_font = ImageFont.truetype(FONT_REGULAR, 32)

    y = draw_centered(draw, headline, headline_font, 96, WHITE, line_spacing=26)
    y = draw_centered(draw, benefit, benefit_font, y + 30, MUTED)

    # Accent rule between the copy and the device shot.
    rule_y = y + 34
    draw.rounded_rectangle(
        [(CANVAS[0] // 2 - 46, rule_y), (CANVAS[0] // 2 + 46, rule_y + 6)],
        radius=3,
        fill=ACCENT,
    )

    shot = Image.open(raw_path).convert("RGB")
    target_width = 840
    scale = target_width / shot.width
    shot = shot.resize((target_width, int(shot.height * scale)), Image.LANCZOS)

    top = rule_y + 70
    available = CANVAS[1] - top - 60
    if shot.height > available:
        shot = shot.crop((0, 0, shot.width, available))

    framed = rounded(shot, 36)

    # Soft drop shadow so the device shot separates from the gradient.
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow_box = Image.new("RGBA", framed.size, (0, 0, 0, 130))
    shadow_box.putalpha(framed.getchannel("A").point(lambda a: int(a * 0.5)))
    shadow.paste(shadow_box, ((CANVAS[0] - framed.width) // 2, top + 14), shadow_box)
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(18)))

    canvas.paste(framed, ((CANVAS[0] - framed.width) // 2, top), framed)

    # Hairline border on the device shot.
    border = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle(
        [
            ((CANVAS[0] - framed.width) // 2, top),
            ((CANVAS[0] + framed.width) // 2, top + framed.height),
        ],
        radius=36,
        outline=(0xE0, 0x7B, 0xDE, 60),
        width=2,
    )
    canvas = Image.alpha_composite(canvas, border)

    canvas.convert("RGB").save(out_path, "PNG", optimize=True)
    print("wrote", out_path)


def main():
    locale = sys.argv[1] if len(sys.argv) > 1 else "de-DE"
    if locale not in CAPTIONS:
        raise SystemExit("unknown locale: %s" % locale)

    raw_dir = os.path.join(RAW_DIR, locale)
    out_dir = os.path.join(OUT_ROOT, locale)
    os.makedirs(out_dir, exist_ok=True)

    made = 0
    for index, (name, headline, benefit) in enumerate(CAPTIONS[locale], start=1):
        raw_path = os.path.join(raw_dir, name + ".png")
        if not os.path.exists(raw_path):
            print("skip (no capture):", raw_path)
            continue
        compose(raw_path, headline, benefit, os.path.join(out_dir, "%02d.png" % index))
        made += 1
    print("done:", made, "screenshots for", locale)


if __name__ == "__main__":
    main()
