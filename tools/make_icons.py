#!/usr/bin/env python3
"""Builds the Android launcher icon layers, the widget picker preview and the 512px Play icon
from the iOS app icon (a blue droplet on white).

    python3 tools/make_icons.py <path to the iOS 1024px icon>

Writes into app/src/main/res and store/.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
STORE = os.path.join(ROOT, "store")


def droplet_rgba(src_path):
    """The droplet (with its soft shadow) on a transparent background, cropped to its bounds."""
    img = Image.open(src_path).convert("RGB")
    marked = img.copy()
    # Everything white-ish that touches the border is background (incl. the faint grey circle)
    for seed in [(0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)]:
        ImageDraw.floodfill(marked, seed, (255, 0, 255), thresh=40)
    alpha = Image.new("L", img.size, 255)
    mp, ap = marked.load(), alpha.load()
    for y in range(img.height):
        for x in range(img.width):
            if mp[x, y] == (255, 0, 255):
                ap[x, y] = 0
    rgba = img.convert("RGBA")
    rgba.putalpha(alpha)
    return rgba.crop(alpha.getbbox())


def place(drop, canvas_px, height_fraction, background=None):
    """Centers the droplet on a square canvas so it is `height_fraction` of the canvas tall."""
    h = int(canvas_px * height_fraction)
    w = int(drop.width * h / drop.height)
    scaled = drop.resize((w, h), Image.LANCZOS)
    canvas = Image.new("RGBA", (canvas_px, canvas_px), background or (0, 0, 0, 0))
    canvas.alpha_composite(scaled, ((canvas_px - w) // 2, (canvas_px - h) // 2))
    return canvas


def main():
    drop = droplet_rgba(sys.argv[1])

    # Adaptive icon foreground: 108dp canvas, droplet kept inside the 66dp safe zone
    for folder, px in [("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)]:
        out_dir = os.path.join(RES, f"mipmap-{folder}")
        os.makedirs(out_dir, exist_ok=True)
        place(drop, px * 4, 0.54).resize((px, px), Image.LANCZOS).save(os.path.join(out_dir, "ic_launcher_foreground.png"))

    # Play Store icon: 512x512, full square (Play rounds the corners itself)
    os.makedirs(STORE, exist_ok=True)
    place(drop, 2048, 0.66, (255, 255, 255, 255)).resize((512, 512), Image.LANCZOS).convert("RGB").save(
        os.path.join(STORE, "icon_512.png"))

    # Widget picker preview for launchers older than Android 12 (newer ones use the preview layout)
    w, h, r = 660, 440, 40
    preview = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(preview)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=r, fill=(0x50, 0x41, 0x36, 255))
    serif = "/System/Library/Fonts/Supplemental/Georgia.ttf"
    quote_font = ImageFont.truetype(serif, 40)
    author_font = ImageFont.truetype(serif, 34)
    lines = ["More is lost by indecision", "than by wrong decision."]
    y = 120
    for line in lines:
        tw = d.textlength(line, font=quote_font)
        d.text(((w - tw) / 2, y), line, font=quote_font, fill=(0xEE, 0xC5, 0x84, 255))
        y += 56
    author = "— Cicero"
    tw = d.textlength(author, font=author_font)
    d.text(((w - tw) / 2, y + 26), author, font=author_font, fill=(0xF8, 0xF3, 0xE1, 255))
    nodpi = os.path.join(RES, "drawable-nodpi")
    os.makedirs(nodpi, exist_ok=True)
    preview.save(os.path.join(nodpi, "widget_preview_image.png"))
    print("icons written")


if __name__ == "__main__":
    main()
