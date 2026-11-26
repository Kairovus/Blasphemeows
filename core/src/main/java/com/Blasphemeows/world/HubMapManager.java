package com.Blasphemeows.world;

import com.Blasphemeows.combat.AttackStrategy;
import com.Blasphemeows.combat.GreatswordAttackStrategy;
import com.Blasphemeows.combat.SpearAttackStrategy;
import com.Blasphemeows.combat.SwordAttackStrategy;
import com.Blasphemeows.core.Checkpoint;
import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.enemy.TrainingDummy;
import com.Blasphemeows.player.Player;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Tiled Main Hub map: loading, rendering, collision geometry,
 * spawn points, and interactive objects (excluding manually positioned portals).
 */
public class HubMapManager implements Disposable {
    private static final String REV_MAP_PATH = "maps/main_hub_rev.tmx";
    private static final String DEFAULT_MAP_PATH = "maps/main_hub.tmx";
    private static final float FALL_RESCUE_MARGIN = 80f;

    private final TiledMap tiledMap;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final float mapWidth;
    private final float mapHeight;
    private final int tileWidth;
    private final int tileHeight;

    private final List<PolygonCollider> colliders = new ArrayList<PolygonCollider>();
    private final List<Rectangle> interactableBounds = new ArrayList<Rectangle>();
    private final List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
    private final List<WeaponStation> weaponStations = new ArrayList<WeaponStation>();
    private UpgradeStation upgradeStation;
    private TrainingDummy trainingDummy;

    private float playerSpawnX = 670f;
    private float playerSpawnY = 500f;
    private float checkpointSpawnX = 640f;
    private float checkpointSpawnY = 336f;
    private Rectangle playerSpawnRect;
    private Rectangle checkpointSpawnRect;

    public HubMapManager(SpriteBatch sharedBatch) {
        String mapPath = Gdx.files.internal(REV_MAP_PATH).exists() ? REV_MAP_PATH : DEFAULT_MAP_PATH;

        // Custom resolver falls back to assets/tilesets/<filename> if a .tsx references an external local path.
        FileHandleResolver resolver = new FileHandleResolver() {
            @Override
            public FileHandle resolve(String fileName) {
                FileHandle direct = Gdx.files.internal(fileName);
                if (direct.exists()) {
                    return direct;
                }
                String baseName = new FileHandle(fileName).name();
                FileHandle fallback = Gdx.files.internal("tilesets/" + baseName);
                return fallback.exists() ? fallback : direct;
            }
        };

        TmxMapLoader loader = new TmxMapLoader(resolver);
        this.tiledMap = loader.load(mapPath);
        this.mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f, sharedBatch);

        MapProperties props = tiledMap.getProperties();
        int widthTiles = props.get("width", Integer.class);
        int heightTiles = props.get("height", Integer.class);
        this.tileWidth = props.get("tilewidth", Integer.class);
        this.tileHeight = props.get("tileheight", Integer.class);
        this.mapWidth = widthTiles * tileWidth;
        this.mapHeight = heightTiles * tileHeight;

        parseCollisionLayer();
        parseSpawnPointsLayer();
        parseInteractablesLayer();
    }

    private void parseCollisionLayer() {
        MapLayer collisionLayer = tiledMap.getLayers().get("Collision");
        if (collisionLayer == null) {
            return;
        }
        TiledMapTileLayer platformTileLayer = getTileLayer("Platform");
        TiledMapTileLayer terrainTileLayer = getTileLayer("Terrain");
        TiledMapTileLayer wallsTileLayer = getTileLayer("Walls");

        for (MapObject obj : collisionLayer.getObjects()) {
            boolean oneWay = isOneWayObject(obj, platformTileLayer, terrainTileLayer, wallsTileLayer);
            if (obj instanceof PolygonMapObject) {
                float[] worldVerts = ((PolygonMapObject) obj).getPolygon().getTransformedVertices();
                colliders.add(new PolygonCollider(worldVerts, oneWay));
            } else if (obj instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                colliders.add(PolygonCollider.fromRectangle(r.x, r.y, r.width, r.height, oneWay));
            }
        }
    }

    private boolean isOneWayObject(
            MapObject obj,
            TiledMapTileLayer platformLayer,
            TiledMapTileLayer terrainLayer,
            TiledMapTileLayer wallsLayer) {
        MapProperties props = obj.getProperties();
        if (props.containsKey("oneWay")) {
            return Boolean.TRUE.equals(props.get("oneWay", Boolean.class));
        }
        String type = props.get("objectType", String.class);
        if (type == null) {
            type = props.get("type", String.class);
        }
        if ("platform".equalsIgnoreCase(type)) {
            return true;
        }
        if (platformLayer == null) {
            return false;
        }

        // Auto-detect elevated ledges whose top lies on the Platform tile layer (and not Terrain/Walls).
        Rectangle bounds = getObjectBounds(obj);
        if (bounds == null) {
            return false;
        }
        int col = Math.max(0, Math.min(platformLayer.getWidth() - 1, (int) ((bounds.x + bounds.width * 0.5f) / tileWidth)));
        int topRow = Math.max(0, Math.min(platformLayer.getHeight() - 1, (int) ((bounds.y + bounds.height - 2f) / tileHeight)));
        boolean hasPlatformTile = platformLayer.getCell(col, topRow) != null;
        boolean hasSolidTile = (terrainLayer != null && terrainLayer.getCell(col, topRow) != null)
                || (wallsLayer != null && wallsLayer.getCell(col, topRow) != null);
        return hasPlatformTile && !hasSolidTile;
    }

    private void parseSpawnPointsLayer() {
        MapLayer spawnLayer = tiledMap.getLayers().get("SpawnPoints");
        if (spawnLayer == null) {
            return;
        }
        for (MapObject obj : spawnLayer.getObjects()) {
            Rectangle bounds = getObjectBounds(obj);
            if (bounds == null) {
                continue;
            }
            String name = obj.getName() != null ? obj.getName().trim() : "";
            if ("player_spawn".equalsIgnoreCase(name) || "spawn".equalsIgnoreCase(name)) {
                playerSpawnX = bounds.x;
                playerSpawnY = bounds.y;
                playerSpawnRect = new Rectangle(bounds);
            } else if ("checkpoint_spawn".equalsIgnoreCase(name) || "check_spawn".equalsIgnoreCase(name)) {
                checkpointSpawnX = bounds.x;
                checkpointSpawnY = bounds.y;
                checkpointSpawnRect = new Rectangle(bounds);
            }
        }
    }

    private void parseInteractablesLayer() {
        MapLayer interactablesLayer = tiledMap.getLayers().get("Interactables");
        if (interactablesLayer == null) {
            return;
        }
        for (MapObject obj : interactablesLayer.getObjects()) {
            MapProperties props = obj.getProperties();
            if (props.containsKey("interactable") && Boolean.FALSE.equals(props.get("interactable", Boolean.class))) {
                continue;
            }
            Rectangle bounds = getObjectBounds(obj);
            if (bounds == null) {
                continue;
            }
            interactableBounds.add(new Rectangle(bounds));

            String name = obj.getName() != null ? obj.getName().trim() : "";
            String objectType = props.get("objectType", String.class);
            if (objectType == null || objectType.trim().isEmpty()) {
                if (props.containsKey("weaponType") || name.toLowerCase().startsWith("weapon")) {
                    objectType = "weapon_station";
                } else {
                    objectType = name;
                }
            }

            if ("checkpoint".equalsIgnoreCase(objectType)) {
                String id = name.isEmpty() ? "checkpoint_01" : name;
                Checkpoint cp = new Checkpoint(
                        id,
                        "Grand Sanctuary Monolith",
                        GameManager.Region.TUTORIAL_HUB,
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height);
                cp.setRespawnPosition(checkpointSpawnX, checkpointSpawnY);
                cp.setActivated(true);
                checkpoints.add(cp);
            } else if ("upgrade_station".equalsIgnoreCase(objectType)) {
                upgradeStation = new UpgradeStation(bounds.x, bounds.y, bounds.width, bounds.height);
            } else if ("training_dummy".equalsIgnoreCase(objectType)) {
                trainingDummy = new TrainingDummy(bounds.x, bounds.y);
            } else if ("weapon_station".equalsIgnoreCase(objectType)) {
                String weaponType = props.get("weaponType", String.class);
                if (weaponType == null && name.contains("_")) {
                    weaponType = name.substring(name.indexOf('_') + 1);
                }
                AttackStrategy strategy = resolveWeaponStrategy(weaponType);
                if (strategy != null) {
                    weaponStations.add(new WeaponStation(bounds.x, bounds.y, bounds.width, bounds.height, strategy));
                }
            }
        }
    }

    private AttackStrategy resolveWeaponStrategy(String weaponType) {
        if (weaponType == null) {
            return null;
        }
        String normalized = weaponType.trim().toLowerCase();
        if ("sword".equals(normalized)) {
            return new SwordAttackStrategy();
        } else if ("greatsword".equals(normalized)) {
            return new GreatswordAttackStrategy();
        } else if ("spear".equals(normalized)) {
            return new SpearAttackStrategy();
        }
        return null;
    }

    private TiledMapTileLayer getTileLayer(String name) {
        MapLayer layer = tiledMap.getLayers().get(name);
        return layer instanceof TiledMapTileLayer ? (TiledMapTileLayer) layer : null;
    }

    private static Rectangle getObjectBounds(MapObject obj) {
        if (obj instanceof RectangleMapObject) {
            return ((RectangleMapObject) obj).getRectangle();
        }
        if (obj instanceof PolygonMapObject) {
            return ((PolygonMapObject) obj).getPolygon().getBoundingRectangle();
        }
        return null;
    }

    /** Renders all visible Tiled tile layers once using the world camera. */
    public void render(OrthographicCamera worldCamera) {
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
    }

    /** Resolves player collisions against all Tiled Collision objects (solid first, then one-way). */
    public void resolveCollisions(Player player, float previousX, float previousY) {
        for (PolygonCollider collider : colliders) {
            if (!collider.isOneWay()) {
                collider.resolveCollision(player, previousX, previousY);
            }
        }
        for (PolygonCollider collider : colliders) {
            if (collider.isOneWay()) {
                collider.resolveCollision(player, previousX, previousY);
            }
        }
    }

    /** Clamps the player horizontally to map bounds and rescues them if they fall below the map. */
    public void clampPlayerToMap(Player player) {
        float maxX = mapWidth - player.getWidth();
        if (player.getX() < 0f) {
            player.setX(0f);
        } else if (player.getX() > maxX) {
            player.setX(maxX);
        }

        if (player.getY() < -FALL_RESCUE_MARGIN) {
            player.setX(playerSpawnX);
            player.setY(playerSpawnY);
            player.setVelocityX(0f);
            player.setVelocityY(0f);
            player.getAttributes().notifyCombatLog("Fell out of the hub. Returned to spawn.");
        }
    }

    /** Renders debug outlines for Tiled collision polygons, interactable zones, and spawn points. */
    public void renderDebug(ShapeRenderer shapeRenderer) {
        for (PolygonCollider collider : colliders) {
            shapeRenderer.setColor(collider.isOneWay() ? Color.MAGENTA : Color.CYAN);
            shapeRenderer.polygon(collider.getVertices());
        }

        shapeRenderer.setColor(Color.YELLOW);
        for (Rectangle rect : interactableBounds) {
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        if (playerSpawnRect != null) {
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.rect(playerSpawnRect.x, playerSpawnRect.y, playerSpawnRect.width, playerSpawnRect.height);
            shapeRenderer.line(playerSpawnRect.x, playerSpawnRect.y,
                    playerSpawnRect.x + playerSpawnRect.width, playerSpawnRect.y + playerSpawnRect.height);
        }
        if (checkpointSpawnRect != null) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.rect(checkpointSpawnRect.x, checkpointSpawnRect.y, checkpointSpawnRect.width, checkpointSpawnRect.height);
            shapeRenderer.line(checkpointSpawnRect.x, checkpointSpawnRect.y,
                    checkpointSpawnRect.x + checkpointSpawnRect.width, checkpointSpawnRect.y + checkpointSpawnRect.height);
        }
    }

    public List<Checkpoint> getCheckpoints() { return checkpoints; }
    public List<WeaponStation> getWeaponStations() { return weaponStations; }
    public UpgradeStation getUpgradeStation() { return upgradeStation; }
    public TrainingDummy getTrainingDummy() { return trainingDummy; }
    public float getPlayerSpawnX() { return playerSpawnX; }
    public float getPlayerSpawnY() { return playerSpawnY; }
    public float getCheckpointSpawnX() { return checkpointSpawnX; }
    public float getCheckpointSpawnY() { return checkpointSpawnY; }
    public float getMapWidth() { return mapWidth; }
    public float getMapHeight() { return mapHeight; }

    @Override
    public void dispose() {
        mapRenderer.dispose();
        tiledMap.dispose();
    }
}
