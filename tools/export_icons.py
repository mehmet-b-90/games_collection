"""Export the approved artwork without changing its design (requires Pillow)."""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "design/brand"
SOURCE = BRAND / "games-collection-master.png"
RES = ROOT / "android/app/src/main/res"
WEB = ROOT / "files/static/icons"


def export(image, path, size, artwork_ratio=1.0):
    path.parent.mkdir(parents=True, exist_ok=True)
    side = round(size * artwork_ratio)
    scaled = image.resize((side, side), Image.Resampling.LANCZOS)
    # Extend only the outer background pixels; preserve the complete artwork.
    canvas = Image.new("RGBA", (size, size), image.getpixel((0, 0)))
    offset = (size - side) // 2
    if offset:
        source_edges = ((0, 1), (0, side), (side - 1, side))
        target_edges = ((0, offset), (offset, offset + side), (offset + side, size))
        for x, (left, right) in enumerate(target_edges):
            for y, (top, bottom) in enumerate(target_edges):
                source_x = source_edges[x]
                source_y = source_edges[y]
                tile = scaled.crop((source_x[0], source_y[0], source_x[1], source_y[1]))
                canvas.paste(tile.resize((right - left, bottom - top)), (left, top))
    else:
        canvas.paste(scaled, (0, 0))
    canvas.save(path, optimize=True)


def main():
    image = Image.open(SOURCE).convert("RGBA")
    if image.width != image.height:
        raise ValueError("The approved master must be square.")
    export(image, BRAND / "play-store-icon-512.png", 512)
    export(image, BRAND / "github-icon-512.png", 512, 0.88)
    for size in (32, 192, 512):
        export(image, WEB / f"app-icon-{size}.png", size)
    export(image, WEB / "apple-touch-icon-180.png", 180)
    export(image, WEB / "app-icon-maskable-512.png", 512, 0.88)
    # 108dp at xxxhdpi; 66dp square contains the complete source image.
    # Its artwork also stays within the central 66dp safe circle.
    export(image, RES / "drawable-nodpi/ic_launcher_foreground.png", 432, 66 / 108)
    export(image, RES / "drawable-nodpi/brand_icon.png", 256)


if __name__ == "__main__":
    main()
