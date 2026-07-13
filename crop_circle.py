from PIL import Image, ImageDraw

def make_circle(image_path):
    img = Image.open(image_path).convert("RGBA")
    
    # Make square
    size = min(img.size)
    left = (img.size[0] - size) // 2
    top = (img.size[1] - size) // 2
    right = (img.size[0] + size) // 2
    bottom = (img.size[1] + size) // 2
    img = img.crop((left, top, right, bottom))
    
    # Create mask
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    
    # Apply mask
    result = img.copy()
    result.putalpha(mask)
    
    result.save(image_path)

if __name__ == "__main__":
    make_circle(r"c:\Users\afaqa\OneDrive\Documents\Development\ShonenX-1\assets\images\app_icon.png")
