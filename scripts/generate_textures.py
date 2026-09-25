"""
Texture stylizer for SimplePipes.
PRESERVES EXACT PIXEL LAYOUT - only enhances colors with shading/contrast.
Reads the current texture (the correct original), improves colors,
writes back with identical opaque/transparent mask.
"""
from PIL import Image
import os

TEXTURES_DIR = r'C:\Users\Nahataen\Desktop\New folder - Copy\src\main\resources\assets\simplepipes\textures\block'
S = 16


def enhance_texture(name):
    """Enhance colors preserving exact pixel layout."""
    path = os.path.join(TEXTURES_DIR, name)
    img = Image.open(path).convert('RGBA')
    grid = [[img.getpixel((x,y)) for x in range(S)] for y in range(S)]
    new_img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    
    # Get bounding box of opaque pixels
    opaque = [(x,y) for y in range(S) for x in range(S) if grid[y][x][3] > 0]
    if not opaque:
        return
    
    min_x = min(x for x,y in opaque)
    max_x = max(x for x,y in opaque)
    min_y = min(y for x,y in opaque)
    max_y = max(y for x,y in opaque)
    
    # Get all colors from the original to preserve palette feel
    orig_colors = [grid[y][x][:3] for y,x in opaque]
    avg_r = sum(c[0] for c in orig_colors) // len(orig_colors)
    avg_g = sum(c[1] for c in orig_colors) // len(orig_colors)
    avg_b = sum(c[2] for c in orig_colors) // len(orig_colors)
    max_r = max(c[0] for c in orig_colors)
    max_g = max(c[1] for c in orig_colors)
    max_b = max(c[2] for c in orig_colors)
    min_r = min(c[0] for c in orig_colors)
    min_g = min(c[1] for c in orig_colors)
    min_b = min(c[2] for c in orig_colors)
    
    cx = (min_x + max_x) / 2.0
    cy = (min_y + max_y) / 2.0
    bw = max(1, (max_x - min_x) / 2.0)
    bh = max(1, (max_y - min_y) / 2.0)
    
    print(f"  {name}: {len(opaque)}px, avg=({avg_r},{avg_g},{avg_b}), range=[({min_r},{min_g},{min_b})->({max_r},{max_g},{max_b})]")
    
    for y in range(S):
        for x in range(S):
            orig = grid[y][x]
            r, g, b, a = orig
            if a == 0:
                new_img.putpixel((x, y), (0, 0, 0, 0))
                continue
            
            # Normalized position within bounding box (-1 to +1)
            dx = (x - cx) / bw
            dy = (y - cy) / bh
            dist = (dx*dx + dy*dy) ** 0.5
            
            # --- SHADING ---
            
            # 1) Light from top-left (brighten top-left, darken bottom-right)
            light_angle = (-dx - dy) / 2.0  # ranges ~ -1 to +1
            light_factor = 1.0 + 0.12 * light_angle  # ±12%
            
            # 2) Edge darkening (center brighter, edges darker)
            edge_factor = 1.0 - 0.08 * dist  # -8% at max dist
            
            # 3) Top highlight boost (y near min_y = top of shape)
            top_factor = 1.0
            if dy < -0.3:
                top_factor = 1.0 + 0.06 * (-dy - 0.3) / 0.7
            
            # 4) Bottom shadow
            bottom_factor = 1.0
            if dy > 0.3:
                bottom_factor = 1.0 - 0.08 * (dy - 0.3) / 0.7
            
            factor = light_factor * edge_factor * top_factor * bottom_factor
            factor = max(0.65, min(1.35, factor))
            
            # Apply enhancement
            new_r = int(r * factor)
            new_g = int(g * factor)
            new_b = int(b * factor)
            
            # Subtle saturation boost
            gray = (new_r + new_g + new_b) / 3.0
            sat_boost = 1.08
            new_r = int(gray + (new_r - gray) * sat_boost)
            new_g = int(gray + (new_g - gray) * sat_boost)
            new_b = int(gray + (new_b - gray) * sat_boost)
            
            # Clamp
            new_r = max(0, min(255, new_r))
            new_g = max(0, min(255, new_g))
            new_b = max(0, min(255, new_b))
            
            new_img.putpixel((x, y), (new_r, new_g, new_b, a))
    
    out_path = os.path.join(TEXTURES_DIR, name)
    new_img.save(out_path)
    print(f"    -> saved")


def main():
    print("=== SimplePipes Texture Stylizer (layout-preserving) ===\n")
    
    textures = [
        'tube_center.png',
        'tube_side.png',
        'extraction_tube_center.png',
        'extraction_tube_side.png',
        'filter_tube_center.png',
        'filter_tube_side.png',
    ]
    
    for tex in textures:
        enhance_texture(tex)
    
    print("\n✓ All textures enhanced!")


if __name__ == '__main__':
    main()
