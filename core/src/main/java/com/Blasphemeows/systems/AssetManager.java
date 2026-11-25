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
    private final Map<String, Texture> textureMap;

    private AssetManager() {
        textureRegionMap = new HashMap<>();
        animationMap = new HashMap<>();
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

    // ---------- Animation Helpers ----------

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
            new TextureRegion(tex, 0,               row1Y, topColW,              row1H),
            new TextureRegion(tex, topColW,         row1Y, topColW,              row1H),
            new TextureRegion(tex, topColW * 2,     row1Y, w - (topColW * 2),    row1H),
            new TextureRegion(tex, (int)(w * 0.04f), row2Y, (int)(w * 0.46f),    row2H),
            new TextureRegion(tex, (int)(w * 0.50f), row2Y, (int)(w * 0.48f),    row2H)
        };
    }

    public void registerLompatAnimations() {
        Texture texMauLompat = loadFirstExisting(
            "Si Meong/Meong_MauLompat.png"
        );
        Texture texLompat = loadFirstExisting(
            "Si Meong/Meong_Lompat.png"
        );

        if (texMauLompat != null) {
            TextureRegion[] allFrames = extract5FrameLompatSheet(texMauLompat);
            TextureRegion[] activeFrames = new TextureRegion[] {
                allFrames[1], allFrames[2], allFrames[3], allFrames[4]
            };
            Animation<TextureRegion> anim = new Animation<>(0.055f, activeFrames);
            anim.setPlayMode(Animation.PlayMode.NORMAL);
            animationMap.put("Meong_MauLompat", anim);
        } else {
            Gdx.app.error("AssetManager", "File Meong_MauLompat tidak ditemukan di folder assets/Si Meong/!");
        }

        if (texLompat != null) {
            TextureRegion[] frames = extract5FrameLompatSheet(texLompat);
            Animation<TextureRegion> anim = new Animation<>(0.08f, frames);
            anim.setPlayMode(Animation.PlayMode.NORMAL);
            animationMap.put("Meong_Lompat", anim);
        } else {
            Gdx.app.error("AssetManager", "File Meong_Lompat tidak ditemukan di folder assets/Si Meong/!");
        }
    }

    public TextureRegion getTextureRegion(String key) { return textureRegionMap.get(key); }
    public TextureRegion getRegion(String key) { return getTextureRegion(key); }
    public Animation<TextureRegion> getAnimation(String key) { return animationMap.get(key); }

    // ---------- Init Aset yang Tersedia ----------

    public void init() {
        registerMultiRowAnimation("Meong_Idle",  "Si Meong/Meong_Idle.png",   2, 6, 0, 0, 0.12f, Animation.PlayMode.LOOP);
        registerMultiRowAnimation("Meong_Run",   "Si Meong/Meong_Run.png",    2, 6, 0, 0, 0.09f, Animation.PlayMode.LOOP);
        registerMultiRowAnimation("Meong_Dash",  "Si Meong/Meong_Dash.png",   1, 6, 0, 0, 0.05f, Animation.PlayMode.NORMAL);
        registerMultiRowAnimation("Meong_Light", "Si Meong/Meong_Attack.png", 5, 5, 0, 1, 0.04f, Animation.PlayMode.NORMAL);
        registerMultiRowAnimation("Meong_Heavy", "Si Meong/Meong_Attack.png", 5, 5, 2, 3, 0.06f, Animation.PlayMode.NORMAL);

        registerLompatAnimations();
    }

    public void dispose() {
        for (Texture t : textureMap.values()) if (t != null) t.dispose();
        textureMap.clear();
        textureRegionMap.clear();
        animationMap.clear();
    }
}
