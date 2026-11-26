package com.Blasphemeows.command;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.Blasphemeows.combat.GreatswordAttackStrategy;
import com.Blasphemeows.combat.SpearAttackStrategy;
import com.Blasphemeows.combat.SwordAttackStrategy;
import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.RootedState;
import com.Blasphemeows.state.ComboRecoveryState;

/**
 * Translates hardware keyboard inputs into Command objects and dispatches them to the Player.
 */
public class InputHandler {
    private final SwordAttackStrategy sword = new SwordAttackStrategy();
    private final GreatswordAttackStrategy greatsword = new GreatswordAttackStrategy();
    private final SpearAttackStrategy spear = new SpearAttackStrategy();
    private InteractCommand.InteractionHandler interactionHandler;

    public void setInteractionHandler(InteractCommand.InteractionHandler handler) {
        this.interactionHandler = handler;
    }

    public void handleInput(Player player) {
        if (player == null || Gdx.input == null) return;

        if (player.getStateMachine().getCurrentState() instanceof ComboRecoveryState) return;

        boolean isRooted = player.getStateMachine().getCurrentState() instanceof RootedState;

        // QTE Mash when Rooted
        if (isRooted) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                new QTEMashCommand(true).execute(player);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                new QTEMashCommand(false).execute(player);
            }
            return;
        }

        // Interactions (Portals, Sanctuary Altar)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            new InteractCommand(interactionHandler).execute(player);
        }

        // Horizontal movement
        float moveDir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveDir -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveDir += 1f;
        }
        new MoveCommand(moveDir).execute(player);

        // Jump
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            new JumpCommand().execute(player);
        }

        // Drop through platform (S / DOWN)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            player.dropThroughPlatform();
        }

        // Attacks
        if (Gdx.input.isKeyJustPressed(Input.Keys.J) || Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            new LightAttackCommand().execute(player);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.K) || Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            new HeavyAttackCommand().execute(player);
        }

        // Heal (Max 3 charges)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            new HealCommand().execute(player);
        }

        // Dodge / Monk Dash
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) {
            new DodgeOrDashCommand().execute(player);
        }

        // Abilities
        if (Gdx.input.isKeyJustPressed(Input.Keys.U) || Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            new SeismicSlamCommand().execute(player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I) || Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            new GlacialOrbCommand().execute(player);
        }

        // Note: Unrestricted mid-combat weapon switching via 1/2/3 has been removed
        // per design specification. Weapons can now only be equipped by visiting
        // the corresponding Weapon Selection Stations in the Main Hub.
    }
}
