from PIL import Image, ImageDraw

def create_app_icon(size, filename):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Rounded Squircle Background
    radius = int(size * 0.22)
    # Background color: #090D16
    draw.rounded_rectangle([(0, 0), (size - 1, size - 1)], radius=radius, fill=(9, 13, 22, 255))

    # Border glow
    draw.rounded_rectangle([(2, 2), (size - 3, size - 3)], radius=radius - 2, outline=(59, 130, 246, 120), width=int(size * 0.02))

    # Inner Glow Circle
    cx, cy = size // 2, size // 2
    glow_r = int(size * 0.32)
    draw.ellipse([(cx - glow_r, cy - glow_r), (cx + glow_r, cy + glow_r)], fill=(37, 99, 235, 60))

    # Diamond / Gem Logo
    # Top point, right, bottom, left
    top = (cx, int(size * 0.26))
    right = (int(size * 0.74), int(size * 0.44))
    bottom = (cx, int(size * 0.74))
    left = (int(size * 0.26), int(size * 0.44))
    center = (cx, int(size * 0.50))

    # Outer Shield Lines
    shield_pts = [
        (cx, int(size * 0.18)),
        (int(size * 0.82), int(size * 0.32)),
        (int(size * 0.82), int(size * 0.62)),
        (cx, int(size * 0.84)),
        (int(size * 0.18), int(size * 0.62)),
        (int(size * 0.18), int(size * 0.32)),
    ]
    draw.polygon(shield_pts, outline=(59, 130, 246, 230), width=int(size * 0.035))

    # Diamond facets
    # Emerald Gradient Facets
    draw.polygon([top, right, center], fill=(16, 185, 129, 255))
    draw.polygon([top, left, center], fill=(52, 211, 153, 255))
    draw.polygon([left, bottom, center], fill=(5, 150, 105, 255))
    draw.polygon([right, bottom, center], fill=(4, 120, 87, 255))

    # Accent Sparkles
    sparkle_r = int(size * 0.025)
    draw.ellipse([(top[0] - sparkle_r, top[1] - sparkle_r), (top[0] + sparkle_r, top[1] + sparkle_r)], fill=(253, 224, 71, 255))
    draw.ellipse([(right[0] - sparkle_r, right[1] - sparkle_r), (right[0] + sparkle_r, right[1] + sparkle_r)], fill=(253, 224, 71, 255))
    draw.ellipse([(left[0] - sparkle_r, left[1] - sparkle_r), (left[0] + sparkle_r, left[1] + sparkle_r)], fill=(253, 224, 71, 255))

    img.save(filename, "PNG")
    print(f"Generated {filename} ({size}x{size})")

create_app_icon(192, "assets/icon-192.png")
create_app_icon(512, "assets/icon-512.png")
