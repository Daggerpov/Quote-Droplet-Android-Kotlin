#!/usr/bin/env python3
"""Builds the Play Store feature graphic and the framed phone screenshots.

    python3 tools/make_store_assets.py <iOS 1024px icon> <folder of raw emulator screenshots>
    python3 tools/make_store_assets.py - <folder>   # screenshots only, keep the feature graphic

Raw screenshots are 1080x2340 PNGs taken with `adb exec-out screencap -p` (status bar in demo
mode). Writes store/feature_graphic.png and store/screenshots/*.png (1080x1920).
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_icons import droplet_rgba  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "store")
FONTS = os.path.expanduser("~/Library/Android/sdk/platforms/android-29/data/fonts")
BOLD = os.path.join(FONTS, "Roboto-Bold.ttf")
MEDIUM = os.path.join(FONTS, "Roboto-Medium.ttf")
SERIF_BOLD = os.path.join(FONTS, "NotoSerif-Bold.ttf")

# (raw file, caption, background, caption color) in store order. Each screen was taken in a
# different palette; the background and caption use that palette's colors.
SCREENS = [
    ("s5_widget.png", "A fresh quote on\nyour home screen", "#1E1D2A", "#F4F2F8"),  # Ember
    ("s1_feed.png", "Scroll, save and\nshare quotes", "#3A2C24", "#F7EEDD"),  # Espresso
    ("s4_appearance.png", "Eight palettes,\nor your own colors", "#3A2C24", "#F7EEDD"),  # Espresso
    ("s2_search.png", "Search by quote\nor author", "#0C1A2E", "#F1E4CE"),  # Midnight
    ("s3_top.png", "The community's\nfavourite quotes", "#DCEEF8", "#0F2536"),  # Sky
    ("s6_community.png", "Submit quotes\nof your own", "#F6F0E6", "#3A2E27"),  # Linen
    ("s7_notification.png", "One quote a day, at\nthe time you choose", "#1E1D2A", "#F4F2F8"),  # Ember
]


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def vertical_gradient(size, top, bottom):
    w, h = size
    grad = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / max(1, h - 1)
        grad.putpixel((0, y), tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return grad.resize((w, h))


def shade(rgb, amount):
    """Mixes toward black (amount > 0) or white (amount < 0)."""
    target = (0, 0, 0) if amount > 0 else (255, 255, 255)
    a = abs(amount)
    return tuple(round(c + (t - c) * a) for c, t in zip(rgb, target))


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.width - 1, img.height - 1], radius=radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def framed_screenshot(raw_path, caption, bg_hex, text_hex, out_path):
    W, H = 1080, 1920
    bg = hex_rgb(bg_hex)
    light = sum(bg) / 3 > 140
    canvas = vertical_gradient((W, H), shade(bg, -0.08) if not light else bg, shade(bg, 0.18)).convert("RGBA")
    d = ImageDraw.Draw(canvas)

    font = ImageFont.truetype(SERIF_BOLD, 72)
    y = 90
    for line in caption.split("\n"):
        tw = d.textlength(line, font=font)
        d.text(((W - tw) / 2, y), line, font=font, fill=hex_rgb(text_hex))
        y += 92

    shot = Image.open(raw_path).convert("RGB")
    target_h = H - 330 - 40
    target_w = round(shot.width * target_h / shot.height)
    shot = rounded(shot.resize((target_w, target_h), Image.LANCZOS), 46)
    x, top = (W - target_w) // 2, 330

    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([x, top + 14, x + target_w, top + target_h + 14], radius=46,
                                             fill=(0, 0, 0, 110 if light else 160))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(22)))
    border = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    edge = (0, 0, 0, 60) if light else (255, 255, 255, 50)
    ImageDraw.Draw(border).rounded_rectangle([x - 3, top - 3, x + target_w + 2, top + target_h + 2], radius=49,
                                             outline=edge, width=3)
    canvas.alpha_composite(border)
    canvas.alpha_composite(shot, (x, top))
    canvas.convert("RGB").save(out_path, optimize=True)


def feature_graphic(icon_path, out_path):
    W, H = 1024, 500
    canvas = Image.new("RGBA", (W, H))
    top, bottom = hex_rgb("#0A2342"), hex_rgb("#0E5A8A")
    # Diagonal gradient
    for x in range(W):
        for_y = Image.new("RGBA", (1, H))
        for y in range(H):
            t = (x / W) * 0.6 + (y / H) * 0.4
            for_y.putpixel((0, y), tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,))
        canvas.paste(for_y, (x, 0))

    # Soft glow behind the droplet
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([70, 60, 420, 440], fill=(120, 210, 255, 70))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(50)))

    drop = droplet_rgba(icon_path)
    h = 330
    w = round(drop.width * h / drop.height)
    canvas.alpha_composite(drop.resize((w, h), Image.LANCZOS), (245 - w // 2, (H - h) // 2 + 6))

    d = ImageDraw.Draw(canvas)
    title_font = ImageFont.truetype(SERIF_BOLD, 76)
    tag_font = ImageFont.truetype(MEDIUM, 34)
    tx = 450
    d.text((tx, 150), "Quote Droplet", font=title_font, fill=(255, 255, 255))
    d.text((tx + 4, 268), "A daily quote on your", font=tag_font, fill=(170, 222, 255))
    d.text((tx + 4, 312), "home screen", font=tag_font, fill=(170, 222, 255))
    canvas.convert("RGB").save(out_path, optimize=True)


def main():
    icon, raw_dir = sys.argv[1], sys.argv[2]
    os.makedirs(os.path.join(STORE, "screenshots"), exist_ok=True)
    if icon != "-":
        feature_graphic(icon, os.path.join(STORE, "feature_graphic.png"))
    for i, (raw, caption, bg, fg) in enumerate(SCREENS, start=1):
        path = os.path.join(raw_dir, raw)
        if not os.path.exists(path):
            print("skipping missing", raw)
            continue
        name = os.path.splitext(raw)[0].split("_", 1)[1]
        framed_screenshot(path, caption, bg, fg, os.path.join(STORE, "screenshots", f"{i:02d}_{name}.png"))
    print("done")


if __name__ == "__main__":
    main()
