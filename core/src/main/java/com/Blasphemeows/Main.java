package com.Blasphemeows;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.Blasphemeows.observer.CombatLogObserver;
import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.RootedState;
import com.Blasphemeows.world.Platform;
import com.Blasphemeows.world.RegionPortal;
import com.Blasphemeows.world.UpgradeStation;
import com.Blasphemeows.world.WeaponStation;

import java.util.ArrayList;
import java.util.List;

/**
 * BLASPHEMEOWS — Main Game Runner
 * Full integration of:
 * 1. Checkpoint & Persistent Save/Load System (SaveManager)
 * 2. Combo Combat System (L->L->L, H->H, L->L->H with buffering & frame-specific damage)
 * 3. Main Hub with Multi-Elevation Platforms, Target Practice (Passive/Aggressive),
 *    Upgrade Station, Central Checkpoint, and 3 Weapon Stations
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

    // World entities
    private final List<Enemy> activeEnemies = new ArrayList<Enemy>();
    private final List<Projectile> activeWorldProjectiles = new ArrayList<Projectile>();
    private final List<RegionPortal> activePortals = new ArrayList<RegionPortal>();
    private final List<Platform> activePlatforms = new ArrayList<Platform>();
    private final List<Checkpoint> activeCheckpoints = new ArrayList<Checkpoint>();
    private final List<WeaponStation> activeWeaponStations = new ArrayList<WeaponStation>();
    private UpgradeStation upgradeStation;
    private TrainingDummy hubTrainingDummy;

    private Checkpoint lastActivatedCheckpoint;
    private final List<String> recentLogs = new ArrayList<String>();
    private final float groundY = 80f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

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

        // 4. Load saved game if exists, otherwise boot into Main Hub
        boolean loaded = SaveManager.getInstance().loadGame(player);
        if (loaded) {
            SaveData save = SaveManager.getInstance().getCurrentSaveData();
            try {
                GameManager.Region savedRegion = GameManager.Region.valueOf(save.lastCheckpointRegion);
                setRegion(savedRegion);
                player.setX(save.respawnX);
                player.setY(save.respawnY);
            } catch (Exception e) {
                setRegion(GameManager.Region.TUTORIAL_HUB);
            }
        } else {
            setRegion(GameManager.Region.TUTORIAL_HUB);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        worldCamera.setToOrtho(false, width, height);
        uiCamera.setToOrtho(false, width, height);
    }

    public void setRegion(GameManager.Region region) {
        GameManager.getInstance().setCurrentRegion(region);
        activePortals.clear();
        activeEnemies.clear();
        activeWorldProjectiles.clear();
        activePlatforms.clear();
        activeCheckpoints.clear();
        activeWeaponStations.clear();
        upgradeStation = null;
        hubTrainingDummy = null;

        if (region == GameManager.Region.TUTORIAL_HUB) {
            setupMainHubEnvironment();
        } else {
            setupCombatRegion(region);
        }

        player.getAttributes().notifyCombatLog("Entered: " + region.getDisplayName());
    }

    /**
     * Recreates the EXACT layout from the reference drawing for the Main Sanctuary Hub:
     * - Outer Building Frame: Solid black ceiling and side walls with doorway/balcony openings.
     * - Green Element: Character Upgrade Altar on the floor at bottom left.
     * - Left Step Platforms: Two elevated steps on the left for climbing upwards.
     * - Red Element: Massive tall Central Checkpoint Monolith right in the center floor.
     * - Three Blue Elements: Weapon Selection Stations side-by-side on the floor.
     * - Upper Long Platform: Spanning across the right side above the checkpoint/weapon racks.
     * - Yellow Element: Target Practice Dummy standing on the far right end of the upper platform.
     *
     * Portals and Outdoor Parkour Paths:
     * - Top Left: Pagoda mountain parkour leading to The Forgotten Temple (high altar).
     * - Left Side: Sunken mossy root parkour leading to Overgrown Spire.
     * - Bottom Right: Smelter iron girder walkway leading to Ashen Foundry.
     * - Upper Right: Cathedral flying buttress parkour leading to The Hollow Altars of the Crooked Saints.
     */
    private void setupMainHubEnvironment() {
        currentEnemyFactory = new TutorialHubEnemyFactory();

        // 1. Central Checkpoint (Red element) - Made significantly bigger to match reference drawing
        Checkpoint centralCheckpoint = new Checkpoint("hub_central", "Grand Sanctuary Monolith",
                GameManager.Region.TUTORIAL_HUB, -40f, groundY, 80f, 195f);
        centralCheckpoint.setActivated(true);
        activeCheckpoints.add(centralCheckpoint);
        lastActivatedCheckpoint = centralCheckpoint;

        // 2. Character Upgrade Altar (Green element) - Bottom left floor
        upgradeStation = new UpgradeStation(-350f, groundY, 120f, 50f);

        // 3. Three Weapon Selection Stations (Three Blue elements) - Floor to the right of Checkpoint
        activeWeaponStations.add(new WeaponStation(130f, groundY, 32f, 56f, new SwordAttackStrategy()));
        activeWeaponStations.add(new WeaponStation(195f, groundY, 32f, 56f, new GreatswordAttackStrategy()));
        activeWeaponStations.add(new WeaponStation(260f, groundY, 32f, 56f, new SpearAttackStrategy()));

        // 4. Sanctuary Platforms inside the building (Black elements matching drawing)
        // Lower left step above/right of green altar
        activePlatforms.add(new Platform(-220f, 175f, 110f, 16f, new Color(0.12f, 0.12f, 0.14f, 1f)));
        // Higher left step
        activePlatforms.add(new Platform(-360f, 265f, 110f, 16f, new Color(0.12f, 0.12f, 0.14f, 1f)));
        // Long upper platform spanning from center across the right side
        activePlatforms.add(new Platform(-50f, 320f, 430f, 16f, new Color(0.12f, 0.12f, 0.14f, 1f)));

        // 5. Target Practice Station (Yellow element) - Resting on the right end of the long upper platform
        hubTrainingDummy = new TrainingDummy(310f, 320f + 16f);
        activeEnemies.add(hubTrainingDummy);

        // =========================================================================
        // SPREAD-OUT OUTDOOR PATHS & PARKOUR TOWARDS THE 4 REGIONAL PORTALS
        // =========================================================================

        // --- PATH 1 (Top Left): Mountain Pagoda Parkour -> The Forgotten Temple ---
        activePlatforms.add(new Platform(-540f, 330f, 85f, 16f, new Color(0.16f, 0.20f, 0.22f, 1f)));
        activePlatforms.add(new Platform(-690f, 400f, 80f, 16f, new Color(0.16f, 0.20f, 0.22f, 1f)));
        activePlatforms.add(new Platform(-840f, 470f, 85f, 16f, new Color(0.16f, 0.20f, 0.22f, 1f)));
        activePlatforms.add(new Platform(-1070f, 530f, 180f, 20f, new Color(0.18f, 0.24f, 0.26f, 1f))); // High Pagoda Dais
        activePortals.add(new RegionPortal(-1020f, 550f, 56f, 88f,
                GameManager.Region.FORGOTTEN_TEMPLE, Color.TEAL, "Forgotten Temple (Top-Left)"));

        // --- PATH 2 (Left Side): Sunken Botanical Root Parkour -> Overgrown Spire ---
        activePlatforms.add(new Platform(-540f, 120f, 100f, 16f, new Color(0.14f, 0.22f, 0.16f, 1f)));
        activePlatforms.add(new Platform(-700f, 160f, 90f, 16f, new Color(0.14f, 0.22f, 0.16f, 1f)));
        activePlatforms.add(new Platform(-860f, 200f, 100f, 16f, new Color(0.14f, 0.22f, 0.16f, 1f)));
        activePlatforms.add(new Platform(-1080f, 230f, 180f, 20f, new Color(0.15f, 0.25f, 0.18f, 1f))); // Spire Dais
        activePortals.add(new RegionPortal(-1030f, 250f, 56f, 88f,
                GameManager.Region.OVERGROWN_SPIRE, Color.CHARTREUSE, "Overgrown Spire (Left)"));

        // --- PATH 3 (Bottom Right): Molten Smelter Girder Path -> Ashen Foundry ---
        activePlatforms.add(new Platform(520f, 95f, 100f, 16f, new Color(0.24f, 0.16f, 0.12f, 1f)));
        activePlatforms.add(new Platform(680f, 120f, 110f, 16f, new Color(0.24f, 0.16f, 0.12f, 1f)));
        activePlatforms.add(new Platform(840f, 95f, 110f, 16f, new Color(0.24f, 0.16f, 0.12f, 1f)));
        activePlatforms.add(new Platform(1020f, 80f, 190f, 20f, new Color(0.26f, 0.18f, 0.14f, 1f))); // Foundry Dais
        activePortals.add(new RegionPortal(1080f, 80f, 56f, 88f,
                GameManager.Region.ASHEN_FOUNDRY, Color.ORANGE, "Ashen Foundry (Bottom-Right)"));

        // --- PATH 4 (Upper / Far Right): Grand Gothic Buttress Parkour -> The Hollow Altars ---
        activePlatforms.add(new Platform(530f, 350f, 90f, 16f, new Color(0.22f, 0.15f, 0.24f, 1f)));
        activePlatforms.add(new Platform(690f, 420f, 85f, 16f, new Color(0.22f, 0.15f, 0.24f, 1f)));
        activePlatforms.add(new Platform(850f, 480f, 85f, 16f, new Color(0.22f, 0.15f, 0.24f, 1f)));
        activePlatforms.add(new Platform(1040f, 540f, 210f, 20f, new Color(0.24f, 0.16f, 0.26f, 1f))); // Cathedral Dais
        activePortals.add(new RegionPortal(1110f, 560f, 62f, 98f,
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
        activeCheckpoints.add(regionCheckpoint);
        lastActivatedCheckpoint = regionCheckpoint;

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
                cp.activate(player);
                lastActivatedCheckpoint = cp;
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
            player.getAttributes().notifyCombatLog("🎯 Target Practice Mode: " +
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
                    player.getAttributes().notifyCombatLog("⛔ " + portal.getLabel() + " is sealed! Clear all 3 branch regions first.");
                    return;
                }
                setRegion(portal.getDestination());
                return;
            }
        }
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

        // 1. Update Player Input & Movement/Jump
        float prevY = player.getY();
        inputHandler.handleInput(player);
        player.update(delta);

        // Platform collisions (resolves landing on elevated walkways)
        for (Platform platform : activePlatforms) {
            platform.resolveCollision(player, prevY);
        }

        // 2. Camera Smoothly Follows the Player (Lerp)
        float targetCamX = player.getX() + player.getWidth() / 2f;
        float targetCamY = Math.max(groundY + (Gdx.graphics.getHeight() / 2f) - 60f, player.getY() + 30f);
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

            // Enemy death & XP distribution
            if (!enemy.isAlive()) {
                int xpEarned = enemy.getXpReward();
                player.getAttributes().addXP(xpEarned);
                player.getAttributes().notifyCombatLog("☠️ Defeated " + enemy.getName() + "! (+" + xpEarned + " XP)");
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
        GameManager.Region cur = GameManager.getInstance().getCurrentRegion();
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
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Ground platform
        shapeRenderer.setColor(0.18f, 0.18f, 0.20f, 1f);
        shapeRenderer.rect(-10000, 0, 20000, groundY);

        // Floor markers
        shapeRenderer.setColor(0.25f, 0.25f, 0.28f, 1f);
        for (int gx = -4000; gx <= 4000; gx += 200) {
            shapeRenderer.rect(gx, groundY - 10, 4, 10);
        }

        // Render Sanctuary Building Frame when in Main Hub (matching reference drawing)
        if (cur == GameManager.Region.TUTORIAL_HUB) {
            shapeRenderer.setColor(0.06f, 0.06f, 0.08f, 1f); // Solid architectural black
            // Roof / Ceiling across the top
            shapeRenderer.rect(-410f, 520f, 830f, 20f);
            // Left Wall (drops down from ceiling with high terrace & ground doorway openings)
            shapeRenderer.rect(-410f, 370f, 18f, 150f);
            shapeRenderer.rect(-410f, 160f, 18f, 105f);
            // Right Wall (drops down from ceiling with upper balcony & ground doorway openings)
            shapeRenderer.rect(400f, 430f, 18f, 90f);
            shapeRenderer.rect(400f, 160f, 18f, 160f);
        }

        // Render Platforms (Black elements for verticality)
        for (Platform platform : activePlatforms) {
            shapeRenderer.setColor(platform.getColor());
            shapeRenderer.rect(platform.getX(), platform.getY(), platform.getWidth(), platform.getHeight());
            // Platform top rim
            shapeRenderer.setColor(0.35f, 0.35f, 0.40f, 1f);
            shapeRenderer.rect(platform.getX(), platform.getY() + platform.getHeight() - 3, platform.getWidth(), 3);
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

        // Render Player
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

        // Render Projectiles
        for (Projectile proj : activeWorldProjectiles) {
            shapeRenderer.setColor(proj.getType().getColor());
            if (proj.getType() == Projectile.Type.GLACIAL_ORB || proj.getType() == Projectile.Type.FIREBALL) {
                shapeRenderer.circle(proj.getX() + proj.getWidth() / 2f, proj.getY() + proj.getHeight() / 2f, proj.getWidth() / 2f);
            } else {
                shapeRenderer.rect(proj.getX(), proj.getY(), proj.getWidth(), proj.getHeight());
            }
        }

        // Render Enemies / Target Practice Dummy (Yellow element on Upper Platform in Hub)
        for (Enemy enemy : activeEnemies) {
            if (enemy.isFrozen()) {
                shapeRenderer.setColor(Color.SKY);
            } else if (enemy.isStunnedOrFrozen()) {
                shapeRenderer.setColor(Color.YELLOW);
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

            // Health bar
            float barW = enemy.getWidth();
            float hpPct = (float) enemy.getHp() / enemy.getMaxHp();
            shapeRenderer.setColor(Color.BLACK);
            shapeRenderer.rect(enemy.getX(), enemy.getY() + enemy.getHeight() + 6, barW, 5);
            shapeRenderer.setColor(enemy instanceof TrainingDummy ? Color.GREEN : Color.RED);
            shapeRenderer.rect(enemy.getX(), enemy.getY() + enemy.getHeight() + 6, barW * hpPct, 5);
        }

        // ==========================================
        // ALWAYS-VISIBLE DEBUG HITBOX VISUALIZATION
        // (Player Attack Hitboxes & Enemy Attack Hitboxes)
        // ==========================================
        // 1. Player attack hitboxes
        for (Hitbox hb : player.getActiveHitboxes()) {
            if (!hb.isFinished()) {
                shapeRenderer.setColor(hb.getDebugColor());
                shapeRenderer.rect(hb.getX(), hb.getY(), hb.getWidth(), hb.getHeight());
            }
        }

        // 2. Enemy attack hitboxes (Telegraph Startup = Yellow, Active = Red, Recovery = Cyan)
        for (Enemy enemy : activeEnemies) {
            Hitbox ehb = enemy.getCurrentAttackHitbox();
            if (ehb != null && !ehb.isFinished()) {
                shapeRenderer.setColor(ehb.getDebugColor());
                shapeRenderer.rect(ehb.getX(), ehb.getY(), ehb.getWidth(), ehb.getHeight());
            }
        }
        shapeRenderer.end();

        // 3. Enemy Body Hurtbox outlines (Line mode)
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
        shapeRenderer.end();

        // Render World Text (Labels & Prompts)
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // Checkpoint prompts
        for (Checkpoint cp : activeCheckpoints) {
            font.setColor(Color.RED);
            font.draw(batch, cp.getName(), cp.getX() - 25f, cp.getY() + cp.getHeight() + 22f);
            if (cp.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                font.draw(batch, "🔥 [F / ENTER] Activate Checkpoint & Save", cp.getX() - 65f, cp.getY() + cp.getHeight() + 40f);
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

        // Weapon Stations prompts
        for (WeaponStation ws : activeWeaponStations) {
            font.setColor(Color.SKY);
            font.draw(batch, ws.getWeaponStrategy().getWeaponName(), ws.getX() - 30f, ws.getY() + ws.getHeight() + 22f);
            if (ws.isPlayerNearby(player.getX(), player.getY())) {
                font.setColor(Color.GOLD);
                font.draw(batch, ws.getPromptText(), ws.getX() - 50f, ws.getY() + ws.getHeight() + 40f);
            }
        }

        // Target Practice prompt
        if (hubTrainingDummy != null) {
            font.setColor(Color.YELLOW);
            String modeStr = hubTrainingDummy.isAggressiveMode() ? "Aggressive Mode" : "Passive Mode";
            font.draw(batch, "Target Practice (" + modeStr + ")", hubTrainingDummy.getX() - 35f, hubTrainingDummy.getY() + hubTrainingDummy.getHeight() + 22f);
            if (Math.abs(player.getX() - hubTrainingDummy.getX()) < 65f && Math.abs(player.getY() - hubTrainingDummy.getY()) < 65f) {
                font.setColor(Color.GOLD);
                font.draw(batch, "[F / ENTER] Toggle Practice Mode", hubTrainingDummy.getX() - 50f, hubTrainingDummy.getY() + hubTrainingDummy.getHeight() + 40f);
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
                    font.draw(batch, "⚠️ [ENEMY ATK: " + ehb.getCurrentPhase() + "]",
                            ehb.getX() - 15f, ehb.getY() + ehb.getHeight() + 16f);
                }
            }
        }

        // Player active attack hitbox phase text overlay
        for (Hitbox hb : player.getActiveHitboxes()) {
            if (!hb.isFinished()) {
                font.setColor(hb.getDebugColor());
                font.draw(batch, "⚔️ [PL ATK: " + hb.getCurrentPhase() + "]",
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
        font.draw(batch, "[F4] Final Region: " + (gm.isFinalRegionUnlocked() ? "★ PORTAL UNLOCKED ★" : "SEALED (Need 3 regions)"), rightX, barY - 80);

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
        font.draw(batch, "J: Light | K: Heavy (Combos: LLL, HH, LLH - Zero Recovery Cancel!) | E/H: Heal | U: Slam | I: Orb | T: Hub", 20, botY - 20);

        // Combat Logs
        font.setColor(Color.YELLOW);
        float logY = groundY + 140f;
        for (int i = 0; i < recentLogs.size(); i++) {
            font.draw(batch, recentLogs.get(i), 20, logY - (i * 18));
        }

        batch.end();
    }

    private void handlePlayerDeath() {
        player.getAttributes().notifyCombatLog("💀 You have perished! Respawning at last activated checkpoint...");
        if (lastActivatedCheckpoint != null) {
            setRegion(lastActivatedCheckpoint.getRegion());
            player.respawn(lastActivatedCheckpoint.getX() + 20f, lastActivatedCheckpoint.getY());
        } else {
            setRegion(GameManager.Region.TUTORIAL_HUB);
            player.respawn(-40f, groundY);
        }
    }

    private void handleDebugKeys() {
        GameManager gm = GameManager.getInstance();

        // Attribute spending when upgrade station menu is open
        if (upgradeStation != null && upgradeStation.isMenuOpen()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                upgradeStation.upgrade(player, AttributeType.VITALITY);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                upgradeStation.upgrade(player, AttributeType.STRENGTH);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                upgradeStation.upgrade(player, AttributeType.ENDURANCE);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                upgradeStation.upgrade(player, AttributeType.DEXTERITY);
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
            player.getAttributes().notifyCombatLog("💾 Progression saved to blasphemeows_save.json!");
        }

        // Region cheats for fast testing
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            gm.completeRegion(GameManager.Region.FORGOTTEN_TEMPLE);
            gm.unlockAbility(GameManager.Ability.MONK_DASH);
            player.getAttributes().notifyCombatLog("✨ Cleared Forgotten Temple! Unlocked Monk Dash.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            gm.completeRegion(GameManager.Region.ASHEN_FOUNDRY);
            gm.unlockAbility(GameManager.Ability.SEISMIC_SLAM);
            player.getAttributes().notifyCombatLog("✨ Cleared Ashen Foundry! Unlocked Seismic Slam.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            gm.completeRegion(GameManager.Region.OVERGROWN_SPIRE);
            gm.unlockAbility(GameManager.Ability.GLACIAL_ORB);
            player.getAttributes().notifyCombatLog("✨ Cleared Overgrown Spire! Unlocked Glacial Orb.");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            if (gm.isFinalRegionUnlocked()) {
                setRegion(GameManager.Region.HOLLOW_ALTARS);
            } else {
                player.getAttributes().notifyCombatLog("⛔ The Hollow Altars remain sealed! Clear all 3 regions first.");
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            setRegion(gm.getCurrentRegion());
            player.getAttributes().refillHealCharges();
            player.getAttributes().notifyCombatLog("Respawned region entities & refilled healing flasks (3/3)!");
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
