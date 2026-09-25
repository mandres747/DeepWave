"""Store icon (512 x 512) and feature graphic (1024 x 500) for the "Schwebung" look.

The icon is drawn from the same geometry as the adaptive launcher icon
(res/drawable/ic_launcher_foreground.xml, 108 dp viewport), so store and
launcher cannot drift apart. The feature graphic lays the Canva motif
(playstore/canva/feature_motif.png, wave band of the Canva mood design DAHWPXpO_y8, KI-Asset) under the
app name in the bundled Fraunces cut. Colours are pulled to the exact theme
values; see docs/GESTALTUNG.md.

Usage:
    python playstore/make_store_graphics.py
"""

import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "fraunces_soft_semibold.ttf")
MOTIF = os.path.join(HERE, "canva", "feature_motif.png")

PLUM = (0x1D, 0x0E, 0x1D)
ORCHID = (0xE0, 0x7B, 0xDE)
MOON = (0xF1, 0xE9, 0xF4)
MUTED = (0xBF, 0xA9, 0xC2)

# Launcher geometry (108 viewport): two mirrored cubic runs through y = 54.
WAVE_A = [((24, 54),
          (31.2, 39.33),
          (36.8, 39.33),
          (44, 54)),
          ((44, 54),
          (51.2, 79.33),
          (56.8, 79.33),
          (64, 54)),
          ((64, 54),
          (71.2, 39.33),
          (76.8, 39.33),
          (84, 54))]
WAVE_B = [((x0, 108 - y0), (x1, 108 - y1), (x2, 108 - y2), (x3, 108 - y3))
          for (x0, y0), (x1, y1), (x2, y2), (x3, y3) in WAVE_A]


def bezier(segments, scale, steps=240):
    pts = []
    for p0, p1, p2, p3 in segments:
        for i in range(steps + 1):
            t = i / steps
            u = 1 - t
            x = u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0]
            y = u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]
            pts.append((x * scale, y * scale))
    return pts


def stroke(draw, pts, width, fill):
    """Round brush: a disc at every sample, so joins and ends are round."""
    r = width / 2
    for x, y in pts:
        draw.ellipse((x - r, y - r, x + r, y + r), fill=fill)


def icon(size=512):
    ss = 4  # supersample for smooth curves
    s = size * ss
    scale = s / 108
    img = Image.new("RGB", (s, s), PLUM)
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    for r, a in ((36, 20), (26, 20)):
        c = 54 * scale
        g.ellipse((c - r * scale, c - r * scale, c + r * scale, c + r * scale), fill=ORCHID + (a,))
    img.paste(glow, (0, 0), glow)
    layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    stroke(d, bezier(WAVE_B, scale), int(3.5 * scale), MOON + (217,))
    stroke(d, bezier(WAVE_A, scale), int(5.5 * scale), ORCHID + (255,))
    img.paste(layer, (0, 0), layer)
    return img.resize((size, size), Image.LANCZOS)


def pull_to_palette(img):
    """Canva delivers a slightly shifted, noisy ground; clamp the darkest tones to PLUM."""
    px = img.convert("RGB").load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if r + g + b < 120:
                k = (r + g + b) / 120
                px[x, y] = tuple(int(PLUM[i] * (1 - k) + (r, g, b)[i] * k) for i in range(3))
    return img


def feature(title, line):
    w, h = 1024, 500
    bg = Image.new("RGB", (w, h), PLUM)
    if os.path.exists(MOTIF):
        # The wave band from the Canva mood (design DAHWPXpO_y8), faded in at
        # top and bottom so it floats on the plum ground.
        m = pull_to_palette(Image.open(MOTIF).convert("RGB"))
        m = m.resize((w, int(m.height * w / m.width)), Image.LANCZOS)
        fade = Image.new("L", m.size, 255)
        fd = ImageDraw.Draw(fade)
        edge = m.height // 4
        for y in range(edge):
            a = int(255 * y / edge)
            fd.line([(0, y), (m.width, y)], fill=a)
            fd.line([(0, m.height - 1 - y), (m.width, m.height - 1 - y)], fill=a)
        bg.paste(m, (0, (h - m.height) // 2), fade)
    # Darken the left third so the title always reads.
    shade = Image.new("L", (w, h), 0)
    sd = ImageDraw.Draw(shade)
    for x in range(w):
        a = int(max(0, 1 - x / 620) * 170)
        sd.line([(x, 0), (x, h)], fill=a)
    bg = Image.composite(Image.new("RGB", (w, h), PLUM), bg, shade)
    d = ImageDraw.Draw(bg)
    ic = icon(132)
    mask = Image.new("L", ic.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 131, 131), radius=30, fill=255)
    bg.paste(ic, (64, 110), mask)
    d.text((64, 262), title, font=ImageFont.truetype(FONT, 76), fill=MOON)
    d.text((66, 362), line, font=ImageFont.truetype(FONT, 30), fill=MUTED)
    return bg


if __name__ == "__main__":
    icon().save(os.path.join(HERE, "ic_launcher-playstore.png"))
    feature("DeepWave", "Binaurale Beats zum Einschlafen und Fokussieren").save(
        os.path.join(HERE, "feature_graphic.png"))
    feature("DeepWave", "Binaural beats for sleep and focus").save(
        os.path.join(HERE, "feature_graphic_en.png"))
    print("ok")
