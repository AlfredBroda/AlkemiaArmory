//by Tartiflette, Anti-missile missile AI: precise and able to randomly choose a target between nearby enemy missiles.
//feel free to use it, credit is appreciated but not mandatory
//V2 done
package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
// import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.plugins.SCY_projectilesEffectPlugin;
import data.scripts.util.MagicRender;
import data.scripts.util.MagicTargeting;

import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class alkemiaPDMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
    // private final IntervalUtil timer = new IntervalUtil(0.025f, 0.075f);
    private Color COLOR_RED = new Color(255, 0, 0, 255);
    // data
    private final float OVERSHOT_ANGLE = 45;
    private final float MAX_SPEED;
    private final float DAMPING = 0.05f;
    private final Color EXPLOSION_COLOR = new Color(255, 0, 0, 255);
    private final Color PARTICLE_COLOR = new Color(240, 200, 50, 255);
    private final int NUM_PARTICLES = 10;

    private boolean launch = true;
    private float timer = 0, check = 0f;

    public alkemiaPDMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed();
        // timer.randomize();

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
    }

    @Override
    public void advance(float amount) {

        if (engine.isPaused() || missile.isFading() || missile.isFizzling()) {
            return;
        }

        // assigning a target if there is none or it got destroyed
        if (target == null
                || !engine.isEntityInPlay(target)
                || (target instanceof MissileAPI && isHarmless((MissileAPI) target))
                || (target instanceof ShipAPI && ((ShipAPI) target).isHulk())
                || target.getCollisionClass() == CollisionClass.NONE) {

            // target has vanished, remove the missile from the master list
            SCY_projectilesEffectPlugin.forceCheck();
            // dumbly get the nearest missile as target
            // target = AIUtils.getNearestEnemyMissile(missile);
            target = findRandomMissileWithinRange(missile);

            // if none, target nearest enemy fighter/frigate instead
            if (target == null) {
                target = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.FULL_RANDOM,
                        (int) missile.getWeapon().getRange(), 360, 3, 2, 1, 1, 1, true);
            }
            // debug
            if (target != null) {
                engine.addFloatingText(target.getLocation(), "O", 20.0F,
                        COLOR_RED, target, 0.2f, 0.8f);
            }

            // forced acceleration by default
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        // timer.advance(amount);
        // if (timer.intervalElapsed()) {
        float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());

        // proximity fuse
        if (dist < 2500) {
            proximityFuse();
            return;
        }

        timer += amount;
        // finding lead point to aim to
        if (launch || timer >= check) {
            launch = false;
            timer -= check;
            // set the next check time
            check = Math.min(0.5f, Math.max(0.05f,
                    MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation())));
            // best intercepting point
            lead = AIUtils.getBestInterceptPoint(
                    missile.getLocation(),
                    MAX_SPEED,
                    target.getLocation(),
                    target.getVelocity());
            // null pointer protection
            if (lead == null) {
                lead = target.getLocation();
            }
        }

        // best velocity vector angle for interception
        float correctAngle = VectorUtils.getAngle(
                missile.getLocation(),
                lead);

        // target angle for interception
        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), correctAngle);

        // velocity angle correction
        float offCourseAngle = MathUtils.getShortestRotation(
                VectorUtils.getFacing(missile.getVelocity()),
                correctAngle);

        float correction = MathUtils.getShortestRotation(
                correctAngle,
                VectorUtils.getFacing(missile.getVelocity()) + 180)
                * 0.5f * // oversteer
                // damping when the correction isn't important
                (float) ((FastTrig.sin(MathUtils.FPI / 90 * (Math.min(Math.abs(offCourseAngle), 45)))));

        // modified optimal facing to correct the velocity vector angle as soon as
        // possible
        correctAngle = correctAngle + correction;

        if (Math.abs(aimAngle) < OVERSHOT_ANGLE) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }

        // Damp angular velocity if we're getting close to the target angle
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
    }

    private CombatEntityAPI findRandomMissileWithinRange(MissileAPI missile) {
        MissileAPI theTarget;
        ShipAPI launchingShip = missile.getSource();
        WeightedRandomPicker<MissileAPI> targets = new WeightedRandomPicker<>();

        List<MissileAPI> TARGETTED = SCY_projectilesEffectPlugin.getAntimissiles();

        for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(launchingShip, getRemainingRange(missile))) {

            if (isHarmless(m)
                    || m.getProjectileSpecId().equals("SCY_antiS")
                    || m.getProjectileSpecId().equals("diableavionics_magicmissile"))
                continue;

            float danger = 2f;

            // determine the proximity danger:
            if (MathUtils.isWithinRange(launchingShip, m, 333)) {
                danger = 4;
            } else if (MathUtils.isWithinRange(launchingShip, m, 666)) {
                danger = 3;
            }

            // adjust for the damage danger
            if (m.getDamageAmount() > 700) {
                danger++;
            } else if (m.getDamageAmount() < 150) {
                danger--;
            }

            // reduce the danger from missiles already under interception
            if (TARGETTED.contains(m)) {
                if (m.getHitpoints() * m.getHullLevel() <= missile.getDamageAmount()) {
                    danger -= 2;
                } else {
                    danger--;
                }
            }

            targets.add(m, danger);
        }

        theTarget = targets.pick();
        if (theTarget != null) {
            SCY_projectilesEffectPlugin.addAntimissiles(missile, theTarget);
        }

        return theTarget;
    }

    private boolean isHarmless(MissileAPI m) {
        return m.isFizzling() || m.isExpired() || m.isFading();
    }

    private float getRemainingRange(MissileAPI missile) {
        return MAX_SPEED * (missile.getFlightTime() - missile.getElapsed());
    }

    void proximityFuse() {
        // damage the target
        engine.applyDamage(
                target,
                target.getLocation(),
                missile.getDamageAmount(),
                DamageType.FRAGMENTATION,
                missile.getEmpAmount(),
                false,
                false,
                missile.getSource());

        // damage nearby targets
        List<MissileAPI> closeMissiles = AIUtils.getNearbyEnemyMissiles(missile, 100);
        for (MissileAPI cm : closeMissiles) {
            if (cm != target) {
                float damageMult = ((float) Math
                        .cos(3000 / (MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation()) + 1000))
                        + 1);
                engine.applyDamage(
                        cm,
                        cm.getLocation(),
                        (2 * missile.getDamageAmount() / 3) - (missile.getDamageAmount() / 3) * damageMult,
                        DamageType.FRAGMENTATION,
                        missile.getEmpAmount() * damageMult,
                        false,
                        true,
                        missile.getSource());
            }
        }

        if (MagicRender.screenCheck(0.5f, missile.getLocation())) {
            engine.addHitParticle(
                    missile.getLocation(),
                    new Vector2f(),
                    100,
                    1,
                    0.25f,
                    EXPLOSION_COLOR);

            for (int i = 0; i < NUM_PARTICLES; i++) {
                float axis = (float) Math.random() * 360;
                float range = (float) Math.random() * 100;
                engine.addHitParticle(
                        MathUtils.getPoint(missile.getLocation(), range / 5, axis),
                        MathUtils.getPoint(new Vector2f(), range, axis),
                        2 + (float) Math.random() * 2,
                        1,
                        1 + (float) Math.random(),
                        PARTICLE_COLOR);
            }
        }

        // remove the missile from the master list
        SCY_projectilesEffectPlugin.forceCheck();

        // kill the missile
        engine.applyDamage(
                missile,
                missile.getLocation(),
                missile.getHitpoints() * 2f,
                DamageType.FRAGMENTATION,
                0f,
                false,
                false,
                missile);
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    public void init(CombatEngineAPI engine) {
    }
}
