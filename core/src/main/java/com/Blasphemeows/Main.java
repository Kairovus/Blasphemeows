package com.Blasphemeows;

import com.Blasphemeows.world.*;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.Blasphemeows.combat.GreatswordAttackStrategy;
import com.Blasphemeows.combat.Hitbox;
import com.Blasphemeows.combat.Projectile;
import com.Blasphemeows.combat.SpearAttackStrategy;
import com.Blasphemeows.combat.SwordAttackStrategy;
import com.Blasphemeows.command.InputHandler;
import com.Blasphemeows.core.Checkpoint;
import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.core.SaveData;
import com.Blasphemeows.core.SaveManager;
import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.ShieldBulwark;
import com.Blasphemeows.enemy.TrainingDummy;
import com.Blasphemeows.enemy.factory.AshenFoundryEnemyFactory;
import com.Blasphemeows.enemy.factory.EnemyFactory;
import com.Blasphemeows.enemy.factory.ForgottenTempleEnemyFactory;
import com.Blasphemeows.enemy.factory.HollowAltarsEnemyFactory;
import com.Blasphemeows.enemy.factory.OvergrownSpireEnemyFactory;
import com.Blasphemeows.enemy.factory.TutorialHubEnemyFactory;
import com.Blasphemeows.model.AttributeType;
import com.Blasphemeows.model.PlayerAttributes;
import com.Blasphemeows.net.ApiClient;
import com.Blasphemeows.net.AuthSession;
import com.Blasphemeows.observer.CombatLogObserver;
import com.Blasphemeows.player.Player;
import com.Blasphemeows.score.ScoreTracker;
import com.Blasphemeows.state.AttackState;
import com.Blasphemeows.state.ComboRecoveryState;
import com.Blasphemeows.state.DashState;
import com.Blasphemeows.state.JumpState;
import com.Blasphemeows.state.MoveState;
import com.Blasphemeows.state.PlayerState;
import com.Blasphemeows.state.RootedState;
import com.Blasphemeows.state.SeismicSlamState;
import com.Blasphemeows.systems.AssetManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BLASPHEMEOWS - Main Game Runner
 * Full integration of:
 * 1. Checkpoint & Persistent Save/Load System (SaveManager)
 * 2. Combo Combat System (L->L->L, H->H, L->L->H with buffering & frame-specific damage)
 * 3. Main Hub built from the hand-drawn sketch: sanctuary dais with frame, Target Practice
 *    (Passive/Aggressive), Upgrade Station, Central Checkpoint, 3 Weapon Stations,
 *    two floating cliffs, a staircase, a bridge and a canyon ramp to the four portals
 * 4. Restricted weapon switching (hub-only via Weapon Stations)
 * 5. RPG Attributes, XP drops, Leveling, and Stat Scaling
 * 6. Always-visible debug hitbox visualization
 * 7. Regional Portals with lock gates and seamless camera follow
 */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Dual cameras: World (smooth follow) and UI (fixed screen space)
    private OrthographicCamera worldCamera;
    private OrthographicCamera uiCamera;

    private Player player;
    private InputHandler inputHandler;
    private EnemyFactory currentEnemyFactory;
    private HubMapManager hubMapManager;
    private boolean debugRender = false;

    // Player & Enemy Animation state
    private float playerAnimTime = 0f;
    private float enemyAnimTime = 0f;
    private String lastAnimKey = "";

    // World entities
    private final List<Enemy> activeEnemies = new ArrayList<Enemy>();
    private final List<Projectile> activeWorldProjectiles = new ArrayList<Projectile>();
    private final List<RegionPortal> activePortals = new ArrayList<RegionPortal>();
    private final List<Platform> activePlatforms = new ArrayList<Platform>();
    private final List<Terrain> activeTerrains = new ArrayList<Terrain>();
    private final List<Checkpoint> activeCheckpoints = new ArrayList<Checkpoint>();
    private final List<WeaponStation> activeWeaponStations = new ArrayList<WeaponStation>();
    private UpgradeStation upgradeStation;
    private TrainingDummy hubTrainingDummy;

    // Hub-only spawn fallback
    private float hubSpawnX;
    private float hubSpawnY;

    private Checkpoint lastActivatedCheckpoint;
    private final List<String> recentLogs = new ArrayList<String>();
    private final float groundY = 80f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        // 0. Load Sprite Assets
        AssetManager.getInstance().init();

        // 1. Initialize Cameras
        worldCamera = new OrthographicCamera();
        worldCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 2. Initialize Player & Attributes (Observer Pattern)
        PlayerAttributes attributes = new PlayerAttributes(10, 10, 10, 10);
        player = new Player(attributes, new SwordAttackStrategy());
        player.setGroundY(groundY);
        player.setY(groundY);

        attributes.registerObserver(new CombatLogObserver() {
            @Override
            public void onCombatLog(String message) {
                super.onCombatLog(message);
                recentLogs.add(message);
                if (recentLogs.size() > 7) {
                    recentLogs.remove(0);
                }
            }
        });

        // 3. Initialize InputHandler & Contextual Interaction Routing
        inputHandler = new InputHandler();
        inputHandler.setInteractionHandler(p -> handleContextualInteraction());

        // 4. Load Tiled Main Hub map
        hubMapManager = new HubMapManager(batch);

        // 5. Load saved game if exists, otherwise boot into Main Hub
        boolean loaded = SaveManager.getInstance().loadGame(player);
        if (loaded) {
            SaveData save = SaveManager.getInstance().getCurrentSaveData();
            try {
                GameManager.Region savedRegion = GameManager.Region.valueOf(save.lastCheckpointRegion);
                if (savedRegion != GameManager.Region.TUTORIAL_HUB) {
                    Checkpoint restored = new Checkpoint(
                        save.lastCheckpointId != null ? save.lastCheckpointId : savedRegion.name().toLowerCase() + "_checkpoint",
                        savedRegion.getDisplayName() + " Sanctuary",
                        savedRegion,
                        save.respawnX - 20f,
                        save.respawnY,
                        40f,
                        68f
                    );
                    restored.setRespawnPosition(save.respawnX, save.respawnY);
                    restored.setActivated(true);
                    lastActivatedCheckpoint = restored;
                }
                setRegion(savedRegion);
                if (savedRegion != GameManager.Region.TUTORIAL_HUB) {
                    player.setX(save.respawnX);
                    player.setY(save.respawnY);
                }
            } catch (Exception e) {
                setRegion(GameManager.Region.TUTORIAL_HUB);
            }
        } else {
            setRegion(GameManager.Region.TUTORIAL_HUB);
        }
        snapCameraToPlayer();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        worldCamera.setToOrtho(false, width, height);
        uiCamera.setToOrtho(false, width, height);
        if (player != null) snapCameraToPlayer();
    }

    public void setRegion(GameManager.Region region) {
        GameManager.getInstance().setCurrentRegion(region);
        activePortals.clear();
        activeEnemies.clear();
        activeWorldProjectiles.clear();
        activePlatforms.clear();
        activeTerrains.clear();
        activeCheckpoints.clear();
        activeWeaponStations.clear();
        upgradeStation = null;
        hubTrainingDummy = null;

        if (region == GameManager.Region.TUTORIAL_HUB) {
            player.setGroundY(0f);
            setupMainHubEnvironment();
            player.setX(hubSpawnX);
            player.setY(hubSpawnY);
        } else {
            player.setGroundY(groundY);
            setupCombatRegion(region);
        }

        snapCameraToPlayer();
        player.getAttributes().notifyCombatLog("Entered: " + region.getDisplayName());
    }

    /** Configures the Main Hub from the loaded Tiled map while keeping portals manually positioned. */
    private void setupMainHubEnvironment() {
        currentEnemyFactory = new TutorialHubEnemyFactory();

        activeCheckpoints.addAll(hubMapManager.getCheckpoints());

        upgradeStation = hubMapManager.getUpgradeStation();
        activeWeaponStations.addAll(hubMapManager.getWeaponStations());

        hubTrainingDummy = hubMapManager.getTrainingDummy();
        if (hubTrainingDummy != null) {
            activeEnemies.add(hubTrainingDummy);
        }

        hubSpawnX = hubMapManager.getPlayerSpawnX();
        hubSpawnY = hubMapManager.getPlayerSpawnY();

        // Portals remain manually positioned and rendered in Main.java (not loaded from Tiled).
        activePortals.add(new RegionPortal(18f, 688.5f, 44f, 76f,
            GameManager.Region.FORGOTTEN_TEMPLE, Color.TEAL, "Forgotten Temple (Top-Left)"));
        activePortals.add(new RegionPortal(18f, 304f, 44f, 62f,
            GameManager.Region.OVERGROWN_SPIRE, Color.CHARTREUSE, "Overgrown Spire (Left)"));
        activePortals.add(new RegionPortal(1520f, 48.5f, 42f, 64f,
            GameManager.Region.ASHEN_FOUNDRY, Color.ORANGE, "Ashen Foundry (Bottom-Right)"));
        activePortals.add(new RegionPortal(1540f, 522f, 42f, 76f,
            GameManager.Region.HOLLOW_ALTARS, Color.PURPLE, "The Hollow Altars (Right Side)"));
    }

    private void setupCombatRegion(GameManager.Region region) {
        player.setX(160f);
        player.setY(groundY);

        switch (region) {
            case FORGOTTEN_TEMPLE:
                currentEnemyFactory = new ForgottenTempleEnemyFactory();
                break;
            case ASHEN_FOUNDRY:
                currentEnemyFactory = new AshenFoundryEnemyFactory();
                break;
            case OVERGROWN_SPIRE:
                currentEnemyFactory = new OvergrownSpireEnemyFactory();
                break;
            case HOLLOW_ALTARS:
                currentEnemyFactory = new HollowAltarsEnemyFactory();
                break;
            default:
                break;
        }

        // Return portal to Main Hub
        activePortals.add(new RegionPortal(20f, groundY, 46f, 76f,
            GameManager.Region.TUTORIAL_HUB, Color.LIGHT_GRAY, "Return to Main Hub"));

        // Regional Checkpoint
        Checkpoint regionCheckpoint = new Checkpoint(region.name().toLowerCase() + "_checkpoint",
            region.getDisplayName() + " Sanctuary", region, 90f, groundY, 40f, 68f);
        if (lastActivatedCheckpoint != null && lastActivatedCheckpoint.getRegion() == region) {
            regionCheckpoint.setActivated(true);
        }
        activeCheckpoints.add(regionCheckpoint);

        // Combat platforms for aerial engagement
        activePlatforms.add(new Platform(320f, 140f, 180f, 14f));
        activePlatforms.add(new Platform(560f, 210f, 180f, 14f));
        activePlatforms.add(new Platform(800f, 150f, 180f, 14f));

        // Spawn regional foes
        float spawnStartX = 420f;
        activeEnemies.add(currentEnemyFactory.createMeleeEnemy(spawnStartX, groundY));
        activeEnemies.add(currentEnemyFactory.createRangedEnemy(spawnStartX + 160f, groundY));
        activeEnemies.add(currentEnemyFactory.createSpecialEnemy(spawnStartX + 320f, groundY));
    }

    /**
     * Handles contextual interaction with Checkpoints, Portals, Upgrade Altar,
     * Target Practice mode toggle, and Weapon Stations.
     */
    private void handleContextualInteraction() {
        float px = player.getX();
        float py = player.getY();

        // 1. Checkpoint interaction
        for (Checkpoint cp : activeCheckpoints) {
            if (cp.isPlayerNearby(px, py)) {
                if (cp.getRegion() == GameManager.Region.TUTORIAL_HUB) {
                    cp.setActivated(true);
                    player.getAttributes().restoreFullHealthAndStamina();
                    SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
                    if (lastActivatedCheckpoint != null && lastActivatedCheckpoint.getRegion() != GameManager.Region.TUTORIAL_HUB) {
                        setRegion(lastActivatedCheckpoint.getRegion());
                        player.setX(lastActivatedCheckpoint.getRespawnX());
                        player.setY(lastActivatedCheckpoint.getRespawnY());
                        snapCameraToPlayer();
                        player.getAttributes().notifyCombatLog("Warped to last checkpoint: " + lastActivatedCheckpoint.getName());
                    } else {
                        player.getAttributes().notifyCombatLog("Sanctuary restored & saved. No regional checkpoint activated yet.");
                    }
                } else {
                    lastActivatedCheckpoint = cp;
                    cp.activate(player);
                }
                return;
            }
        }

        // 2. Upgrade Altar interaction
        if (upgradeStation != null && upgradeStation.isPlayerNearby(px, py)) {
            upgradeStation.toggleMenu();
            return;
        }

        // 3. Target Practice dummy mode toggle
        if (hubTrainingDummy != null && Math.abs(px - hubTrainingDummy.getX()) < 65f && Math.abs(py - hubTrainingDummy.getY()) < 65f) {
            hubTrainingDummy.toggleMode();
            player.getAttributes().notifyCombatLog("Target Practice Mode: " +
                (hubTrainingDummy.isAggressiveMode() ? "AGGRESSIVE (Will Strike Back)" : "PASSIVE (Training Dummy)"));
            return;
        }

        // 4. Weapon Selection Stations
        for (WeaponStation ws : activeWeaponStations) {
            if (ws.isPlayerNearby(px, py)) {
                ws.interact(player);
                SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
                return;
            }
        }

        // 5. Portal interactions
        for (RegionPortal portal : activePortals) {
            if (portal.isPlayerNearby(px, py)) {
                if (portal.isLocked()) {
                    player.getAttributes().notifyCombatLog(portal.getLabel() + " is sealed! Clear all 3 branch regions first.");
                    return;
                }
                setRegion(portal.getDestination());
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Camera + hub helpers
    // ------------------------------------------------------------------
    private float cameraTargetY() {
        return Math.max(groundY + worldCamera.viewportHeight * 0.195f, player.getY() + 30f);
    }

    /** Puts the camera directly on the player (no lerp), used after teleports and region changes. */
    private void snapCameraToPlayer() {
        worldCamera.position.x = player.getX() + player.getWidth() / 2f;
        worldCamera.position.y = cameraTargetY();
        worldCamera.update();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleDebugKeys();

        // Check player death and respawn
        if (player.getAttributes().isDead()) {
            handlePlayerDeath();
            return;
        }

        GameManager.Region cur = GameManager.getInstance().getCurrentRegion();

        // 1. Update Player Input & Movement/Jump
        float prevX = player.getX();
        float prevY = player.getY();
        inputHandler.handleInput(player);
        player.update(delta);

        // World collisions
        if (cur == GameManager.Region.TUTORIAL_HUB && hubMapManager != null) {
            hubMapManager.resolveCollisions(player, prevX, prevY);
            hubMapManager.clampPlayerToMap(player);
        } else {
            for (Platform platform : activePlatforms) {
                platform.resolveSolidCollision(player, prevX, prevY);
            }
            for (Terrain terrain : activeTerrains) {
                terrain.resolveCollision(player, prevX, prevY);
            }
        }

        // 2. Camera Smoothly Follows the Player (Lerp)
        float targetCamX = player.getX() + player.getWidth() / 2f;
        float targetCamY = cameraTargetY();
        worldCamera.position.x += (targetCamX - worldCamera.position.x) * 5.5f * delta;
        worldCamera.position.y += (targetCamY - worldCamera.position.y) * 5.5f * delta;
        worldCamera.update();
        uiCamera.update();

        // 3. Collect Player Projectiles (e.g. Glacial Orb)
        if (!player.getActiveProjectiles().isEmpty()) {
            activeWorldProjectiles.addAll(player.getActiveProjectiles());
            player.getActiveProjectiles().clear();
        }

        // 4. Update Enemies & Collect Enemy Projectiles
        for (int i = activeEnemies.size() - 1; i >= 0; i--) {
            Enemy enemy = activeEnemies.get(i);
            enemy.update(delta, player);

            // Harvest projectiles spawned by enemy
            enemy.drainPendingProjectiles(activeWorldProjectiles);

            // Check collision with player attack hitboxes
            for (Hitbox hitbox : player.getActiveHitboxes()) {
                // IMPORTANT: Damage detection ONLY occurs during active attack frames!
                if (hitbox.canHitEnemy(enemy) && hitbox.overlaps(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                    boolean hitProcessed = enemy.takeHit(hitbox.getDamage(), hitbox.canBreakShield(), player.getX());
                    if (hitProcessed) {
                        hitbox.registerHit(enemy);
                    }
                }
            }

            // Enemy death & XP / score distribution
            if (!enemy.isAlive()) {
                int xpEarned = enemy.getXpReward();
                player.getAttributes().addXP(xpEarned);
                ScoreTracker.getInstance().addKill(enemy);
                SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
                player.getAttributes().notifyCombatLog("Defeated " + enemy.getName() + "! (+" + xpEarned + " XP, +"
                        + enemy.getScoreType().getPoints() + " score)");
                activeEnemies.remove(i);
            }
        }

        // 5. Update and Resolve All Projectiles
        for (int i = activeWorldProjectiles.size() - 1; i >= 0; i--) {
            Projectile proj = activeWorldProjectiles.get(i);
            proj.update(delta, groundY);

            if (proj.isActive()) {
                if (proj.isHostile()) {
                    if (proj.overlaps(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
                        proj.onHitPlayer(player);
                    }
                } else {
                    for (Enemy enemy : activeEnemies) {
                        if (enemy.isAlive() && proj.overlaps(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight())) {
                            proj.onHitEnemy(enemy);
                            break;
                        }
                    }
                }
            }

            if (!proj.isActive()) {
                activeWorldProjectiles.remove(i);
            }
        }

        // Clear Screen with Background color based on current region
        if (cur == GameManager.Region.TUTORIAL_HUB) {
            ScreenUtils.clear(0.09f, 0.11f, 0.15f, 1f); // Sanctuary twilight slate
        } else if (cur == GameManager.Region.FORGOTTEN_TEMPLE) {
            ScreenUtils.clear(0.12f, 0.16f, 0.14f, 1f); // Temple mossy teal
        } else if (cur == GameManager.Region.ASHEN_FOUNDRY) {
            ScreenUtils.clear(0.18f, 0.11f, 0.08f, 1f); // Industrial rust orange
        } else if (cur == GameManager.Region.OVERGROWN_SPIRE) {
            ScreenUtils.clear(0.08f, 0.15f, 0.12f, 1f); // Mystic botanical green
        } else {
            ScreenUtils.clear(0.15f, 0.08f, 0.18f, 1f); // Crooked Saints purple
        }

        // ==========================================
        // RENDER WORLD (Using World Camera)
        // ==========================================
        if (cur == GameManager.Region.TUTORIAL_HUB && hubMapManager != null) {
            hubMapManager.render(worldCamera);
        }

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Combat regions retain their dark floor and platforms; the Main Hub is rendered via Tiled.
        if (cur != GameManager.Region.TUTORIAL_HUB) {
            shapeRenderer.setColor(0.18f, 0.18f, 0.20f, 1f);
            shapeRenderer.rect(-10000, 0, 20000, groundY);
            shapeRenderer.setColor(0.25f, 0.25f, 0.28f, 1f);
            for (int gx = -4000; gx <= 4000; gx += 200) {
                shapeRenderer.rect(gx, groundY - 10, 4, 10);
            }
            for (Platform platform : activePlatforms) {
                shapeRenderer.setColor(platform.getColor());
                shapeRenderer.rect(platform.getX(), platform.getY(), platform.getWidth(), platform.getHeight());
                if (platform.hasTopRim()) {
                    shapeRenderer.setColor(0.35f, 0.35f, 0.40f, 1f);
                    shapeRenderer.rect(platform.getX(), platform.getY() + platform.getHeight() - 3, platform.getWidth(), 3);
                }
            }
        }

        // Render Portals in World
        for (RegionPortal portal : activePortals) {
            Color pColor = portal.isLocked() ? Color.DARK_GRAY : portal.getColor();
            shapeRenderer.setColor(0.30f, 0.30f, 0.35f, 1f);
            shapeRenderer.rect(portal.getX() - 6, portal.getY(), 6, portal.getHeight());
            shapeRenderer.rect(portal.getX() + portal.getWidth(), portal.getY(), 6, portal.getHeight());
            shapeRenderer.rect(portal.getX() - 6, portal.getY() + portal.getHeight(), portal.getWidth() + 12, 8);
            shapeRenderer.setColor(pColor);
            shapeRenderer.rect(portal.getX(), portal.getY(), portal.getWidth(), portal.getHeight());
        }

        // Render Checkpoints (Red element - Majestic Monolith)
        for (Checkpoint cp : activeCheckpoints) {
            shapeRenderer.setColor(0.15f, 0.15f, 0.18f, 1f);
            shapeRenderer.rect(cp.getX() - 6f, cp.getY(), cp.getWidth() + 12f, 14f); // Base plinth

            shapeRenderer.setColor(cp.getColor());
            shapeRenderer.rect(cp.getX(), cp.getY() + 14f, cp.getWidth(), cp.getHeight() - 14f); // Monolith pillar

            // Inner crimson & gold flame core
            shapeRenderer.setColor(cp.isActivated() ? Color.GOLD : Color.GRAY);
            shapeRenderer.circle(cp.getX() + cp.getWidth() / 2f, cp.getY() + cp.getHeight() - 24f, 14f);
            shapeRenderer.setColor(cp.isActivated() ? Color.FIREBRICK : Color.DARK_GRAY);
            shapeRenderer.circle(cp.getX() + cp.getWidth() / 2f, cp.getY() + cp.getHeight() - 24f, 8f);
        }

        // Render Character Upgrade Station (Green element)
        if (upgradeStation != null) {
            shapeRenderer.setColor(upgradeStation.getColor());
            shapeRenderer.rect(upgradeStation.getX(), upgradeStation.getY(), upgradeStation.getWidth(), upgradeStation.getHeight());
            shapeRenderer.setColor(Color.LIME);
            shapeRenderer.rect(upgradeStation.getX() + 6, upgradeStation.getY() + 10, upgradeStation.getWidth() - 12, 12);
        }

        // Render Weapon Stations (Three Blue elements)
        for (WeaponStation ws : activeWeaponStations) {
            shapeRenderer.setColor(ws.getColor());
            shapeRenderer.rect(ws.getX(), ws.getY(), ws.getWidth(), ws.getHeight());
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(ws.getX() + 8, ws.getY() + 12, ws.getWidth() - 16, ws.getHeight() - 24);
        }

        // Render Player fallback box only if sprite assets are not loaded
        boolean hasPlayerSprite = AssetManager.getInstance().getAnimation("Meong_Idle") != null;
        if (!hasPlayerSprite) {
            if (player.isInvincible()) {
                shapeRenderer.setColor(Color.CYAN);
            } else if (player.getStateMachine().getCurrentState() instanceof RootedState) {
                shapeRenderer.setColor(Color.BROWN);
            } else {
                shapeRenderer.setColor(Color.WHITE);
            }
            shapeRenderer.rect(player.getX(), player.getY(), player.getWidth(), player.getHeight());

            // Player eye indicator
            shapeRenderer.setColor(Color.GOLD);
            float eyeX = player.isFacingRight() ? (player.getX() + player.getWidth() - 8) : (player.getX() + 2);
            shapeRenderer.rect(eyeX, player.getY() + player.getHeight() - 14, 6, 6);
        }

        // Render Projectiles
        for (Projectile proj : activeWorldProjectiles) {
            shapeRenderer.setColor(proj.getType().getColor());
            if (proj.getType() == Projectile.Type.GLACIAL_ORB || proj.getType() == Projectile.Type.FIREBALL) {
                shapeRenderer.circle(proj.getX() + proj.getWidth() / 2f, proj.getY() + proj.getHeight() / 2f, proj.getWidth() / 2f);
            } else {
                shapeRenderer.rect(proj.getX(), proj.getY(), proj.getWidth(), proj.getHeight());
            }
        }

        // Render Enemies / Target Practice Dummy (shapes for enemies without sprite; health bar for all)
        for (Enemy enemy : activeEnemies) {
            boolean isMonk = enemy.getName().toLowerCase().contains("monk");
            boolean hasMonkSprite = isMonk && AssetManager.getInstance().getAnimation("monk_idle") != null;

            if (!hasMonkSprite) {
                if (enemy.isFrozen()) {
                    shapeRenderer.setColor(Color.SKY);
                } else if (enemy.isStunnedOrFrozen()) {
                    shapeRenderer.setColor(Color.YELLOW);
                } else if (enemy.isTelegraphingAttack()) {
                    // On-enemy visual warning during attack preparation
                    shapeRenderer.setColor(Color.RED);
                } else if (enemy instanceof TrainingDummy) {
                    TrainingDummy td = (TrainingDummy) enemy;
                    shapeRenderer.setColor(td.isAggressiveMode() ? Color.ORANGE : Color.YELLOW); // Target Practice Yellow
                } else if (enemy instanceof ShieldBulwark) {
                    ShieldBulwark bulwark = (ShieldBulwark) enemy;
                    shapeRenderer.setColor(bulwark.isInVulnerableWindow() ? Color.RED : Color.DARK_GRAY);
                } else {
                    shapeRenderer.setColor(Color.CORAL);
                }
                shapeRenderer.rect(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
            }

            // Health bar
            float barW = enemy.getWidth();
            float hpPct = (float) enemy.getHp() / enemy.getMaxHp();
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.rect(enemy.getX(), enemy.getY() + enemy.getHeight() + 6, barW, 5);
            shapeRenderer.setColor(enemy instanceof TrainingDummy ? Color.GREEN : Color.RED);
            shapeRenderer.rect(enemy.getX(), enemy.getY() + enemy.getHeight() + 6, barW * hpPct, 5);
        }
        shapeRenderer.end();

        // ==========================================
        // RENDER PLAYER & ENEMY SPRITES
        // ==========================================
        enemyAnimTime += delta;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // Enemy Sprites (Corrupted Monk / Temple Monk)
        for (Enemy enemy : activeEnemies) {
            if (!enemy.isAlive()) continue;
            if (!enemy.getName().toLowerCase().contains("monk")) continue;

            String enemyAnimKey = "monk_idle";
            Hitbox ehb = enemy.getCurrentAttackHitbox();
            if (ehb != null && !ehb.isFinished()) {
                enemyAnimKey = "monk_attack";
            } else if (Math.abs(enemy.getX() - player.getX()) > 70f) {
                enemyAnimKey = "monk_walk";
            }

            Animation<TextureRegion> eAnim = AssetManager.getInstance().getAnimation(enemyAnimKey);
            if (eAnim != null) {
                TextureRegion eFrame = eAnim.getKeyFrame(enemyAnimTime, true);

                if (enemy.isFrozen()) {
                    batch.setColor(Color.SKY);
                } else if (enemy.isStunnedOrFrozen()) {
                    batch.setColor(Color.YELLOW);
                } else {
                    batch.setColor(Color.WHITE);
                }

                float aspectRatio = (float) eFrame.getRegionWidth() / (float) eFrame.getRegionHeight();
                float eDrawH = 120f;
                float eDrawW = eDrawH * aspectRatio;
                boolean enemyFacingRight = player.getX() >= enemy.getX();

                float eDrawX = enemy.getX() + (enemy.getWidth() / 2f) - (eDrawW / 2f);
                float eDrawY = enemy.getY();

                if (enemyFacingRight) {
                    batch.draw(eFrame, eDrawX, eDrawY, eDrawW, eDrawH);
                } else {
                    batch.draw(eFrame, eDrawX + eDrawW, eDrawY, -eDrawW, eDrawH);
                }
                batch.setColor(Color.WHITE);
            }
        }

        // Player Sprite (Si Meong)
        String animKey = "Meong_Idle";
        PlayerState state = player.getStateMachine().getCurrentState();

        if (state instanceof AttackState) {
            AttackState atkState = (AttackState) state;
            String seq = player.getComboManager().getSequenceString();
            String baseAttackKey;

            if (atkState.getAttackData() != null && atkState.getAttackData().isHeavy()) {
                if (seq.endsWith("L, L, H")) {
                    baseAttackKey = "Meong_LLH_3";
                } else if (seq.endsWith("H, H")) {
                    baseAttackKey = "Meong_HH_2";
                } else {
                    baseAttackKey = "Meong_Heavy";
                }
            } else {
                if (seq.endsWith("L, L, L")) {
                    baseAttackKey = "Meong_LLL_3";
                } else if (seq.endsWith("L, L")) {
                    baseAttackKey = "Meong_LLL_2";
                } else {
                    baseAttackKey = "Meong_Light";
                }
            }

            animKey = baseAttackKey + "#" + seq;
        } else if (state instanceof DashState) {
            animKey = "Meong_Dash";
        } else if (state instanceof JumpState || state instanceof SeismicSlamState) {
            animKey = (player.getVelocityY() > -60f) ? "Meong_MauLompat" : "Meong_Lompat";
        } else if (state instanceof MoveState) {
            animKey = "Meong_Run";
        }

        if (!animKey.equals(lastAnimKey)) {
            playerAnimTime = 0f;
            lastAnimKey = animKey;
        } else {
            playerAnimTime += delta;
        }

        String lookupKey = animKey.contains("#") ? animKey.substring(0, animKey.indexOf("#")) : animKey;
        Animation<TextureRegion> currentAnim = AssetManager.getInstance().getAnimation(lookupKey);
        if (currentAnim == null) {
            currentAnim = AssetManager.getInstance().getAnimation("Meong_Idle");
        }

        if (currentAnim != null) {
            TextureRegion frame = currentAnim.getKeyFrame(playerAnimTime);

            if (player.isInvincible()) {
                batch.setColor(Color.CYAN);
            } else if (state instanceof RootedState) {
                batch.setColor(Color.BROWN);
            } else {
                batch.setColor(Color.WHITE);
            }

            boolean isLompatAnim = lookupKey.equals("Meong_MauLompat") || lookupKey.equals("Meong_Lompat");
            boolean isAttackAnim = lookupKey.equals("Meong_Light")
                || lookupKey.equals("Meong_Heavy")
                || lookupKey.startsWith("Meong_LLL")
                || lookupKey.startsWith("Meong_HH")
                || lookupKey.startsWith("Meong_LLH");

            float drawW;
            float drawH;
            float xOffset;
            float drawY;

            if (isLompatAnim) {
                drawW = 62f;
                drawH = 78f;
                xOffset = player.isFacingRight() ? 2f : -2f;
                drawY = player.getY() - 2f;
            } else if (isAttackAnim) {
                drawW = 140f;
                drawH = 140f;
                xOffset = player.isFacingRight() ? 14f : -14f;
                drawY = player.getY() - 36f;
            } else if (lookupKey.equals("Meong_Dash")) {
                float aspectRatio = (float) frame.getRegionWidth() / (float) frame.getRegionHeight();
                drawH = 205f;
                drawW = drawH * aspectRatio;
                xOffset = player.isFacingRight() ? 2f : -2f;
                drawY = player.getY() - 78f;
            } else if (lookupKey.equals("Meong_Run")) {
                drawW = 140f;
                drawH = 140f;
                xOffset = player.isFacingRight() ? 14f : -14f;
                drawY = player.getY() - 20f;
            } else {
                drawW = 140f;
                drawH = 140f;
                xOffset = player.isFacingRight() ? 14f : -14f;
                drawY = player.getY() - 28f;
            }

            float drawX = player.getX() + (player.getWidth() / 2f) - (drawW / 2f) + xOffset;

            if (player.isFacingRight()) {
                batch.draw(frame, drawX, drawY, drawW, drawH);
            } else {
                batch.draw(frame, drawX + drawW, drawY, -drawW, drawH);
            }
            batch.setColor(Color.WHITE);
        }
        batch.end();

        // ==========================================
        // ALWAYS-VISIBLE DEBUG HITBOX VISUALIZATION (Overlaid with alpha)
        // (Player Attack Hitboxes & Enemy Attack Hitboxes)
        // ==========================================
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 1. Player attack hitboxes (semi-transparent)
        for (Hitbox hb : player.getActiveHitboxes()) {
            if (!hb.isFinished()) {
                Color c = hb.getDebugColor();
                shapeRenderer.setColor(c.r, c.g, c.b, 0.45f);
                shapeRenderer.rect(hb.getX(), hb.getY(), hb.getWidth(), hb.getHeight());
            }
        }

        // 2. Enemy attack hitboxes (Telegraph Startup = Yellow, Active = Red, Recovery = Cyan)
        for (Enemy enemy : activeEnemies) {
            Hitbox ehb = enemy.getCurrentAttackHitbox();
            if (ehb != null && !ehb.isFinished()) {
                Color c = ehb.getDebugColor();
                shapeRenderer.setColor(c.r, c.g, c.b, 0.45f);
                shapeRenderer.rect(ehb.getX(), ehb.getY(), ehb.getWidth(), ehb.getHeight());
            }
        }
        shapeRenderer.end();

        // 3. Enemy & Player Body Hurtbox outlines (Line mode)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Enemy enemy : activeEnemies) {
            if (enemy.isAlive()) {
                shapeRenderer.setColor(Color.ORANGE);
                shapeRenderer.rect(enemy.getX() - 1, enemy.getY() - 1, enemy.getWidth() + 2, enemy.getHeight() + 2);
                shapeRenderer.setColor(Color.GOLD);
                shapeRenderer.rect(enemy.getX() - 3, enemy.getY() - 3, enemy.getWidth() + 6, enemy.getHeight() + 6);
            }
        }
        // Player body hurtbox outline
        shapeRenderer.setColor(Color.LIME);
        shapeRenderer.rect(player.getX() - 1, player.getY() - 1, player.getWidth() + 2, player.getHeight() + 2);

        // Player attack hitbox outlines
        for (Hitbox hb : player.getActiveHitboxes()) {
            if (!hb.isFinished()) {
                shapeRenderer.setColor(hb.getDebugColor());
                shapeRenderer.rect(hb.getX(), hb.getY(), hb.getWidth(), hb.getHeight());
            }
        }
        if (debugRender && cur == GameManager.Region.TUTORIAL_HUB && hubMapManager != null) {
            hubMapManager.renderDebug(shapeRenderer);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Render World Text (Labels & Prompts)
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // Checkpoint prompts
        for (Checkpoint cp : activeCheckpoints) {
            font.setColor(Color.RED);
            font.draw(batch, cp.getName(), cp.getX() - 25f, cp.getY() + cp.getHeight() + 22f);
            if (cp.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                String prompt;
                if (cp.getRegion() == GameManager.Region.TUTORIAL_HUB
                        && lastActivatedCheckpoint != null
                        && lastActivatedCheckpoint.getRegion() != GameManager.Region.TUTORIAL_HUB) {
                    prompt = "[F / ENTER] Travel to " + lastActivatedCheckpoint.getName() + " & Save";
                } else {
                    prompt = "[F / ENTER] Activate Checkpoint & Save";
                }
                font.draw(batch, prompt, cp.getX() - 65f, cp.getY() + cp.getHeight() + 40f);
            }
        }

        // Upgrade Station prompt
        if (upgradeStation != null) {
            font.setColor(Color.GREEN);
            font.draw(batch, "Upgrade Altar", upgradeStation.getX() - 10f, upgradeStation.getY() + upgradeStation.getHeight() + 22f);
            if (upgradeStation.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                font.draw(batch, upgradeStation.getPromptText(), upgradeStation.getX() - 50f, upgradeStation.getY() + upgradeStation.getHeight() + 40f);
            }
        }

        // Weapon Stations prompts (names are staggered over three rows because the stations are only 28 apart)
        int stationIndex = 0;
        for (WeaponStation ws : activeWeaponStations) {
            font.setColor(Color.SKY);
            font.draw(batch, ws.getWeaponStrategy().getWeaponName(), ws.getX() - 30f,
                ws.getY() + ws.getHeight() + 22f + (stationIndex % 3) * 16f);
            if (ws.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                font.draw(batch, ws.getPromptText(), ws.getX() - 50f, ws.getY() + ws.getHeight() + 74f);
            }
            stationIndex++;
        }

        // Target Practice prompt (sits above the [HURTBOX] debug label)
        if (hubTrainingDummy != null) {
            font.setColor(Color.YELLOW);
            String modeStr = hubTrainingDummy.isAggressiveMode() ? "Aggressive Mode" : "Passive Mode";
            font.draw(batch, "Target Practice (" + modeStr + ")", hubTrainingDummy.getX() - 35f, hubTrainingDummy.getY() + hubTrainingDummy.getHeight() + 40f);
            if (Math.abs(player.getX() - hubTrainingDummy.getX()) < 65f && Math.abs(player.getY() - hubTrainingDummy.getY()) < 65f) {
                font.setColor(Color.GOLD);
                font.draw(batch, "[F / ENTER] Toggle Practice Mode", hubTrainingDummy.getX() - 50f, hubTrainingDummy.getY() + hubTrainingDummy.getHeight() + 58f);
            }
        }

        // Portals prompts
        for (RegionPortal portal : activePortals) {
            font.setColor(portal.isLocked() ? Color.LIGHT_GRAY : portal.getColor());
            font.draw(batch, portal.getLabel(), portal.getX() - 15f, portal.getY() + portal.getHeight() + 24f);
            if (portal.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                font.draw(batch, portal.getPromptText(), portal.getX() - 55f, portal.getY() + portal.getHeight() + 44f);
            }
        }

        // Hitbox text overlays (Enemy Hurtbox & Attack Hitbox phases)
        for (Enemy enemy : activeEnemies) {
            if (enemy.isAlive()) {
                font.setColor(Color.ORANGE);
                font.draw(batch, "[HURTBOX " + (int) enemy.getWidth() + "x" + (int) enemy.getHeight() + "]",
                    enemy.getX() - 12f, enemy.getY() + enemy.getHeight() + 18f);

                Hitbox ehb = enemy.getCurrentAttackHitbox();
                if (ehb != null && !ehb.isFinished()) {
                    font.setColor(ehb.getDebugColor());
                    font.draw(batch, "[ENEMY ATK: " + ehb.getCurrentPhase() + "]",
                        ehb.getX() - 15f, ehb.getY() + ehb.getHeight() + 16f);
                }
            }
        }

        // Player active attack hitbox phase text overlay
        for (Hitbox hb : player.getActiveHitboxes()) {
            if (!hb.isFinished()) {
                font.setColor(hb.getDebugColor());
                font.draw(batch, "[PL ATK: " + hb.getCurrentPhase() + "]",
                    hb.getX() - 10f, hb.getY() + hb.getHeight() + 16f);
            }
        }
        batch.end();

        // ==========================================
        // RENDER HUD & UI (Using Fixed UI Camera)
        // ==========================================
        renderHUD();
    }

    private void renderHUD() {
        PlayerAttributes attr = player.getAttributes();
        float barX = 20f;
        float barY = Gdx.graphics.getHeight() - 40f;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // HP Bar
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(barX, barY, 200, 16);
        shapeRenderer.setColor(Color.FIREBRICK);
        shapeRenderer.rect(barX, barY, 200 * ((float) attr.getCurrentHP() / attr.getMaxHP()), 16);

        // Stamina Bar
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(barX, barY - 20, 200, 12);
        shapeRenderer.setColor(Color.FOREST);
        shapeRenderer.rect(barX, barY - 20, 200 * (attr.getCurrentStamina() / attr.getMaxStamina()), 12);

        // Healing Flasks (Max 3)
        float flaskX = barX;
        float flaskY = barY - 38;
        for (int i = 0; i < attr.getMaxHealCharges(); i++) {
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.rect(flaskX + (i * 22), flaskY, 18, 12);
            if (i < attr.getHealCharges()) {
                shapeRenderer.setColor(Color.LIME);
                shapeRenderer.rect(flaskX + (i * 22) + 2, flaskY + 2, 14, 8);
            }
        }

        // Character Upgrade Modal Overlay if open
        if (upgradeStation != null && upgradeStation.isMenuOpen()) {
            float modalW = 460f;
            float modalH = 210f;
            float modalX = (Gdx.graphics.getWidth() - modalW) / 2f;
            float modalY = (Gdx.graphics.getHeight() - modalH) / 2f;

            shapeRenderer.setColor(new Color(0.06f, 0.08f, 0.12f, 0.95f));
            shapeRenderer.rect(modalX, modalY, modalW, modalH);

            shapeRenderer.setColor(Color.FOREST);
            shapeRenderer.rect(modalX, modalY + modalH - 6, modalW, 6);
        }
        shapeRenderer.end();

        // Render UI Text
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "HP: " + attr.getCurrentHP() + "/" + attr.getMaxHP(), barX + 5, barY + 13);
        font.draw(batch, "SP: " + (int) attr.getCurrentStamina() + "/" + (int) attr.getMaxStamina(), barX + 5, barY - 10);
        font.draw(batch, "Flasks: " + attr.getHealCharges() + "/" + attr.getMaxHealCharges() + " [E/H: Heal]", flaskX + 74, flaskY + 11);

        // Level, XP & Attribute Points
        font.setColor(Color.GOLD);
        font.draw(batch, "LVL " + attr.getLevel() + " | XP: " + attr.getCurrentXP() + "/" + attr.getNextLevelXP() +
            " | Unspent Points: " + attr.getAttributePoints(), barX, barY - 48);
        font.setColor(Color.ORANGE);
        String account = AuthSession.getInstance().isLoggedIn()
                ? AuthSession.getInstance().getUsername()
                : "guest (F6 login / F7 register)";
        font.draw(batch, "Score: " + ScoreTracker.getInstance().getCurrentScore()
                + " | " + account + " | F8 leaderboard", barX, barY - 30);

        // State, Weapon & Active Combo Sequence
        font.setColor(Color.WHITE);
        font.draw(batch, "State: " + player.getStateMachine().getCurrentStateName(), barX, barY - 68);
        font.draw(batch, "Weapon: " + player.getAttackStrategy().getWeaponName(), barX, barY - 86);
        font.setColor(Color.CYAN);
        font.draw(batch, "Combo: [" + player.getComboManager().getSequenceString() + "]", barX, barY - 104);

        // Region Progression Status
        GameManager gm = GameManager.getInstance();
        float rightX = Gdx.graphics.getWidth() - 360f;
        font.setColor(Color.WHITE);
        font.draw(batch, "REGION: " + gm.getCurrentRegion().getDisplayName(), rightX, barY);
        font.draw(batch, "[F1] Temple (Monk Dash): " + (gm.isRegionCompleted(GameManager.Region.FORGOTTEN_TEMPLE) ? "CLEARED" : "LOCKED"), rightX, barY - 20);
        font.draw(batch, "[F2] Foundry (Seismic Slam): " + (gm.isRegionCompleted(GameManager.Region.ASHEN_FOUNDRY) ? "CLEARED" : "LOCKED"), rightX, barY - 40);
        font.draw(batch, "[F3] Spire (Glacial Orb): " + (gm.isRegionCompleted(GameManager.Region.OVERGROWN_SPIRE) ? "CLEARED" : "LOCKED"), rightX, barY - 60);
        font.draw(batch, "[F4] Final Region: " + (gm.isFinalRegionUnlocked() ? "PORTAL UNLOCKED" : "SEALED (Need 3 regions)"), rightX, barY - 80);

        // Upgrade Modal Content
        if (upgradeStation != null && upgradeStation.isMenuOpen()) {
            float modalW = 460f;
            float modalH = 210f;
            float modalX = (Gdx.graphics.getWidth() - modalW) / 2f;
            float modalY = (Gdx.graphics.getHeight() - modalH) / 2f;

            font.setColor(Color.GREEN);
            font.draw(batch, "=== CHARACTER UPGRADE ALTAR ===", modalX + 90f, modalY + modalH - 20f);
            font.setColor(Color.GOLD);
            font.draw(batch, "Available Attribute Points: " + attr.getAttributePoints() + " | Level: " + attr.getLevel(), modalX + 30f, modalY + modalH - 45f);

            font.setColor(Color.WHITE);
            font.draw(batch, "[1] Vitality:  " + attr.getVitality() + " -> Increases Max HP (" + attr.getMaxHP() + ")", modalX + 30f, modalY + modalH - 75f);
            font.draw(batch, "[2] Strength:  " + attr.getStrength() + " -> Primary Phys Dmg (" + String.format("%.0f%%", attr.getPhysicalDamageScaling() * 100) + ")", modalX + 30f, modalY + modalH - 95f);
            font.draw(batch, "[3] Endurance: " + attr.getEndurance() + " -> Max Stamina (" + (int) attr.getMaxStamina() + ")", modalX + 30f, modalY + modalH - 115f);
            font.draw(batch, "[4] Dexterity: " + attr.getDexterity() + " -> Reduces Recovery (" + String.format("%.0f%% speed", attr.getRecoverySpeedMultiplier() * 100) + ")", modalX + 30f, modalY + modalH - 135f);

            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Press [1 - 4] to Allocate Points | [F / ESC] Close Altar", modalX + 40f, modalY + 30f);
        }

        // Controls Guide
        float botY = groundY - 15f;
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "A/D: Move | W/UP: JUMP | S/DOWN: Drop Platform | SPACE: Dash | F/ENTER: Interact", 20, botY);
        font.draw(batch, "J: Light | K: Heavy (Combos: LLL, HH, LLH + finisher recovery) | E/H: Heal | U: Slam | I: Orb | T: Hub | F12: Debug", 20, botY - 20);

        // Combat Logs
        font.setColor(Color.YELLOW);
        float logY = groundY + 140f;
        for (int i = 0; i < recentLogs.size(); i++) {
            font.draw(batch, recentLogs.get(i), 20, logY - (i * 18));
        }

        batch.end();
    }

    private void handlePlayerDeath() {
        submitRunScore("Run ended (death)");
        player.getAttributes().notifyCombatLog("You have perished! Respawning at last activated checkpoint...");
        if (lastActivatedCheckpoint != null) {
            setRegion(lastActivatedCheckpoint.getRegion());
            player.respawn(lastActivatedCheckpoint.getRespawnX(), lastActivatedCheckpoint.getRespawnY());
        } else {
            setRegion(GameManager.Region.TUTORIAL_HUB);
            player.respawn(hubSpawnX, hubSpawnY);
        }
        snapCameraToPlayer();
    }

    /** Submits the current run score asynchronously if logged in, then resets local score. */
    private void submitRunScore(String reason) {
        long score = ScoreTracker.getInstance().takeScoreForSubmit();
        if (score <= 0) {
            return;
        }
        if (!AuthSession.getInstance().isLoggedIn()) {
            player.getAttributes().notifyCombatLog(reason + ": score " + score + " (login with F6 to upload)");
            return;
        }
        player.getAttributes().notifyCombatLog(reason + ": uploading score " + score + "...");
        ApiClient.getInstance().submitScore(score, (ok, body, error) -> {
            if (ok) {
                player.getAttributes().notifyCombatLog("Score " + score + " saved to leaderboard.");
            } else {
                player.getAttributes().notifyCombatLog("Score upload failed: " + error);
            }
        });
    }

    private void handleDebugKeys() {
        if (player.getStateMachine().getCurrentState() instanceof ComboRecoveryState) return;
        GameManager gm = GameManager.getInstance();

        // Attribute spending when upgrade station menu is open
        if (upgradeStation != null && upgradeStation.isMenuOpen()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                if (upgradeStation.upgrade(player, AttributeType.VITALITY)) SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                if (upgradeStation.upgrade(player, AttributeType.STRENGTH)) SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                if (upgradeStation.upgrade(player, AttributeType.ENDURANCE)) SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                if (upgradeStation.upgrade(player, AttributeType.DEXTERITY)) SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                upgradeStation.closeMenu();
            }
        }

        // Fast return to Hub anytime
        if (Gdx.input.isKeyJustPressed(Input.Keys.T) || Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            setRegion(GameManager.Region.TUTORIAL_HUB);
        }

        // Quick Manual Save
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            SaveManager.getInstance().saveGame(player, lastActivatedCheckpoint);
            player.getAttributes().notifyCombatLog("Progression saved to this device's local preferences!");
        }

        // Region cheats for fast testing
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            gm.completeRegion(GameManager.Region.FORGOTTEN_TEMPLE);
            gm.unlockAbility(GameManager.Ability.MONK_DASH);
            player.getAttributes().notifyCombatLog("Cleared Forgotten Temple! Unlocked Monk Dash.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            gm.completeRegion(GameManager.Region.ASHEN_FOUNDRY);
            gm.unlockAbility(GameManager.Ability.SEISMIC_SLAM);
            player.getAttributes().notifyCombatLog("Cleared Ashen Foundry! Unlocked Seismic Slam.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            gm.completeRegion(GameManager.Region.OVERGROWN_SPIRE);
            gm.unlockAbility(GameManager.Ability.GLACIAL_ORB);
            player.getAttributes().notifyCombatLog("Cleared Overgrown Spire! Unlocked Glacial Orb.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            if (gm.isFinalRegionUnlocked()) {
                setRegion(GameManager.Region.HOLLOW_ALTARS);
            } else {
                player.getAttributes().notifyCombatLog("The Hollow Altars remain sealed! Clear all 3 regions first.");
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            setRegion(gm.getCurrentRegion());
            player.getAttributes().refillHealCharges();
            player.getAttributes().notifyCombatLog("Respawned region entities & refilled healing flasks (3/3)!");
        }

        // Online: F6 login, F7 register, F8 leaderboard, F9 submit score now
        // Default credentials for local testing — change via Preferences later if needed.
        final String demoUser = "player1";
        final String demoPass = "password";
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            player.getAttributes().notifyCombatLog("Logging in as " + demoUser + "...");
            ApiClient.getInstance().login(demoUser, demoPass, (ok, body, error) -> {
                if (ok) {
                    player.getAttributes().notifyCombatLog("Logged in as " + AuthSession.getInstance().getUsername());
                } else {
                    player.getAttributes().notifyCombatLog("Login failed: " + error);
                }
            });
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            player.getAttributes().notifyCombatLog("Registering " + demoUser + "...");
            ApiClient.getInstance().register(demoUser, demoPass, (ok, body, error) -> {
                if (ok) {
                    player.getAttributes().notifyCombatLog("Registered & logged in as " + AuthSession.getInstance().getUsername());
                } else {
                    player.getAttributes().notifyCombatLog("Register failed: " + error);
                }
            });
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
            ApiClient.getInstance().leaderboard((ok, body, error) -> {
                if (!ok) {
                    player.getAttributes().notifyCombatLog("Leaderboard failed: " + error);
                    return;
                }
                player.getAttributes().notifyCombatLog("=== LEADERBOARD ===");
                try {
                    com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(body);
                    int shown = 0;
                    for (com.badlogic.gdx.utils.JsonValue entry = root.child; entry != null && shown < 5; entry = entry.next) {
                        shown++;
                        player.getAttributes().notifyCombatLog(
                                shown + ". " + entry.getString("username") + " — " + entry.getLong("score"));
                    }
                    if (shown == 0) {
                        player.getAttributes().notifyCombatLog("(empty)");
                    }
                } catch (Exception e) {
                    player.getAttributes().notifyCombatLog("Could not parse leaderboard");
                }
            });
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            submitRunScore("Manual submit");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            debugRender = !debugRender;
            player.getAttributes().notifyCombatLog("Debug overlay: " + (debugRender ? "ON" : "OFF"));
        }
    }

    @Override
    public void dispose() {
        if (hubMapManager != null) {
            hubMapManager.dispose();
        }
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
