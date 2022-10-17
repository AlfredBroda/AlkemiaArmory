package data.scripts.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.MagicRender;
import data.scripts.util.MagicTargeting;

import java.awt.Color;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
// import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

//Based on Anti-missile missile AI v2 by Tartiflette
public class BaseSmartMissileAI implements MissileAIPlugin, GuidedMissileAI {
    //////////////////////
    // SETTINGS //
    //////////////////////

    public static final Color FLARE_COLOR = new Color(200, 165, 55, 255);
    public static final Color SMOKE_COLOR = new Color(100, 100, 100, 200);
    public static final Color COLOR_RED = new Color(255, 0, 0, 255);

    public Integer SEARCH_CONE;
    public boolean ACTIVE_SEEKER;
    public boolean STAGE_ONE_TRANSFER_MOMENTUM;
    public boolean STAGE_ONE_EXPLODE;
    // Glow particle visual when second stage is litup
    public boolean STAGE_ONE_FLARE;
    public float PROXIMITY_FUSE_DISTANCE;

    public boolean DEBUG;

    private final float OVERSHOT_ANGLE = 90, WAVE_TIME = 2, WAVE_AMPLITUDE = 5, DAMPING = 0.1f, MAX_SPEED, OFFSET, BIAS;
    private float PRECISION_RANGE = 400;
    private static final Vector2f ZERO_VELOCITY = new Vector2f();

    // Max fudged extra velocity added to the submunitions
    private static final float SUBMUNITION_VELOCITY_MOD_MAX = 250f;
    // Min fudged extra velocity added to the submunitions
    private static final float SUBMUNITION_VELOCITY_MOD_MIN = 50f;
    // How much each submunition's aim point is offset relative to others
    private static final float SUBMUNITION_RELATIVE_OFFSET = 6f;
    // Set to engine location matched to missile projectile file
    private static final float FLARE_OFFSET = -9f;

    // Leading loss without ECCM hullmod. The higher, the less accurate the leading
    // calculation will be.
    // 1: perfect leading with and without ECCM
    // 2: half precision without ECCM
    // 3: a third as precise without ECCM. Default
    // 4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
    private float ECCM = 2; // A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!

    public CombatEngineAPI engine;
    public final MissileAPI missile;
    public final ShipAPI launchingShip;
    public CombatEntityAPI target;
    public Vector2f lead = new Vector2f();

    private boolean launch = true;
    private float timer = 0, check = 0f;

    protected Logger log = Global.getLogger(this.getClass());

    private final IntervalUtil minSplitInterval = new IntervalUtil(0.025f, 0.075f);

    // MIRV spec
    private float splitRange = 500.0f;
    private int submunitionNum = 1;
    private String splitSound = "pilum_lrm_split";
    private String secondaryProjectile = "pilum_second_stage";
    // private Smoke smokeSpec;
    private int minTimeToSplit = 1;

    private Color EXPLOSION_COLOR = new Color(255, 0, 0, 255);
    private Color PARTICLE_COLOR = new Color(240, 200, 50, 255);
    private int MIN_PARTICLES = 5;
    private int MAX_PARTICLES = 9;

    public BaseSmartMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.launchingShip = launchingShip;

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        this.missile = missile;

        DEBUG = false;
        SEARCH_CONE = 360;
        STAGE_ONE_EXPLODE = false;
        STAGE_ONE_FLARE = false;
        STAGE_ONE_TRANSFER_MOMENTUM = true;
        PROXIMITY_FUSE_DISTANCE = 0;
        ACTIVE_SEEKER = false;

        MAX_SPEED = missile.getMaxSpeed();
        if (missile.getSource().getVariant().getHullMods().contains("eccm")) {
            ECCM = 1;
        }
        // calculate the precision range factor
        PRECISION_RANGE = (float) Math.pow((2 * PRECISION_RANGE), 2);
        OFFSET = (float) (Math.random() * MathUtils.FPI * 2);
        BIAS = MathUtils.getRandomNumberInRange(-15, 15);

        if (missile.isMirv()) {
            try {
                JSONObject mirvBehavior = missile.getBehaviorSpecParams();
                splitRange = mirvBehavior.getInt("splitRange");
                splitSound = mirvBehavior.getString("splitSound");
                submunitionNum = mirvBehavior.getInt("numShots");
                secondaryProjectile = mirvBehavior.getString("projectileSpec");
                // smokeSpec = mirvBehavior.get("smokeSpec");
                minTimeToSplit = mirvBehavior.getInt("minTimeToSplit");

                minSplitInterval.forceCurrInterval(minTimeToSplit);
            } catch (JSONException e) {
                log.error(String.format("Failed reading MIRV spec: %s", e.getMessage()));
            }

            if (DEBUG) {
                Global.getLogger(BaseSmartMissileAI.class)
                        .warn(String.format("Missile %s is MIRV, split at: %.2f, projectiles: %d (%s)",
                                missile.getProjectileSpecId(), splitRange, submunitionNum, secondaryProjectile));
            }
        }
    }

    @Override
    public void advance(float amount) {

        // skip the AI if the game is paused, the missile is engineless or fading
        if (engine.isPaused() || isHarmless(missile)) {
            return;
        }

        if (target != null) {
            float targetDistance = MathUtils.getDistance(missile, lead);

            // mirv split
            if (missile.isMirv()) {
                minSplitInterval.advance(amount);
                if (targetDistance < splitRange && minSplitInterval.intervalElapsed()) {

                    launchSubmunitions();
                    return;
                }
            }

            // proximity fuse
            if (PROXIMITY_FUSE_DISTANCE > 0) {
                float dist = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());
                if (dist < PROXIMITY_FUSE_DISTANCE * PROXIMITY_FUSE_DISTANCE) {
                    proximityFuse();
                    return;
                }
            }
        }

        // assigning a target if there is none or it got destroyed
        if (target == null || isIgnored(target)) {
            lostTarget();

            target = selectTarget();

            return;
        }

        timer += amount;
        // finding lead point to aim to
        if (launch || timer >= check) {
            launch = false;
            timer -= check;

            // Reasses target selection
            if (ACTIVE_SEEKER) {
                CombatEntityAPI newTarget = selectTarget();

                if (newTarget != null) {
                    target = newTarget;
                }
            }

            // set the next check time
            check = Math.min(0.5f, Math.max(0.05f,
                    MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation()) / PRECISION_RANGE));

            // best intercepting point
            lead = AIUtils.getBestInterceptPoint(
                    missile.getLocation(),
                    MAX_SPEED * ECCM, // if eccm is installed the point is accurate, otherwise it's placed closer to
                                      // the target (almost tailchasing)
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
        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), correctAngle + BIAS);

        if (Math.abs(aimAngle) < OVERSHOT_ANGLE) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        // waving
        aimAngle += WAVE_AMPLITUDE * 4 / 3 * check * ECCM
                * Math.cos(OFFSET + missile.getElapsed() * (2 * MathUtils.FPI / WAVE_TIME));

        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }

        // Damp angular velocity if the missile aim is getting close to the targeted
        // angle
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
    }

    public boolean isIgnored(CombatEntityAPI target) {
        return (target instanceof MissileAPI && isHarmless((MissileAPI) target))
                || (target instanceof ShipAPI && ((ShipAPI) target).isHulk())
                || !engine.isEntityInPlay(target)
                || (target.getCollisionClass() == CollisionClass.NONE);
    }

    public boolean isHarmless(MissileAPI m) {
        return m.isFizzling() || m.isExpired() || m.isFading();
    }

    public float getRemainingRange(MissileAPI missile) {
        return MAX_SPEED * (missile.getFlightTime() - missile.getElapsed());
    }

    public CombatEntityAPI selectTarget() {
        ShipAPI newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(), SEARCH_CONE, 1, 2, 3, 4, 5, true);

        if (DEBUG && newTarget != null) {
            engine.addFloatingText(newTarget.getLocation(), "locked", 30.0F,
                    COLOR_RED, newTarget, 0.1f, 0.5f);
        }
        return newTarget;
    }

    private void launchSubmunitions() {
        Vector2f submunitionVelocityMod = new Vector2f(0,
                MathUtils.getRandomNumberInRange(
                        SUBMUNITION_VELOCITY_MOD_MAX, SUBMUNITION_VELOCITY_MOD_MIN));

        DamagingProjectileAPI submunition = null;
        for (int i = 0; i < submunitionNum; i++) {
            float angle = missile.getFacing() + i * SUBMUNITION_RELATIVE_OFFSET * (i % 2 > 0 ? 1 : -1);

            if (angle < 0f) {
                angle += 360f;
            } else if (angle >= 360f) {
                angle -= 360f;
            }

            Vector2f vel = STAGE_ONE_TRANSFER_MOMENTUM ? missile.getVelocity() : ZERO_VELOCITY;
            Vector2f boost = VectorUtils.rotate(submunitionVelocityMod, missile.getFacing());
            vel.translate(boost.x, boost.y);

            submunition = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(launchingShip,
                    missile.getWeapon(),
                    secondaryProjectile, missile.getLocation(), angle, vel);
            submunition.setFromMissile(true);
            if (submunition instanceof MissileAPI) {
                MissileAPI newMissile = ((MissileAPI) submunition);
                newMissile.setArmedWhileFizzling(true);
                newMissile.setForceAlwaysArmed(true);
                // newMissile.setMissileAI();
                newMissile.giveCommand(ShipCommand.ACCELERATE);
            }
        }

        Global.getSoundPlayer().playSound(splitSound, 1f, 1f, missile.getLocation(), missile.getVelocity());

        // GFX on the spot of the switcheroo if desired
        // Remove old missile
        if (STAGE_ONE_EXPLODE) {
            engine.addSmokeParticle(missile.getLocation(), missile.getVelocity(), 60f, 0.75f, 0.75f,
                    SMOKE_COLOR);
            engine.applyDamage(missile, missile.getLocation(), missile.getHitpoints() * 100f,
                    DamageType.FRAGMENTATION, 0f, false, false, missile);
        } else if (STAGE_ONE_FLARE) {
            createFlare();
        } else {
            engine.addSmokeParticle(missile.getLocation(), missile.getVelocity(), 5f, 0.75f, 1.5f,
                    SMOKE_COLOR);
            int numParticles = MathUtils.getRandomNumberInRange(MIN_PARTICLES, MAX_PARTICLES);
            for (int i = 0; i < numParticles; i++) {
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
        engine.removeEntity(missile);
    }

    private void createFlare() {
        Vector2f offset = new Vector2f(FLARE_OFFSET, 0f);
        VectorUtils.rotate(offset, missile.getFacing(), offset);
        Vector2f.add(offset, missile.getLocation(), offset);
        engine.addHitParticle(offset, missile.getVelocity(), 100f, 0.5f, 0.25f, FLARE_COLOR);
    }

    private void proximityFuse() {
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
                    PROXIMITY_FUSE_DISTANCE,
                    1,
                    0.25f,
                    EXPLOSION_COLOR);

            int numParticles = MathUtils.getRandomNumberInRange(MIN_PARTICLES, MAX_PARTICLES);
            for (int i = 0; i < numParticles; i++) {
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

        postDetonate(target.getLocation(), missile.getVelocity());

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

    public void postDetonate(Vector2f site, Vector2f dir) {
        // do cleanup when using target plugins
        if (DEBUG) {
            engine.addFloatingText(site, "BOOM!", 30.0F,
                    COLOR_RED, missile, 0.1f, 0.5f);
        }
    }

    public void lostTarget() {
        // forced acceleration by default
        missile.giveCommand(ShipCommand.ACCELERATE);
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
}
