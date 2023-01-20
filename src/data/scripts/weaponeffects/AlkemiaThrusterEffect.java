/*
 * by Frederoo, based on MagicVectorThruster by Tartiflette
 */
package data.scripts.weaponeffects;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.tools.Helpers;

public class AlkemiaThrusterEffect implements EveryFrameWeaponEffectPlugin {

    private static final int ANGLE_FORWARD_MAX = 45;
    private static final int ANGLE_FORWARD_MIN = -45;
    private static final int ANGLE_REVERSE_MAX = ANGLE_FORWARD_MAX + 180;
    private static final int ANGLE_REVERSE_MIN = ANGLE_FORWARD_MIN + 180;

    private enum ThrusterRole {
        FORWARD,
        REVERSE,
        LEFT,
        RIGHT,
        TURN_LEFT,
        TURN_RIGHT
    }

    private boolean setupDone = false, accel = false, turn = false;
    private ShipAPI SHIP;
    private ShipEngineAPI thruster;
    private ShipEngineControllerAPI EMGINES;
    private Map<ThrusterRole, Object> ROLES = new HashMap<>();
    private float time = 0, previousThrust = 0;

    // Smooth thrusting prevents instant changes in directions and levels of thrust,
    // lower is smoother
    private static final float FREQ = 0.05f, SMOOTH_THRUSTING = 0.1f;
    private static final float ENGINE_SEARCH_RANGE = 6;

    private float TURN_RIGHT_ANGLE = 0, THRUST_TO_TURN = 0, NEUTRAL_ANGLE = 180, FRAMES = 0, OFFSET = 0;
    // sprite size, could be scaled with the engine width to allow variable engine
    // length
    private Vector2f size = new Vector2f(8, 74);
    private float MAX_DEFLECTION;
    private float ARC_MAX;
    private float ARC_MIN;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!setupDone) {
            SHIP = weapon.getShip();
            EMGINES = SHIP.getEngineController();
            if (weapon.getAnimation() != null) {
                FRAMES = weapon.getAnimation().getNumFrames();
            }

            // find the ship engine associated with the deco thruster
            for (ShipEngineAPI e : EMGINES.getShipEngines()) {
                // Helpers.addFloatingText(SHIP, "a: " + e.getEngineSlot().getAngle(),
                // e.getLocation());
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), ENGINE_SEARCH_RANGE)) {
                    thruster = e;
                }
            }

            WeaponSlotAPI weaponSlot = weapon.getSlot();
            switch (weaponSlot.getSlotSize()) {
                case LARGE:
                    size = new Vector2f(20, 232);
                    break;
                case MEDIUM:
                    size = new Vector2f(12, 140);
                    break;
                default:
                    size = new Vector2f(8, 74);
                    break;
            }

            // desync the engines wobble
            // OFFSET = (float) (Math.random() * MathUtils.FPI);

            // "rest" angle when not in use
            NEUTRAL_ANGLE = weaponSlot.getAngle();

            MAX_DEFLECTION = weapon.getArc() / 2;
            ARC_MAX = NEUTRAL_ANGLE + MAX_DEFLECTION;
            ARC_MIN = NEUTRAL_ANGLE - MAX_DEFLECTION;

            ROLES = analyzeRoles(weaponSlot);

            // ideal aim angle to rotate the ship (allows free-form placement on the hull)
            TURN_RIGHT_ANGLE = MathUtils.clampAngle(VectorUtils.getAngle(SHIP.getLocation(), weapon.getLocation()));
            // is the thruster performant at turning the ship? Engines closer to the center
            // of mass will concentrate more on dealing with changes of velocity.
            THRUST_TO_TURN = smooth(
                    MathUtils.getDistance(SHIP.getLocation(), weapon.getLocation()) / SHIP.getCollisionRadius());

            setupDone = true;
        }

        if (engine.isPaused() || SHIP.getOriginalOwner() == -1) {
            return;
        }

        if (!SHIP.isAlive() || (thruster != null && thruster.isDisabled())) {
            if (weapon.getAnimation() == null) {
                return;
            }
            weapon.getAnimation().setFrame(0);
            previousThrust = 0;
            return;
        }

        // 20FPS
        time += amount;
        if (time >= FREQ) {
            time = 0;

            // check what the ship is doing
            float accelerateAngle = NEUTRAL_ANGLE;
            float turnAngle = NEUTRAL_ANGLE;
            float thrust = 0;

            if (EMGINES.isAccelerating()) {
                accelerateAngle = 180;
                thrust = 1.5f;
                accel = true;
            } else if (EMGINES.isAcceleratingBackwards()) {
                accelerateAngle = 0;
                thrust = 1.5f;
                accel = true;
            } else if (EMGINES.isDecelerating()) {
                accelerateAngle = NEUTRAL_ANGLE;
                thrust = 0.5f;
                accel = true;
            } else {
                accel = false;
            }

            if (EMGINES.isStrafingLeft()) {
                if (thrust == 0) {
                    accelerateAngle = -90;
                } else {
                    accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, -90) / 2 + accelerateAngle;
                }
                thrust = Math.max(1, thrust);
                accel = true;
            } else if (EMGINES.isStrafingRight()) {
                if (thrust == 0) {
                    accelerateAngle = 90;
                } else {
                    accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, 90) / 2 + accelerateAngle;
                }
                thrust = Math.max(1, thrust);
                accel = true;
            }

            if (EMGINES.isTurningRight()) {
                turnAngle = TURN_RIGHT_ANGLE;
                thrust = Math.max(1, thrust);
                turn = true;
            } else if (EMGINES.isTurningLeft()) {
                turnAngle = MathUtils.clampAngle(180 + TURN_RIGHT_ANGLE);
                thrust = Math.max(1, thrust);
                turn = true;
            } else {
                turn = false;
            }

            // calculate the corresponding vector thrusting
            if (thrust > 0) {

                // DEBUG
                Vector2f offset = new Vector2f(weapon.getLocation().x - SHIP.getLocation().x,
                        weapon.getLocation().y - SHIP.getLocation().y);
                VectorUtils.rotate(offset, -SHIP.getFacing(), offset);

                if (!turn && FRAMES != 0) {
                    // thrust only, easy.
                    thrust(weapon, accelerateAngle,
                            thrust * (SHIP.getMutableStats().getAcceleration().computeMultMod()), SMOOTH_THRUSTING);
                } else {
                    if (!accel && FRAMES != 0) {
                        // turn only, easy too.
                        thrust(weapon, turnAngle,
                                thrust * (SHIP.getMutableStats().getTurnAcceleration().computeMultMod()),
                                SMOOTH_THRUSTING);
                    } else {
                        // combined turn and thrust, aka the funky part.

                        // aim-to-mouse clamp, helps to avoid flickering when the ship is almost facing
                        // the cursor and not turning much.
                        float clampedThrustToTurn = THRUST_TO_TURN
                                * Math.min(1, Math.abs(SHIP.getAngularVelocity()) / 10);
                        clampedThrustToTurn = smooth(clampedThrustToTurn);

                        // start from the neutral angle
                        float combinedAngle = NEUTRAL_ANGLE;

                        // adds both thrust and turn angle at their respective thrust-to-turn ratio.
                        // Gives a "middleground" angle
                        combinedAngle = MathUtils.clampAngle(
                                combinedAngle + MathUtils.getShortestRotation(NEUTRAL_ANGLE, accelerateAngle));
                        combinedAngle = MathUtils.clampAngle(combinedAngle
                                + clampedThrustToTurn * MathUtils.getShortestRotation(accelerateAngle, turnAngle));

                        // get the total thrust with mults
                        float combinedThrust = thrust;
                        combinedThrust *= (SHIP.getMutableStats().getTurnAcceleration().computeMultMod()
                                + SHIP.getMutableStats().getAcceleration().computeMultMod()) / 2;

                        // calculate how much appart the turn and thrust angle are
                        // bellow 90 degrees, the engine is kept at full thrust
                        // if they are further appart, the engine is less useful and it's output get
                        // reduced
                        float offAxis = Math.abs(MathUtils.getShortestRotation(turnAngle, accelerateAngle));
                        offAxis = Math.max(0, offAxis - 90);
                        offAxis /= 45;

                        combinedThrust *= 1 - Math.max(0, Math.min(1, offAxis));

                        // combined thrust is finicky, thus twice smoother
                        if (FRAMES == 0) {
                            // non animated weapons like covers are just oriented
                            rotate(weapon, combinedAngle, combinedThrust, SMOOTH_THRUSTING / 2);
                        } else {
                            thrust(weapon, combinedAngle, combinedThrust, SMOOTH_THRUSTING / 2);
                        }
                    }
                }
                // DEBUG
                // MagicRender.objectspace(
                // Global.getSettings().getSprite("fx", "bar"),
                // SHIP,
                // offset,
                // new Vector2f(),
                // new Vector2f(16, 16),
                // new Vector2f(),
                // turnAngle,
                // 0,
                // true,
                // Color.red,
                // true,
                // 0,
                // 0,
                // 0.1f,
                // false);
                // MagicRender.objectspace(
                // Global.getSettings().getSprite("fx", "bar"),
                // SHIP,
                // offset,
                // new Vector2f(),
                // new Vector2f(16, 16),
                // new Vector2f(),
                // accelerateAngle,
                // 0,
                // true,
                // Color.green,
                // true,
                // 0,
                // 0,
                // 0.1f,
                // false);
                // DEBUG

            } else {
                if (FRAMES == 0) {
                    // non animated weapons like covers are just oriented
                    rotate(weapon, NEUTRAL_ANGLE, 0, SMOOTH_THRUSTING);
                } else {
                    thrust(weapon, NEUTRAL_ANGLE, 0, SMOOTH_THRUSTING);
                }
            }
        }
    }

    private Map<ThrusterRole, Object> analyzeRoles(WeaponSlotAPI slot) {
        Map<ThrusterRole, Object> roles = new HashMap<>();

        Vector2f location = slot.getLocation();
        
        // \?/
        // / \
        if ((ARC_MAX >= ANGLE_FORWARD_MAX) || (ARC_MIN <= ANGLE_FORWARD_MIN)) {
            roles.put(ThrusterRole.FORWARD, new Object());
        }

        // \ /
        // /?\
        if ((ARC_MAX >= ANGLE_REVERSE_MAX) || (ARC_MIN <= ANGLE_REVERSE_MIN)) {
            roles.put(ThrusterRole.REVERSE, new Object());
        }

        // ?\ /
        // ?/ \
        if ((ARC_MAX >= ANGLE_FORWARD_MAX) || (ARC_MIN <= ANGLE_FORWARD_MIN)) {
            roles.put(ThrusterRole.FORWARD, new Object());
        }

        //  \ /?
        //  / \?
        if ((ARC_MAX >= ANGLE_FORWARD_MAX) || (ARC_MIN <= ANGLE_FORWARD_MIN)) {
            roles.put(ThrusterRole.FORWARD, new Object());
        }

        return roles;
    }

    /**
     * @param weapon
     * @param angle
     * @param thrust
     * @param smooth
     * @return float, aim angle
     */
    private float rotate(WeaponAPI weapon, float angle, float thrust, float smooth) {
        // target angle
        float aim = angle + SHIP.getFacing();

        // how far from the target angle the engine is aimed at
        aim = MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);

        // engine wooble
        aim += 5 * FastTrig.cos(SHIP.getFullTimeDeployed() * 5 * thrust + OFFSET);
        aim *= smooth;
        float weaponAngle = weapon.getCurrAngle() + aim;

        float upperLimit = NEUTRAL_ANGLE + MAX_DEFLECTION + SHIP.getFacing();
        float lowerLimit = NEUTRAL_ANGLE - MAX_DEFLECTION + SHIP.getFacing();
        if (weaponAngle > upperLimit) {
            weaponAngle = upperLimit;
        } else if (weaponAngle < lowerLimit) {
            weaponAngle = lowerLimit;
        }

        weapon.setCurrAngle(MathUtils.clampAngle(weaponAngle));

        if (thruster != null) {
            EngineSlotAPI engine = thruster.getEngineSlot();
            engine.setAngle(weaponAngle);

            Helpers.addFloatingText(SHIP, String.format("a:%.2f(%.2f, %.2f)", weaponAngle, lowerLimit, upperLimit),
                    Vector2f.add(new Vector2f(0, 50), weapon.getLocation(), null), Misc.getMissileMountColor());
        }

        return aim;
    }

    private void thrust(WeaponAPI weapon, float angle, float thrust, float smooth) {

        // random sprite
        int frame = (int) (Math.random() * (FRAMES - 1)) + 1;
        if (frame == weapon.getAnimation().getNumFrames()) {
            frame = 1;
        }
        weapon.getAnimation().setFrame(frame);
        SpriteAPI sprite = weapon.getSprite();

        float length = thrust;

        // how far from the target angle the engine is aimed at
        float aim = rotate(weapon, angle, thrust, smooth);

        // thrust is reduced while the engine isn't facing the target angle, then
        // smoothed
        length *= Math.max(0, 1 - (Math.abs(aim) / 90));
        length -= previousThrust;
        length *= smooth;
        length += previousThrust;
        previousThrust = length;

        // finally the actual sprite manipulation
        float width = length * size.x / 2 + size.x / 2;
        if (weapon.getShip().getVariant().getHullMods().contains("safetyoverrides")) {
            length = length * 1.25f;
        }
        float height = length * size.y + (float) Math.random() * 3 + 3;
        sprite.setSize(width, height);
        sprite.setCenter(width / 2, height / 2);

        if (thruster != null) {
            // TODO: Find a way to adjust thruster flame length somehow
            EngineSlotAPI engine = thruster.getEngineSlot();
            engine.setContrailWidth(length);
        }

        // clamp the thrust then color stuff
        length = Math.max(0, Math.min(1, length));

        Color thrustColor = new Color(1, Math.min(1, 0.5f + length / 2f), Math.min(1, 0.5f + length / 4));
        if (weapon.getShip().getVariant().getHullMods().contains("safetyoverrides")) {
            thrustColor = new Color(1, Math.min(1, 0.5f + length / 20), Math.min(1, 0.5f + length / 1.5f));
        }

        sprite.setColor(thrustColor);
    }

    public float smooth(float x) {
        return 0.5f - ((float) (Math.cos(x * MathUtils.FPI) / 2));
    }
}
