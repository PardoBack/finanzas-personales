import os
from PIL import Image, ImageDraw, ImageFilter

def create_fintech_logo(size=512, is_round=False):
    # Canvas with supersampling for crisp anti-aliasing
    scale = 4
    canvas_size = size * scale
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    margin = int(canvas_size * 0.05) if is_round else int(canvas_size * 0.08)
    corner_radius = int(canvas_size / 2) if is_round else int(canvas_size * 0.24)

    # 1. Base Squircle / Circle with subtle gradient
    # Deep Obsidian #090D14 to #0E1626
    c1 = (14, 22, 38, 255) # Top-left dark navy
    c2 = (7, 10, 16, 255)  # Bottom-right obsidian
    
    # Draw background plate
    draw.rounded_rectangle(
        [margin, margin, canvas_size - margin, canvas_size - margin],
        radius=corner_radius,
        fill=c1
    )
    
    # Gradient overlay via vertical bands
    for y in range(margin, canvas_size - margin):
        factor = (y - margin) / float(canvas_size - 2 * margin)
        r = int(c1[0] * (1 - factor) + c2[0] * factor)
        g = int(c1[1] * (1 - factor) + c2[1] * factor)
        b = int(c1[2] * (1 - factor) + c2[2] * factor)
        draw.line(
            [(margin + int(corner_radius * 0.2), y), (canvas_size - margin - int(corner_radius * 0.2), y)],
            fill=(r, g, b, 255)
        )

    # Redraw crisp rounded rectangle to clamp edges
    mask = Image.new("L", (canvas_size, canvas_size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle(
        [margin, margin, canvas_size - margin, canvas_size - margin],
        radius=corner_radius,
        fill=255
    )

    # 2. Outer Border (Electric Cyan / Emerald 1.5px subtle metallic glow)
    draw.rounded_rectangle(
        [margin, margin, canvas_size - margin, canvas_size - margin],
        radius=corner_radius,
        outline=(56, 189, 248, 120), # Cyan 50% opacity
        width=int(12 * scale)
    )

    # Inner subtle glow ring
    inner_m = margin + int(10 * scale)
    draw.rounded_rectangle(
        [inner_m, inner_m, canvas_size - inner_m, canvas_size - inner_m],
        radius=max(1, corner_radius - int(10 * scale)),
        outline=(16, 185, 129, 60), # Emerald 25% opacity
        width=int(6 * scale)
    )

    # 3. Center Emblem: Modern Geometric Vault Shield + Monogram 'P'
    # Center coordinates
    cx = canvas_size // 2
    cy = canvas_size // 2

    # Draw Outer Diamond / Shield Facets
    # Diamond top: (cx, cy - 350), right: (cx + 320, cy), bot: (cx, cy + 360), left: (cx - 320, cy)
    d_scale = 1.05
    top_pt = (cx, int(cy - 360 * scale * 0.25 * d_scale))
    right_pt = (int(cx + 340 * scale * 0.25 * d_scale), int(cy - 20 * scale * 0.25))
    bot_pt = (cx, int(cy + 370 * scale * 0.25 * d_scale))
    left_pt = (int(cx - 340 * scale * 0.25 * d_scale), int(cy - 20 * scale * 0.25))

    # Shield polygon fill: Deep slate with cyan reflection
    draw.polygon([top_pt, right_pt, bot_pt, left_pt], fill=(18, 28, 48, 240), outline=(56, 189, 248, 220))
    # Shield outline
    shield_pts = [top_pt, right_pt, bot_pt, left_pt]
    for i in range(len(shield_pts)):
        p1 = shield_pts[i]
        p2 = shield_pts[(i + 1) % len(shield_pts)]
        draw.line([p1, p2], fill=(56, 189, 248, 240), width=int(14 * scale))

    # Right facet shadow / depth
    draw.polygon([top_pt, right_pt, bot_pt, (cx, cy)], fill=(12, 19, 32, 180))

    # 4. Stylized High-Tech Monogram 'P' (Pardo)
    # Vertical Spine: Thick rounded bar
    spine_x1 = int(cx - 130 * scale * 0.25)
    spine_x2 = int(cx - 50 * scale * 0.25)
    spine_y1 = int(cy - 220 * scale * 0.25)
    spine_y2 = int(cy + 240 * scale * 0.25)

    # Vertical bar with White / Cyan gradient
    draw.rounded_rectangle(
        [spine_x1, spine_y1, spine_x2, spine_y2],
        radius=int(18 * scale),
        fill=(248, 250, 252, 255) # Clean White #F8FAFC
    )

    # P Upper Loop (Emerald Green #10B981 to #34D399)
    loop_x1 = spine_x1
    loop_y1 = spine_y1
    loop_x2 = int(cx + 170 * scale * 0.25)
    loop_y2 = int(cy + 50 * scale * 0.25)
    loop_w = int(65 * scale * 0.25)

    # Outer arc
    draw.rounded_rectangle(
        [loop_x1, loop_y1, loop_x2, loop_y2],
        radius=int(45 * scale * 0.25),
        fill=(16, 185, 129, 255) # Emerald
    )
    # Inner hole
    draw.rounded_rectangle(
        [spine_x2, loop_y1 + loop_w, loop_x2 - loop_w, loop_y2 - loop_w],
        radius=int(22 * scale * 0.25),
        fill=(15, 23, 40, 255) # Dark hole
    )

    # 5. Golden Apex Spark Diamond (Financial Growth Indicator)
    spark_size = int(45 * scale * 0.25)
    spark_cx = cx
    spark_cy = top_pt[1] - int(15 * scale)
    spark_pts = [
        (spark_cx, spark_cy - spark_size),
        (spark_cx + spark_size, spark_cy),
        (spark_cx, spark_cy + spark_size),
        (spark_cx - spark_size, spark_cy)
    ]
    draw.polygon(spark_pts, fill=(245, 158, 11, 255), outline=(251, 191, 36, 255)) # Amber Gold

    # 6. Cyan Tech Node Glow at Loop Vertex
    node_cx = loop_x2 - int(10 * scale)
    node_cy = (loop_y1 + loop_y2) // 2
    node_r = int(22 * scale * 0.25)
    draw.ellipse(
        [node_cx - node_r, node_cy - node_r, node_cx + node_r, node_cy + node_r],
        fill=(0, 242, 254, 255),
        outline=(255, 255, 255, 255),
        width=int(4 * scale)
    )

    # Apply mask for round / squircle clamping
    img.putalpha(mask)

    # Downsample with Lanczos high quality filter
    final_img = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_img

def main():
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }

    res_dir = os.path.abspath("app/src/main/res")
    print(f"Target res directory: {res_dir}")

    # High-res master icon (512x512)
    master = create_fintech_logo(512, is_round=False)
    master_round = create_fintech_logo(512, is_round=True)

    assets_dir = os.path.abspath("assets")
    os.makedirs(assets_dir, exist_ok=True)
    master.save(os.path.join(assets_dir, "logo_pardofinanzas_512.png"), "PNG")
    master.resize((192, 192), Image.Resampling.LANCZOS).save(os.path.join(assets_dir, "icon-192.png"), "PNG")
    print("Saved high-res assets.")

    for name, sz in densities.items():
        folder = os.path.join(res_dir, f"mipmap-{name}")
        os.makedirs(folder, exist_ok=True)

        # Standard squircle
        icon_squircle = master.resize((sz, sz), Image.Resampling.LANCZOS)
        icon_squircle.save(os.path.join(folder, "ic_launcher.webp"), "WEBP", quality=95)

        # Round icon
        icon_round = master_round.resize((sz, sz), Image.Resampling.LANCZOS)
        icon_round.save(os.path.join(folder, "ic_launcher_round.webp"), "WEBP", quality=95)

        print(f"Generated {name} ({sz}x{sz}): ic_launcher.webp and ic_launcher_round.webp")

    print("Successfully generated all Android launcher icons!")

if __name__ == "__main__":
    main()
