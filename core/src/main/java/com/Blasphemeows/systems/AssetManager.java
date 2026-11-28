package com.Blasphemeows.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private static AssetManager instance;

    private final Map<String, TextureRegion> textureRegionMap;
    private final Map<String, Animation<TextureRegion>> animationMap;
    private final Map<String, TextureRegion[]> frameMap;
    private final Map<String, Texture> textureMap;

    private AssetManager() {
        textureRegionMap = new HashMap<>();
        animationMap = new HashMap<>();
        frameMap = new HashMap<>();
        textureMap = new HashMap<>();
    }

    public static AssetManager getInstance() {
        if (instance == null) instance = new AssetManager();
        return instance;
    }

    // ---------- Texture (Auto hapus background putih PNG & background hitam JPG) ----------

    public Texture loadTexture(String filename) {
        if (textureMap.containsKey(filename)) return textureMap.get(filename);
        if (Gdx.files != null && Gdx.files.internal(filename).exists()) {
            Pixmap raw = new Pixmap(Gdx.files.internal(filename));
            Pixmap clean = new Pixmap(raw.getWidth(), raw.getHeight(), Pixmap.Format.RGBA8888);

            boolean isBlackBg = filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg");

            for (int y = 0; y < raw.getHeight(); y++) {
                for (int x = 0; x < raw.getWidth(); x++) {
                    int pixel = raw.getPixel(x, y);
                    int r = (pixel >>> 24) & 0xff;
                    int g = (pixel >>> 16) & 0xff;
                    int b = (pixel >>> 8) & 0xff;

                    if (isBlackBg) {
                        if (r < 24 && g < 24 && b < 24) {
                            clean.drawPixel(x, y, 0x00000000);
                        } else {
                            clean.drawPixel(x, y, pixel);
                        }
                    } else {
                        if (r > 240 && g > 240 && b > 240) {
                            clean.drawPixel(x, y, 0x00000000);
                        } else {
                            clean.drawPixel(x, y, pixel);
                        }
                    }
                }
            }
            raw.dispose();

            Texture texture = new Texture(clean);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            clean.dispose();
            textureMap.put(filename, texture);
            return texture;
        }
        return null;
    }

    private Texture loadFirstExisting(String... candidates) {
        for (String path : candidates) {
            Texture tex = loadTexture(path);
            if (tex != null) return tex;
        }
        return null;
    }

    // ---------- Region & Animation Helpers ----------

    public void registerRegion(String key, TextureRegion region) {
        if (region != null) {
            textureRegionMap.put(key, region);
        }
    }

    public void registerRegionFromSheet(String key, String filename, int tileWidth, int tileHeight, int row, int col) {
        Texture tex = loadTexture(filename);
        if (tex != null) {
            TextureRegion[][] grid = TextureRegion.split(tex, tileWidth, tileHeight);
            if (row < grid.length && col < grid[row].length) {
                textureRegionMap.put(key, grid[row][col]);
            }
        }
    }

    public void registerAnimationFromSheet(String key, String filename, int tileWidth, int tileHeight,
                                           int row, int startCol, int frameCount, float frameDuration,
                                           Animation.PlayMode playMode) {
        Texture tex = loadTexture(filename);
        if (tex != null) {
            TextureRegion[][] grid = TextureRegion.split(tex, tileWidth, tileHeight);
            if (row < grid.length) {
                TextureRegion[] frames = new TextureRegion[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    frames[i] = grid[row][startCol + i];
                }
                Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
                anim.setPlayMode(playMode);
                animationMap.put(key, anim);
                frameMap.put(key, frames);
                textureRegionMap.put(key, frames[0]);
            }
        }
    }

    public void registerMultiRowAnimation(String key, String filename, int rows, int cols,
                                          int startRow, int endRow, float frameDuration, Animation.PlayMode playMode) {
        Texture tex = loadTexture(filename);
        if (tex != null) {
            int tileWidth = tex.getWidth() / cols;
            int tileHeight = tex.getHeight() / rows;
            TextureRegion[][] grid = TextureRegion.split(tex, tileWidth, tileHeight);

            int totalFrames = (endRow - startRow + 1) * cols;
            TextureRegion[] frames = new TextureRegion[totalFrames];
            int index = 0;
            for (int r = startRow; r <= endRow; r++) {
                for (int c = 0; c < cols; c++) {
                    frames[index++] = grid[r][c];
                }
            }
            Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
            anim.setPlayMode(playMode);
            animationMap.put(key, anim);
            frameMap.put(key, frames);
            textureRegionMap.put(key, frames[0]);
        }
    }

    private TextureRegion[] extract5FrameLompatSheet(Texture tex) {
        int w = tex.getWidth();
        int h = tex.getHeight();

        int row1Y = 0;
        int row1H = (int) (h * 0.46f);
        int topColW = w / 3;

        int row2Y = (int) (h * 0.50f);
        int row2H = (int) (h * 0.48f);

        return new TextureRegion[] {
            new TextureRegion(tex, 0,                row1Y, topColW,           row1H),
            new TextureRegion(tex, topColW,          row1Y, topColW,           row1H),
            new TextureRegion(tex, topColW * 2,      row1Y, w - (topColW * 2), row1H),
            new TextureRegion(tex, (int)(w * 0.04f), row2Y, (int)(w * 0.46f),  row2H),
            new TextureRegion(tex, (int)(w * 0.50f), row2Y, (int)(w * 0.48f),  row2H)
        };
    }

    public void registerLompatAnimations() {
        Texture texMauLompat = loadFirstExisting("Si Meong/Meong_MauLompat.png");
        Texture texLompat = loadFirstExisting("Si Meong/Meong_Lompat.png");

        if (texMauLompat != null) {
            TextureRegion[] allFrames = extract5FrameLompatSheet(texMauLompat);
            TextureRegion[] activeFrames = new TextureRegion[] {
                allFrames[1], allFrames[2], allFrames[3], allFrames[4]
            };
            Animation<TextureRegion> anim = new Animation<>(0.055f, activeFrames);
            anim.setPlayMode(Animation.PlayMode.NORMAL);
            animationMap.put("Meong_MauLompat", anim);
            frameMap.put("Meong_MauLompat", activeFrames);
        } else {
            Gdx.app.error("AssetManager", "File Meong_MauLompat tidak ditemukan di folder assets/Si Meong/!");
        }

        if (texLompat != null) {
            TextureRegion[] frames = extract5FrameLompatSheet(texLompat);
            Animation<TextureRegion> anim = new Animation<>(0.08f, frames);
            anim.setPlayMode(Animation.PlayMode.NORMAL);
            animationMap.put("Meong_Lompat", anim);
            frameMap.put("Meong_Lompat", frames);
        } else {
            Gdx.app.error("AssetManager", "File Meong_Lompat tidak ditemukan di folder assets/Si Meong/!");
        }
    }

    public void registerGridRowAnimation(String key, String filename, int totalRows, int totalCols,
                                         int targetRow, int frameCount, float frameDuration,
                                         Animation.PlayMode playMode) {
        Texture tex = loadTexture(filename);
        if (tex != null) {
            int tileWidth = tex.getWidth() / totalCols;
            int tileHeight = tex.getHeight() / totalRows;
            TextureRegion[][] grid = TextureRegion.split(tex, tileWidth, tileHeight);

            if (targetRow < grid.length) {
                TextureRegion[] frames = new TextureRegion[frameCount];
                for (int c = 0; c < frameCount; c++) {
                    frames[c] = grid[targetRow][c];
                }
                Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
                anim.setPlayMode(playMode);
                animationMap.put(key, anim);
                frameMap.put(key, frames);
                textureRegionMap.put(key, frames[0]);
            }
        }
    }

    public TextureRegion getTextureRegion(String key) { return textureRegionMap.get(key); }
    public TextureRegion getRegion(String key) { return getTextureRegion(key); }
    public Animation<TextureRegion> getAnimation(String key) { return animationMap.get(key); }
    public TextureRegion[] getFrames(String key) { return frameMap.get(key); }

    // ---------- Init semua aset ----------

    public void init() {
        // ---- PLAYER (Si Meong Custom Sheets) ----
        registerMultiRowAnimation("Meong_Idle",  "Si Meong/Meong_Idle.png",   2, 6, 0, 0, 0.12f, Animation.PlayMode.LOOP);
        registerMultiRowAnimation("Meong_Run",   "Si Meong/Meong_Run.png",    2, 6, 0, 0, 0.09f, Animation.PlayMode.LOOP);
        registerMultiRowAnimation("Meong_Dash",  "Si Meong/Meong_Dash.png",   1, 4, 0, 0, 0.055f, Animation.PlayMode.NORMAL);
        registerMultiRowAnimation("Meong_Light", "Si Meong/Meong_Attack.png", 5, 5, 0, 1, 0.04f, Animation.PlayMode.NORMAL);
        registerMultiRowAnimation("Meong_Heavy", "Si Meong/Meong_Attack.png", 5, 5, 2, 3, 0.06f, Animation.PlayMode.NORMAL);

        // ---- COMBO LLL (Grid 3x6) ----
        registerGridRowAnimation("Meong_LLL_1", "Si Meong/Meong_Combo_LLL.png", 3, 6, 0, 4, 0.045f, Animation.PlayMode.NORMAL);
        registerGridRowAnimation("Meong_LLL_2", "Si Meong/Meong_Combo_LLL.png", 3, 6, 1, 4, 0.045f, Animation.PlayMode.NORMAL);
        registerGridRowAnimation("Meong_LLL_3", "Si Meong/Meong_Combo_LLL.png", 3, 6, 2, 5, 0.045f, Animation.PlayMode.NORMAL);

        // ---- COMBO HH (Grid 2x6) ----
        registerGridRowAnimation("Meong_HH_1",  "Si Meong/Meong_Combo_HH.png",  2, 6, 0, 5, 0.055f, Animation.PlayMode.NORMAL);
        registerGridRowAnimation("Meong_HH_2",  "Si Meong/Meong_Combo_HH.png",  2, 6, 1, 6, 0.055f, Animation.PlayMode.NORMAL);

        // ---- COMBO LLH Finisher (Grid 3x6, baris ke-3 / index 2) ----
        registerGridRowAnimation("Meong_LLH_3", "Si Meong/Meong_Combo_LLH.png", 3, 6, 2, 5, 0.055f, Animation.PlayMode.NORMAL);
        registerLompatAnimations();

        // ---- ENEMY: TempleMonk / Corrupted Monk (Dog Skull) ----
        registerGridRowAnimation("monk_idle",    "Enemies/CorruptedMonk_Idle.png",   2, 9,  1, 6, 0.12f, Animation.PlayMode.LOOP);
        registerGridRowAnimation("monk_walk",    "Enemies/CorruptedMonk_Walk.png",   2, 12, 0, 7, 0.10f, Animation.PlayMode.LOOP);
        registerMultiRowAnimation("monk_dash",   "Enemies/CorruptedMonk_Attack.png", 6, 6,  1, 1, 0.07f, Animation.PlayMode.NORMAL);
        registerMultiRowAnimation("monk_attack", "Enemies/CorruptedMonk_Attack.png", 6, 6,  4, 5, 0.08f, Animation.PlayMode.NORMAL);

        // ---- ENEMY: ForgeBrute ----
        registerAnimationFromSheet("brute_idle",   "sprites/forge_brute.png", 48, 60, 0, 0, 4, 0.15f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("brute_walk",   "sprites/forge_brute.png", 48, 60, 1, 0, 6, 0.12f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("brute_windup", "sprites/forge_brute.png", 48, 60, 2, 0, 3, 0.20f, Animation.PlayMode.NORMAL);
        registerAnimationFromSheet("brute_slam",   "sprites/forge_brute.png", 48, 60, 3, 0, 3, 0.08f, Animation.PlayMode.NORMAL);

        // ---- ENEMY: ShieldBulwark ----
        registerAnimationFromSheet("bulwark_idle",     "sprites/shield_bulwark.png", 44, 56, 0, 0, 4, 0.15f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("bulwark_guard",    "sprites/shield_bulwark.png", 44, 56, 1, 0, 2, 0.20f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("bulwark_windup",   "sprites/shield_bulwark.png", 44, 56, 2, 0, 3, 0.20f, Animation.PlayMode.NORMAL);
        registerAnimationFromSheet("bulwark_bash",     "sprites/shield_bulwark.png", 44, 56, 3, 0, 3, 0.08f, Animation.PlayMode.NORMAL);

        // ---- ENEMY: Spire caster ----
        registerAnimationFromSheet("pyromancer_idle",   "sprites/pyromancer.png", 30, 48, 0, 0, 4, 0.15f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("pyromancer_cast",   "sprites/pyromancer.png", 30, 48, 1, 0, 4, 0.10f, Animation.PlayMode.NORMAL);

        // ---- ENEMY: RootWeaver ----
        registerAnimationFromSheet("rootweaver_idle",    "sprites/root_weaver.png", 34, 54, 0, 0, 4, 0.15f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("rootweaver_channel", "sprites/root_weaver.png", 34, 54, 1, 0, 4, 0.12f, Animation.PlayMode.NORMAL);

        // ---- BOSS: Crooked Saint (64x64) ----
        registerAnimationFromSheet("boss_idle",    "sprites/boss_crooked_saint.png", 64, 64, 0, 0, 4, 0.18f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("boss_phase2",  "sprites/boss_crooked_saint.png", 64, 64, 1, 0, 6, 0.12f, Animation.PlayMode.LOOP);
        registerAnimationFromSheet("boss_attack",  "sprites/boss_crooked_saint.png", 64, 64, 2, 0, 4, 0.08f, Animation.PlayMode.NORMAL);

        // ---- PROJECTILES ----
        registerRegionFromSheet("proj_shuriken",   "sprites/projectiles.png", 16, 16, 0, 0);
        registerRegionFromSheet("proj_fireball",   "sprites/projectiles.png", 16, 16, 0, 1);
        registerRegionFromSheet("proj_glacial",    "sprites/projectiles.png", 16, 16, 0, 2);
        registerRegionFromSheet("proj_slag",       "sprites/projectiles.png", 16, 16, 0, 3);

        // ---- ITEMS / UI ----
        registerRegionFromSheet("item_flask",      "sprites/items.png", 16, 16, 0, 0);

        // ---- PORTRAITS untuk dialog ----
        Texture pPlayer = loadTexture("portraits/player.png");
        if (pPlayer != null) registerRegion("portrait_player", new TextureRegion(pPlayer));

        Texture pSaint = loadTexture("portraits/crooked_saint.png");
        if (pSaint != null) registerRegion("portrait_crooked_saint", new TextureRegion(pSaint));

        Texture pMonk = loadTexture("portraits/temple_monk.png");
        if (pMonk != null) registerRegion("portrait_temple_monk", new TextureRegion(pMonk));
    }

    public void dispose() {
        for (Texture t : textureMap.values()) if (t != null) t.dispose();
        textureMap.clear();
        textureRegionMap.clear();
        animationMap.clear();
        frameMap.clear();
    }
}
