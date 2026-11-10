"""Generates build files, textures, models, recipes, loot and world generation for Deepvein."""
import json
import os
import random
import sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1]
MOD = "deepvein"
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data", MOD)
MCDATA = os.path.join(RES, "data", "minecraft")
random.seed(7)


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        if isinstance(content, str):
            f.write(content)
        else:
            json.dump(content, f, indent=2)
            f.write("\n")


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ------------------------------------------------------------------ build files

write(os.path.join(ROOT, "settings.gradle"), """pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
    }
}

rootProject.name = 'deepvein'
""")

write(os.path.join(ROOT, "build.gradle"), """plugins {
    id 'java'
    id 'net.minecraftforge.gradle' version '[6.0.16,6.2)'
}

version = '0.2.0'
group = 'dev.deepvein'

base {
    archivesName = 'deepvein-forge-1.20.1'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: '1.20.1'
    copyIdeResources = true

    runs {
        client {
            workingDirectory project.file('run')
            mods { deepvein { source sourceSets.main } }
        }
        server {
            workingDirectory project.file('run')
            args '--nogui'
            mods { deepvein { source sourceSets.main } }
        }
    }
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.3.0'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

jar {
    manifest {
        attributes([
            'Specification-Title'   : 'deepvein',
            'Specification-Version' : '1',
            'Implementation-Title'  : 'deepvein',
            'Implementation-Version': project.version
        ])
    }
}

jar.finalizedBy('reobfJar')
""")

write(os.path.join(ROOT, "gradle.properties"), "org.gradle.jvmargs=-Xmx3G\norg.gradle.daemon=false\n")
write(os.path.join(ROOT, ".gitignore"), "build/\n.gradle/\nrun/\n*.iml\n.idea/\n")

write(os.path.join(RES, "META-INF/mods.toml"), '''modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"

[[mods]]
modId="deepvein"
version="0.2.0"
displayName="Deepvein"
description="""
An action-RPG overhaul: mana and magic, accessories, life crystals, three ore tiers,
a spreading blight, two bosses and a town merchant.
"""

[[dependencies.deepvein]]
modId="forge"
mandatory=true
versionRange="[47,)"
ordering="NONE"
side="BOTH"

[[dependencies.deepvein]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.21)"
ordering="NONE"
side="BOTH"
''')

write(os.path.join(RES, "pack.mcmeta"),
      {"pack": {"description": "Deepvein resources", "pack_format": 15}})

# ------------------------------------------------------------------ colours

MATS = {
    "emberite": (226, 104, 40),
    "gloomsilver": (150, 160, 200),
    "aetherium": (90, 225, 215),
}
BROWN = (120, 82, 46)
GOLD = (236, 196, 70)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def finish(img):
    """Adds per-pixel noise, a top-left highlight and a dark outline to a flat drawing."""
    w, h = img.size
    src = img.load()
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            edge = False
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if nx < 0 or ny < 0 or nx >= w or ny >= h or src[nx, ny][3] == 0:
                    edge = True
            f = 0.55 if edge else 1.0 + (0.18 - 0.03 * (x + y) / 2) + random.uniform(-0.07, 0.07)
            dst[x, y] = shade((r, g, b), f) + (255,)
    return out


def canvas():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def icon(shape, col, col2=None):
    img, d = canvas()
    c2 = col2 or shade(col, 1.3)
    if shape == "ingot":
        d.polygon([(2, 9), (5, 5), (13, 5), (14, 8), (11, 12), (3, 12)], fill=col)
        d.line([(5, 6), (12, 6)], fill=shade(col, 1.35))
    elif shape == "raw":
        d.ellipse([3, 4, 12, 12], fill=col)
        d.ellipse([7, 2, 13, 8], fill=shade(col, 0.85))
        d.point([(5, 7), (9, 9), (10, 5), (6, 10)], fill=shade(col, 1.4))
    elif shape == "gem":
        d.polygon([(8, 1), (13, 6), (8, 14), (3, 6)], fill=col)
        d.line([(3, 6), (13, 6)], fill=shade(col, 1.4))
        d.line([(8, 1), (8, 14)], fill=shade(col, 0.8))
    elif shape == "star":
        d.polygon([(8, 1), (10, 6), (15, 6), (11, 9), (13, 14), (8, 11), (3, 14), (5, 9), (1, 6), (6, 6)],
                  fill=col)
        d.point([(8, 7), (7, 8), (8, 8)], fill=shade(col, 1.4))
    elif shape == "dust":
        for _ in range(26):
            x, y = random.randint(3, 12), random.randint(7, 13)
            if abs(x - 7.5) < (y - 5) * 0.9:
                d.point((x, y), fill=shade(col, random.uniform(0.8, 1.3)))
        d.polygon([(3, 13), (8, 8), (12, 13)], fill=col)
    elif shape == "sword":
        d.line([(5, 10), (13, 2)], fill=col, width=2)
        d.line([(13, 2), (14, 1)], fill=shade(col, 1.3))
        d.line([(3, 8), (8, 13)], fill=shade(col, 0.6), width=1)
        d.line([(2, 13), (4, 11)], fill=BROWN, width=2)
    elif shape == "pickaxe":
        d.line([(2, 14), (11, 5)], fill=BROWN, width=1)
        d.line([(4, 2), (10, 2), (13, 5), (13, 11)], fill=col, width=2)
    elif shape == "helmet":
        d.pieslice([2, 2, 13, 16], 180, 360, fill=col)
        d.rectangle([2, 8, 13, 11], fill=col)
        d.rectangle([5, 9, 10, 11], fill=(0, 0, 0, 0))
    elif shape == "chestplate":
        d.polygon([(1, 2), (5, 2), (6, 4), (9, 4), (10, 2), (14, 2), (14, 7), (12, 7), (12, 14), (3, 14),
                   (3, 7), (1, 7)], fill=col)
        d.line([(7, 6), (7, 12)], fill=shade(col, 0.75))
    elif shape == "leggings":
        d.rectangle([3, 2, 12, 5], fill=col)
        d.rectangle([3, 5, 6, 14], fill=col)
        d.rectangle([9, 5, 12, 14], fill=col)
    elif shape == "boots":
        d.rectangle([2, 4, 5, 12], fill=col)
        d.rectangle([2, 10, 7, 12], fill=col)
        d.rectangle([9, 4, 12, 12], fill=col)
        d.rectangle([9, 10, 14, 12], fill=col)
    elif shape == "staff":
        d.line([(2, 14), (10, 6)], fill=BROWN, width=1)
        d.ellipse([9, 1, 14, 6], fill=col)
        d.point([(11, 3)], fill=shade(col, 1.5))
    elif shape == "book":
        d.rectangle([3, 2, 12, 13], fill=c2)
        d.rectangle([3, 2, 4, 13], fill=shade(c2, 0.6))
        d.polygon([(8, 4), (10, 7), (8, 10), (6, 7)], fill=col)
    elif shape == "ring":
        d.ellipse([3, 5, 12, 14], outline=GOLD, width=2)
        d.polygon([(8, 1), (11, 4), (8, 7), (5, 4)], fill=col)
    elif shape == "orb":
        d.ellipse([2, 2, 13, 13], fill=col)
        d.ellipse([5, 5, 10, 10], fill=c2)
    elif shape == "heart":
        d.ellipse([2, 3, 8, 9], fill=col)
        d.ellipse([7, 3, 13, 9], fill=col)
        d.polygon([(2, 7), (13, 7), (8, 14), (7, 14)], fill=col)
        d.point([(4, 5), (5, 5)], fill=shade(col, 1.5))
    elif shape == "bell":
        d.polygon([(8, 2), (11, 5), (12, 11), (14, 12), (1, 12), (3, 11), (4, 5), (7, 2)], fill=col)
        d.rectangle([7, 13, 8, 14], fill=shade(col, 0.7))
    elif shape == "lantern":
        d.rectangle([6, 1, 9, 2], fill=shade(col, 0.6))
        d.rectangle([4, 3, 11, 13], fill=shade(col, 0.6))
        d.rectangle([6, 5, 9, 11], fill=c2)
    elif shape == "idol":
        d.rectangle([5, 2, 10, 7], fill=col)
        d.rectangle([4, 8, 11, 14], fill=col)
        d.point([(6, 4), (9, 4)], fill=c2)
        d.line([(6, 6), (9, 6)], fill=shade(col, 0.6))
    elif shape == "spore":
        d.ellipse([4, 6, 11, 13], fill=col)
        d.line([(8, 6), (8, 2)], fill=shade(col, 0.7))
        d.point([(6, 9), (9, 11), (9, 8)], fill=c2)
    elif shape == "left":
        d.polygon([(3, 8), (9, 2), (9, 6), (13, 6), (13, 10), (9, 10), (9, 14)], fill=col)
    elif shape == "right":
        d.polygon([(13, 8), (7, 2), (7, 6), (3, 6), (3, 10), (7, 10), (7, 14)], fill=col)
    elif shape == "down":
        d.polygon([(8, 13), (2, 7), (6, 7), (6, 2), (10, 2), (10, 7), (14, 7)], fill=col)
    elif shape == "bars":
        d.rectangle([2, 3, 13, 5], fill=col)
        d.rectangle([2, 7, 10, 9], fill=col)
        d.rectangle([2, 11, 7, 13], fill=col)
    elif shape == "paper":
        d.rectangle([4, 2, 11, 13], fill=col)
        d.line([(6, 5), (9, 5)], fill=shade(col, 0.6))
        d.line([(6, 8), (9, 8)], fill=shade(col, 0.6))
    elif shape == "blank":
        d.rectangle([0, 0, 15, 15], fill=col)
    elif shape == "mirror":
        d.ellipse([3, 1, 12, 10], fill=c2)
        d.ellipse([5, 3, 10, 8], fill=col)
        d.line([(7, 11), (7, 14)], fill=BROWN, width=2)
    elif shape == "remote":
        d.rectangle([4, 3, 11, 13], fill=col)
        d.rectangle([6, 5, 9, 7], fill=c2)
        d.point([(6, 10), (9, 10), (6, 12), (9, 12)], fill=shade(col, 0.5))
        d.line([(7, 1), (7, 2)], fill=shade(col, 0.6))
    elif shape == "lens":
        d.ellipse([2, 2, 10, 10], outline=GOLD, width=2)
        d.ellipse([4, 4, 8, 8], fill=col)
        d.line([(10, 10), (14, 14)], fill=BROWN, width=2)
    elif shape == "bolt":
        d.ellipse([4, 4, 11, 11], fill=col)
        d.ellipse([6, 6, 9, 9], fill=(255, 255, 255))
    return finish(img)


def noise_tile(base, spread=0.12, size=16):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(base, 1 + random.uniform(-spread, spread)) + (255,)
    return img


def ore_tile(col):
    img = noise_tile((112, 112, 116), 0.1)
    px = img.load()
    for _ in range(7):
        cx, cy = random.randint(1, 13), random.randint(1, 13)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1)):
            if random.random() < 0.8:
                px[min(15, cx + dx), min(15, cy + dy)] = shade(col, random.uniform(0.85, 1.25)) + (255,)
    return img


# ------------------------------------------------------------------ item list
# name -> (display name, shape, colour, secondary colour, model parent, description)

ITEMS = {}


def item(name, display, shape, col, col2=None, parent="item/generated", desc=None):
    ITEMS[name] = (display, shape, col, col2, parent, desc)


for mat, col in MATS.items():
    title = mat.capitalize()
    item("raw_" + mat, "Raw " + title, "raw", col)
    item(mat + "_ingot", title + " Ingot", "ingot", col)
    item(mat + "_sword", title + " Sword", "sword", col, parent="item/handheld")
    item(mat + "_pickaxe", title + " Pickaxe", "pickaxe", col, parent="item/handheld")
    item(mat + "_helmet", title + " Helmet", "helmet", col)
    item(mat + "_chestplate", title + " Chestplate", "chestplate", col)
    item(mat + "_leggings", title + " Leggings", "leggings", col)
    item(mat + "_boots", title + " Boots", "boots", col)

item("stardust", "Stardust", "dust", (250, 230, 120))
item("blight_spore", "Blight Spore", "spore", (120, 50, 150), (200, 120, 230))
item("warden_core", "Warden Core", "orb", (150, 40, 30), (255, 170, 60))
item("tyrant_heart", "Tyrant Heart", "heart", (110, 40, 170))
item("ember_bolt", "Ember Bolt", "bolt", (255, 120, 30))
item("frost_bolt", "Frost Bolt", "bolt", (120, 200, 255))
item("void_bolt", "Void Bolt", "bolt", (150, 60, 220))
item("ember_wand", "Ember Wand", "staff", (255, 120, 30), parent="item/handheld",
     desc="Right-click: hurls a burning bolt. 4 mana.")
item("frost_staff", "Frost Staff", "staff", (120, 200, 255), parent="item/handheld",
     desc="Right-click: fires a freezing bolt that slows. 6 mana.")
item("storm_tome", "Storm Tome", "book", (250, 240, 120), (70, 80, 150),
     desc="Right-click: calls lightning where you look. 16 mana.")
item("void_scepter", "Void Scepter", "staff", (150, 60, 220), parent="item/handheld",
     desc="Right-click: looses three withering bolts. 12 mana.")
item("life_crystal", "Life Crystal", "heart", (235, 60, 90),
     desc="Use: permanently adds one heart (up to 10).")
item("mana_star", "Mana Star", "star", (90, 140, 255),
     desc="Use: permanently adds 20 max mana (up to 200).")
item("swift_charm", "Swiftstride Charm", "ring", (120, 230, 120),
     desc="Accessory: move faster. Keep it in your hotbar or off hand.")
item("cloud_charm", "Cloudstep Charm", "ring", (220, 235, 255),
     desc="Accessory: jump higher. Keep it in your hotbar or off hand.")
item("aegis_band", "Aegis Band", "ring", (160, 170, 190),
     desc="Accessory: take less damage. Keep it in your hotbar or off hand.")
item("ember_charm", "Ember Charm", "ring", (255, 120, 30),
     desc="Accessory: immune to fire. Keep it in your hotbar or off hand.")
item("mana_locket", "Mana Locket", "ring", (90, 140, 255),
     desc="Accessory: mana regenerates twice as fast. Keep it in your hotbar or off hand.")
item("gloom_lantern", "Gloom Lantern", "lantern", (150, 40, 30), (255, 170, 60),
     desc="Use at night: summons the Gloomwarden.")
item("rift_idol", "Rift Idol", "idol", (110, 40, 170), (255, 120, 255),
     desc="Use: summons the Rift Tyrant.")
item("town_bell", "Town Bell", "bell", GOLD,
     desc="Use: calls a Tinkerer who trades for emeralds.")
item("purity_powder", "Purity Powder", "dust", (200, 255, 210),
     desc="Use on blight: cleanses a 5x5x5 area.")

item("lodestone_charm", "Lodestone Charm", "ring", (200, 60, 60),
     desc="Accessory: pulls nearby dropped items to you. Keep it in your hotbar or off hand.")
item("miners_lens", "Miner's Lens", "lens", (140, 230, 255),
     desc="Accessory: see in the dark. Keep it in your hotbar or off hand.")
item("vault_remote", "Vault Remote", "remote", (120, 130, 150), (90, 225, 215),
     desc="Sneak and use on a Vault Core to link. Use to open it from afar.")
item("recall_mirror", "Recall Mirror", "mirror", (170, 220, 255), (236, 196, 70),
     desc="Use: return to your bed or the world spawn.")
item("vault_button_prev", "Previous Page", "left", (240, 240, 240))
item("vault_button_next", "Next Page", "right", (240, 240, 240))
item("vault_button_sort", "Sort and Merge", "bars", (250, 210, 90))
item("vault_button_deposit", "Quick Stack From Backpack", "down", (120, 230, 120))
item("vault_button_page", "Page", "paper", (235, 230, 215))
item("vault_button_blank", " ", "blank", (58, 58, 64))


def vault_tile(col):
    img = noise_tile((72, 76, 92), 0.08)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(40, 42, 54, 255))
    d.rectangle([4, 4, 11, 11], outline=shade(col, 0.7) + (255,))
    d.rectangle([6, 6, 9, 9], fill=col + (255,))
    return img


BLOCKS = {
    "vault_core": ("Vault Core", vault_tile((90, 225, 215))),
    "vault_cell": ("Vault Cell", vault_tile((226, 104, 40))),
    "emberite_ore": ("Emberite Ore", ore_tile(MATS["emberite"])),
    "gloomsilver_ore": ("Gloomsilver Ore", ore_tile(MATS["gloomsilver"])),
    "aetherium_ore": ("Aetherium Ore", ore_tile(MATS["aetherium"])),
    "life_crystal_block": ("Life Crystal Cluster", ore_tile((240, 70, 110))),
    "blightgrass": ("Blightgrass", noise_tile((96, 48, 122), 0.2)),
    "blightstone": ("Blightstone", noise_tile((66, 52, 84), 0.14)),
}

lang = {
    "itemGroup.deepvein": "Deepvein",
    "entity.deepvein.gloomwarden": "The Gloomwarden",
    "entity.deepvein.rift_tyrant": "The Rift Tyrant",
    "entity.deepvein.tinkerer": "Tinkerer",
    "entity.deepvein.magic_bolt": "Magic Bolt",
    "message.deepvein.no_mana": "Not enough mana",
    "message.deepvein.maxed": "You cannot absorb any more of these",
    "message.deepvein.night_only": "It only answers at night",
    "message.deepvein.remote_linked": "Remote linked to this vault",
    "message.deepvein.remote_unlinked": "Sneak and use on a Vault Core to link this remote",
    "message.deepvein.remote_out_of_range": "The vault is out of range",
    "container.deepvein.vault": "Vault",
}

for name, (display, shape, col, col2, parent, desc) in ITEMS.items():
    save(icon(shape, col, col2), os.path.join(ASSETS, "textures/item", name + ".png"))
    write(os.path.join(ASSETS, "models/item", name + ".json"),
          {"parent": "minecraft:" + parent, "textures": {"layer0": f"{MOD}:item/{name}"}})
    lang[f"item.{MOD}.{name}"] = display
    if desc:
        lang[f"item.{MOD}.{name}.desc"] = desc

for name, (display, tile) in BLOCKS.items():
    save(tile, os.path.join(ASSETS, "textures/block", name + ".png"))
    write(os.path.join(ASSETS, "blockstates", name + ".json"),
          {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
    write(os.path.join(ASSETS, "models/block", name + ".json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{name}"}})
    write(os.path.join(ASSETS, "models/item", name + ".json"), {"parent": f"{MOD}:block/{name}"})
    lang[f"block.{MOD}.{name}"] = display

write(os.path.join(ASSETS, "lang/en_us.json"), lang)

# armour layers (64x32); the overlay "hat" area stays transparent
for mat, col in MATS.items():
    for layer in (1, 2):
        img = noise_tile(col, 0.1, 64).crop((0, 0, 64, 32))
        ImageDraw.Draw(img).rectangle([32, 0, 63, 15], fill=(0, 0, 0, 0))
        save(img, os.path.join(ASSETS, "textures/models/armor", f"{mat}_layer_{layer}.png"))


def boss_skin(base, eye):
    img = noise_tile(base, 0.14, 64)
    d = ImageDraw.Draw(img)
    d.rectangle([32, 0, 63, 15], fill=(0, 0, 0, 0))      # hat overlay
    d.rectangle([0, 32, 63, 47], fill=(0, 0, 0, 0))      # body overlays (unused by this model)
    d.rectangle([0, 48, 15, 63], fill=(0, 0, 0, 0))
    d.rectangle([48, 48, 63, 63], fill=(0, 0, 0, 0))
    d.rectangle([9, 11, 10, 12], fill=eye)               # face is at (8,8)-(15,15)
    d.rectangle([13, 11, 14, 12], fill=eye)
    d.line([(10, 14), (13, 14)], fill=shade(base, 0.4))
    d.rectangle([22, 22, 25, 27], fill=eye)              # chest mark
    return img


save(boss_skin((70, 28, 24), (255, 170, 60, 255)), os.path.join(ASSETS, "textures/entity/gloomwarden.png"))
save(boss_skin((48, 26, 78), (255, 120, 255, 255)), os.path.join(ASSETS, "textures/entity/rift_tyrant.png"))

# ------------------------------------------------------------------ loot tables


def loot(drop, fortune=False):
    entry = {"type": "minecraft:item", "name": f"{MOD}:{drop}"}
    if fortune:
        entry["functions"] = [
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
             "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"},
        ]
    return {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [entry],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


DROPS = {
    "emberite_ore": ("raw_emberite", True),
    "gloomsilver_ore": ("raw_gloomsilver", True),
    "aetherium_ore": ("raw_aetherium", True),
    "life_crystal_block": ("life_crystal", False),
    "blightgrass": ("blight_spore", False),
    "blightstone": ("blightstone", False),
    "vault_core": ("vault_core", False),
    "vault_cell": ("vault_cell", False),
}
for block, (drop, fortune) in DROPS.items():
    write(os.path.join(DATA, "loot_tables/blocks", block + ".json"), loot(drop, fortune))
for mob in ("gloomwarden", "rift_tyrant", "tinkerer"):
    write(os.path.join(DATA, "loot_tables/entities", mob + ".json"),
          {"type": "minecraft:entity", "pools": []})

# ------------------------------------------------------------------ tags


def tag(path, values):
    write(os.path.join(MCDATA, "tags/blocks", path + ".json"),
          {"replace": False, "values": [f"{MOD}:{v}" for v in values]})


tag("mineable/pickaxe", ["emberite_ore", "gloomsilver_ore", "aetherium_ore", "life_crystal_block",
                         "blightstone", "vault_core", "vault_cell"])
tag("mineable/shovel", ["blightgrass"])
tag("needs_iron_tool", ["emberite_ore", "life_crystal_block"])
tag("needs_diamond_tool", ["gloomsilver_ore", "aetherium_ore"])

# ------------------------------------------------------------------ recipes


def ing(x):
    return {"item": x if ":" in x else f"{MOD}:{x}"}


def shaped(name, pattern, key, count=1):
    write(os.path.join(DATA, "recipes", name + ".json"), {
        "type": "minecraft:crafting_shaped", "pattern": pattern,
        "key": {k: ing(v) for k, v in key.items()},
        "result": {"item": f"{MOD}:{name}", "count": count}})


def shapeless(name, ingredients, count=1):
    write(os.path.join(DATA, "recipes", name + ".json"), {
        "type": "minecraft:crafting_shapeless", "ingredients": [ing(i) for i in ingredients],
        "result": {"item": f"{MOD}:{name}", "count": count}})


for kind, time in (("smelting", 200), ("blasting", 100)):
    write(os.path.join(DATA, "recipes", f"emberite_ingot_from_{kind}.json"), {
        "type": f"minecraft:{kind}", "ingredient": ing("raw_emberite"),
        "result": f"{MOD}:emberite_ingot", "experience": 0.8, "cookingtime": time})

# tiers two and three need a boss drop to forge
shapeless("gloomsilver_ingot", ["raw_gloomsilver"] * 4 + ["warden_core"], 4)
shapeless("aetherium_ingot", ["raw_aetherium"] * 4 + ["tyrant_heart"], 4)

S = "minecraft:stick"
for mat in MATS:
    m = mat + "_ingot"
    shaped(mat + "_sword", ["M", "M", "S"], {"M": m, "S": S})
    shaped(mat + "_pickaxe", ["MMM", " S ", " S "], {"M": m, "S": S})
    shaped(mat + "_helmet", ["MMM", "M M"], {"M": m})
    shaped(mat + "_chestplate", ["M M", "MMM", "MMM"], {"M": m})
    shaped(mat + "_leggings", ["MMM", "M M", "M M"], {"M": m})
    shaped(mat + "_boots", ["M M", "M M"], {"M": m})

shaped("ember_wand", [" CE", " SC", "S  "], {"E": "emberite_ingot", "C": "minecraft:coal", "S": S})
shaped("frost_staff", [" IG", " SI", "S  "], {"G": "gloomsilver_ingot", "I": "minecraft:ice", "S": S})
shaped("storm_tome", ["DGD", "GBG", "DGD"],
       {"D": "stardust", "G": "gloomsilver_ingot", "B": "minecraft:book"})
shaped("void_scepter", [" PA", " SP", "S  "],
       {"A": "aetherium_ingot", "P": "minecraft:ender_pearl", "S": S})
shaped("mana_star", [" D ", "DAD", " D "], {"D": "stardust", "A": "minecraft:amethyst_shard"})
shaped("swift_charm", [" F ", "FEF", " S "],
       {"F": "minecraft:feather", "E": "emberite_ingot", "S": "minecraft:sugar"})
shaped("cloud_charm", [" F ", "FDF", " B "],
       {"F": "minecraft:feather", "D": "stardust", "B": "minecraft:slime_ball"})
shaped("aegis_band", [" I ", "IGI", " I "], {"I": "minecraft:iron_ingot", "G": "gloomsilver_ingot"})
shaped("ember_charm", [" E ", "EME", " E "], {"E": "emberite_ingot", "M": "minecraft:magma_cream"})
shaped("mana_locket", [" D ", "DGD", " D "], {"D": "stardust", "G": "minecraft:gold_ingot"})
shaped("gloom_lantern", ["EDE", "DLD", "EDE"],
       {"E": "emberite_ingot", "D": "stardust", "L": "minecraft:lantern"})
shaped("rift_idol", ["BGB", "GEG", "BGB"],
       {"B": "blight_spore", "G": "gloomsilver_ingot", "E": "minecraft:ender_eye"})
shaped("town_bell", ["GGG", "GEG", " S "], {"G": "minecraft:gold_ingot", "E": "emberite_ingot", "S": S})
shaped("vault_core", ["ECE", "CDC", "ECE"],
       {"E": "emberite_ingot", "C": "minecraft:chest", "D": "minecraft:diamond"})
shaped("vault_cell", [" E ", "ECE", " E "], {"E": "emberite_ingot", "C": "minecraft:chest"})
shaped("vault_remote", [" R ", "GPG", " G "],
       {"R": "minecraft:redstone", "G": "gloomsilver_ingot", "P": "minecraft:ender_pearl"})
shaped("recall_mirror", [" D ", "EGE", " E "],
       {"D": "stardust", "E": "emberite_ingot", "G": "minecraft:glass"})
shaped("lodestone_charm", [" R ", "IEI", " R "],
       {"R": "minecraft:redstone", "I": "minecraft:iron_ingot", "E": "emberite_ingot"})
shaped("miners_lens", [" G ", "GLG", " S "],
       {"G": "minecraft:gold_ingot", "L": "minecraft:glass", "S": "minecraft:glowstone_dust"})
shapeless("purity_powder", ["minecraft:bone_meal", "minecraft:glowstone_dust", "stardust"], 4)

# ------------------------------------------------------------------ world generation


def ore(name, block, size, count, lo, hi, rarity=None, targets=None):
    if targets is None:
        targets = [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
             "state": {"Name": f"{MOD}:{block}"}},
            {"target": {"predicate_type": "minecraft:tag_match",
                        "tag": "minecraft:deepslate_ore_replaceables"},
             "state": {"Name": f"{MOD}:{block}"}},
        ]
    write(os.path.join(DATA, "worldgen/configured_feature", name + ".json"), {
        "type": "minecraft:ore",
        "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets}})
    placement = []
    if rarity:
        placement.append({"type": "minecraft:rarity_filter", "chance": rarity})
    else:
        placement.append({"type": "minecraft:count", "count": count})
    placement += [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {
            "type": "minecraft:uniform",
            "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
        {"type": "minecraft:biome"},
    ]
    write(os.path.join(DATA, "worldgen/placed_feature", name + ".json"),
          {"feature": f"{MOD}:{name}", "placement": placement})


ore("ore_emberite", "emberite_ore", 8, 10, -20, 56)
ore("ore_gloomsilver", "gloomsilver_ore", 6, 5, -64, 0)
ore("ore_aetherium", "aetherium_ore", 4, 2, -64, -32)
ore("ore_life_crystal", "life_crystal_block", 3, 3, -48, 40)
ore("blight_patch", "blightgrass", 24, 1, 60, 110, rarity=40, targets=[
    {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:grass_block"},
     "state": {"Name": f"{MOD}:blightgrass"}}])

write(os.path.join(DATA, "forge/biome_modifier/add_features.json"), {
    "type": "forge:add_features",
    "biomes": "#minecraft:is_overworld",
    "features": [f"{MOD}:ore_emberite", f"{MOD}:ore_gloomsilver", f"{MOD}:ore_aetherium",
                 f"{MOD}:ore_life_crystal", f"{MOD}:blight_patch"],
    "step": "underground_ores"})

print("items", len(ITEMS), "blocks", len(BLOCKS))
