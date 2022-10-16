package data.scripts.ai;

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

import data.scripts.util.MagicTargeting;

import java.awt.Color;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
// import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class alkemiaSmartMissileAI implements MissileAIPlugin, GuidedMissileAI {
    //////////////////////
    // SETTINGS //
    //////////////////////

    private final float OVERSHOT_ANGLE = 90, WAVE_TIME = 2, WAVE_AMPLITUDE = 5, DAMPING = 0.1f, MAX_SPEED, OFFSET, BIAS;
    private final int SEARCH_CONE = 270;
    private float PRECISION_RANGE = 400;
    private static final Vector2f ZERO_VELOCITY = new Vector2f();
    private Color COLOR_RED = new Color(255, 0, 0, 255);

    private static final boolean STAGE_ONE_TRANSFER_MOMENTUM = true;
    // Max fudged extra velocity added to the submunitions
    private static final float SUBMUNITION_VELOCITY_MOD_MAX = 250f;
    // Min fudged extra velocity added to the submunitions
    private static final float SUBMUNITION_VELOCITY_MOD_MIN = 50f;
    // How much each submunition's aim point is offset relative to others
    private static final float SUBMUNITION_RELATIVE_OFFSET = 6f;
    // Set to engine location matched to missile projectile file
    private static final float FLARE_OFFSET = -9f;
    private static final Color FLARE_COLOR = new Color(200, 165, 55, 255);
    private static final Color SMOKE_COLOR = new Color(100, 100, 100, 200);
    private static final boolean STAGE_ONE_EXPLODE = false;
    // Glow particle visual when second stage is litup
    private static final boolean STAGE_ONE_FLARE = false;

    // Leading loss without ECCM hullmod. The higher, the less accurate the leading
    // calculation will be.
    // 1: perfect leading with and without ECCM
    // 2: half precision without ECCM
    // 3: a third as precise without ECCM. Default
    // 4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
    private float ECCM = 2; // A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private final ShipAPI launchingShip;
    private CombatEntityAPI target;
    private Vector2f lead = new Vector2f();
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

    public alkemiaSmartMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        this.launchingShip = launchingShip;

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        this.missile = missile;
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
            Global.getLogger(alkemiaSmartMissileAI.class)
                    .warn(String.format("Missile %s is MIRV, split at %.2f, projectile '%s'",
                            missile.getProjectileSpecId(), splitRange, secondaryProjectile));
        }
    }

    @Override
    public void advance(float amount) {

        // skip the AI if the game is paused, the missile is engineless or fading
        if (engine.isPaused() || missile.isFading() || missile.isFizzling()) {
            return;
        }

        if (target != null && missile.isMirv()) {
            float targetDistance = MathUtils.getDistance(missile, lead);
            minSplitInterval.advance(amount);
            if (targetDistance < splitRange && minSplitInterval.intervalElapsed()) {

                launchSubmunitions();
                return;
            }
        }

        // assigning a target if there is none or it got destroyed
        if (target == null
                || (target instanceof ShipAPI && ((ShipAPI) target).isHulk())
                || !engine.isEntityInPlay(target)
                || target.getCollisionClass() == CollisionClass.NONE) {
            ShipAPI designatedTarget = launchingShip.getShipTarget();
            if (designatedTarget != null) {
                setTarget(designatedTarget);
            } else {
                setTarget(MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                        (int) missile.getWeapon().getRange(), SEARCH_CONE, 0, 1, 2, 2, 3, true));
            }

            // debug
            // if (target != null) {
            //     engine.addFloatingText(target.getLocation(), "locked", 30.0F,
            //             COLOR_RED, target, 0.1f, 0.5f);
            // }

            // forced acceleration by default
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        timer += amount;
        // finding lead point to aim to
        if (launch || timer >= check) {
            launch = false;
            timer -= check;
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

    private void launchSubmunitions() {
        Vector2f submunitionVelocityMod = new Vector2f(0,
                MathUtils.getRandomNumberInRange(
                        SUBMUNITION_VELOCITY_MOD_MAX, SUBMUNITION_VELOCITY_MOD_MIN));

        DamagingProjectileAPI submunition = null;
        for (int i = 0; i < submunitionNum; i++) {
            float angle = missile.getFacing() + i * SUBMUNITION_RELATIVE_OFFSET;

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
            // debug
            // engine.addFloatingDamageText(submunition.getLocation(), angle, FLARE_COLOR, submunition, submunition);
        }

        Global.getSoundPlayer().playSound(splitSound, 1f, 1f, missile.getLocation(), missile.getVelocity());

        // GFX on the spot of the switcheroo if desired
        // Remove old missile
        if (STAGE_ONE_EXPLODE) {
            engine.addSmokeParticle(missile.getLocation(), missile.getVelocity(), 60f, 0.75f, 0.75f,
                    SMOKE_COLOR);
            engine.applyDamage(missile, missile.getLocation(), missile.getHitpoints() * 100f,
                    DamageType.FRAGMENTATION, 0f, false, false, missile);
        }
        if (STAGE_ONE_FLARE) {
            Vector2f offset = new Vector2f(FLARE_OFFSET, 0f);
            VectorUtils.rotate(offset, missile.getFacing(), offset);
            Vector2f.add(offset, missile.getLocation(), offset);
            engine.addHitParticle(offset, missile.getVelocity(), 100f, 0.5f, 0.25f, FLARE_COLOR);
        } else {
            engine.addSmokeParticle(missile.getLocation(), missile.getVelocity(), 5f, 0.75f, 1.5f,
                    SMOKE_COLOR);
        }
        engine.removeEntity(missile);
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
