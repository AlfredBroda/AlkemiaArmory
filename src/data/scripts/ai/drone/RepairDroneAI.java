package data.scripts.ai.drone;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

import data.scripts.ai.ship.BaseShipAI;
import data.scripts.AlkemiaIds;
import data.scripts.tools.IceUtils;
import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/*
 * Based on Sundog's MxDroneAI
 * https://github.com/NateNBJ/IdoneusCitadelExiles
 */
public class RepairDroneAI extends BaseShipAI {
    ShipAPI mothership;
    ShipAPI target;
    Vector2f targetOffset;
    Point cellToFix = new Point();
    Random rng = new Random();
    ArmorGridAPI armorGrid;
    SoundAPI repairSound;
    float max, cellSize;
    int gridWidth, gridHeight, cellCount, timesToPriorityChange = 0;
    boolean doingRepairs = false;
    boolean returning = false;
    float dontRestoreAmmoUntil = 0;
    float hpAtLastCheck;
    float targetFacingOffset = Float.MIN_VALUE;
    float repairRange = 20f;
    float roamRange = 500f;

    ShipwideAIFlags shipwideFlags = new ShipwideAIFlags();
    ShipAIConfig config = new ShipAIConfig();

    static HashMap<ShipAPI, Float> peakCrRecovery = new HashMap<ShipAPI, Float>();
    static HashMap<ShipAPI, Integer> assistTracker = new HashMap<ShipAPI, Integer>();
    static HashMap<ShipAPI, Float> priorityMap = new HashMap<ShipAPI, Float>();
    static float priorityUpdateFrequency = 2f;
    static float timeOfPriorityUpdate = 2f;

    static final float REPAIR_AMOUNT = 0.3f;
    static final float CR_PEAK_TIME_RECOVERY_RATE = 3f;
    static final float FLUX_PER_MX_PERFORMED = 1f;
    static final float CRIT_FLUX_LEVEL = 0.9f;
    static final float VENT_FLUX_LEVEL = 0.85f;
    static final float COOLDOWN_PER_OP_OF_AMMO_RESTORED = 45f; // In seconds

    static final Color SPARK_COLOR = new Color(255, 223, 128);
    static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
    static final float SPARK_DURATION = 0.2f;
    static final float SPARK_BRIGHTNESS = 1.0f;
    static final float SPARK_MAX_RADIUS = 7f;
    static final float SPARK_CHANCE = 0.17f;
    static final float SPARK_SPEED_MULTIPLIER = 500.0f;
    static final float SPARK_VOLUME = 1.0f;
    static final float SPARK_PITCH = 1.0f;

    private static final boolean DEBUG = false;

    public RepairDroneAI(ShipAPI drone, ShipAPI mothership) {
        super(drone);

        this.mothership = mothership;

        if (drone.getWing() != null) {
            this.roamRange = drone.getWing().getRange();
        }

        shipwideFlags.setFlag(AIFlags.DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS);
        ;

        hpAtLastCheck = drone.getHitpoints();
        // circumstanceEvaluationTimer.setInterval(0.8f, 1.2f);
    }

    public void warn(String mesg) {
        Global.getLogger(this.getClass()).warn(mesg);
    }

    static void updatePriorities() {
        assistTracker.clear();
        priorityMap.clear();

        for (Iterator<ShipAPI> iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext();) {
            ShipAPI obj = iter.next();
            if (obj.isAlive()) {
                if (obj.getHullSpec().getHullId().equals(AlkemiaIds.ALKEMIA_REPAIR_DRONE)) {
                    addAssistance(obj.getShipTarget(), 1);
                } else if (!obj.isShuttlePod() && !obj.isDrone() && !obj.isFighter()) {
                    priorityMap.put(obj, getPriority(obj));

                    if (DEBUG) {
                        Global.getCombatEngine().addFloatingText(obj.getLocation(),
                                priorityMap.get(obj).toString(), 40, Color.green, obj, 1, 5);
                    }
                }
            }
        }

        // TODO: Find debug utils
        // if (DEBUG) {
        // Utils.print(
        // " Priority:" +
        // (Float)mxPriorities.get(Global.getCombatEngine().getPlayerShip()) +
        // " Armor:" + getArmorPercent(Global.getCombatEngine().getPlayerShip()) +
        // " Ordnance:" +
        // getExpendedOrdnancePoints(Global.getCombatEngine().getPlayerShip()) +
        // " MxAssist:" + getMxAssistance(Global.getCombatEngine().getPlayerShip()) +
        // " PeakCR:" + getSecondsTilCrLoss(Global.getCombatEngine().getPlayerShip()));
        // }

        timeOfPriorityUpdate = priorityUpdateFrequency + Global.getCombatEngine().getTotalElapsedTime(false);
    }

    static void addAssistance(ShipAPI ship, int amount) {
        if (ship != null) {
            if (!assistTracker.containsKey(ship))
                assistTracker.put(ship, (Integer) amount);
            else
                assistTracker.put(ship, ((Integer) assistTracker.get(ship)) + amount);
        }
    }

    static int getAssistance(ShipAPI ship) {
        return (assistTracker.containsKey(ship)) ? (int) (Integer) assistTracker.get(ship) : 0;
    }

    static float getSecondsTilCrLoss(ShipAPI ship) {
        float secondsTilCrLoss = 0;

        if (ship.losesCRDuringCombat()) {
            if (peakCrRecovery.containsKey(ship))
                secondsTilCrLoss += (Float) peakCrRecovery.get(ship);

            secondsTilCrLoss += ship.getHullSpec().getNoCRLossTime() - ship.getTimeDeployedForCRReduction();

        } else
            secondsTilCrLoss = Float.MAX_VALUE;

        return Math.max(0, secondsTilCrLoss);
    }

    static float getExpandedOrdnancePoints(ShipAPI ship) {
        float acc = 0;

        for (Iterator<WeaponAPI> iter = ship.getAllWeapons().iterator(); iter.hasNext();) {
            WeaponAPI weapon = iter.next();

            acc += (weapon.usesAmmo() && weapon.getSpec().getAmmoPerSecond() == 0)
                    ? (1 - weapon.getAmmo() / (float) weapon.getMaxAmmo()) * weapon.getSpec().getOrdnancePointCost(null)
                    : 0;
        }

        return acc;
    }

    static float getPriority(ShipAPI ship) {
        float priority = 0;
        float fp = IceUtils.getFP(ship);
        fp = (ship.isFighter()) ? fp / ship.getWing().getWingMembers().size() : fp;
        // float peakCrLeft = getSecondsTilCrLoss(ship);

        priority += 1.0f * (1 - IceUtils.getArmorPercent(ship)) * fp;
        // TODO: re-enable this if restocking ammo
        // priority += 0.5f * getExpendedOrdnancePoints(ship);

        // if (ship.losesCRDuringCombat())
        // priority += 1.0f
        // * ((60 / (60 + peakCrLeft)) * (1 - peakCrLeft /
        // ship.getHullSpec().getNoCRLossTime()) * fp);

        priority *= 2f / (2f + getAssistance(ship));

        if (ship == Global.getCombatEngine().getPlayerShip())
            priority *= 2;

        return priority;
    }

    @Override
    public void evaluateCircumstances() {
        if (!mothership.isAlive()) {
            if (ship.isAlive()) {
                ship.makeLookDisabled();
                ship.controlsLocked();
            }

            return;
        }
        --timesToPriorityChange;

        if (timeOfPriorityUpdate <= Global.getCombatEngine().getTotalElapsedTime(false)
                || timeOfPriorityUpdate > Global.getCombatEngine().getTotalElapsedTime(false)
                        + priorityUpdateFrequency)
            updatePriorities();

        ShipAPI previousTarget = target;
        setTarget(chooseTarget());

        if (returning) {
            targetOffset = IceUtils.toRelative(target, mothership.getLocation());
        } else if (target != previousTarget || timesToPriorityChange < 1) {
            timesToPriorityChange = 5;

            do {
                targetOffset = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            } while (!CollisionUtils.isPointWithinBounds(targetOffset, target));

            targetOffset = IceUtils.toRelative(target, targetOffset);

            armorGrid = target.getArmorGrid();
            max = armorGrid.getMaxArmorInCell();
            cellSize = armorGrid.getCellSize();
            gridWidth = armorGrid.getGrid().length;
            gridHeight = armorGrid.getGrid()[0].length;
            cellCount = gridWidth * gridHeight;
        }

        if ((target.getPhaseCloak() == null || !target.getPhaseCloak().isOn())
                && !returning
                && !(hpAtLastCheck < ship.getHitpoints())
                && MathUtils.getDistance(ship, target) < repairRange
                && priorityMap.containsKey(target)
                && ((Float) priorityMap.get(target)) > 0) {
            performMaintenance();
        } else {
            doingRepairs = false;
        }

        hpAtLastCheck = ship.getHitpoints();
    }

    void performMaintenance() {
        for (int i = 0; i < (1 + cellCount / 5); ++i) {
            cellToFix.x = rng.nextInt(gridWidth);
            cellToFix.y = rng.nextInt(gridHeight);

            if (armorGrid.getArmorValue(cellToFix.x, cellToFix.y) < max)
                break;
        }

        Vector2f at = IceUtils.getCellLocation(target, cellToFix.x, cellToFix.y);

        // for (int i = 0; (i < 10) && !CollisionUtils.isPointWithinBounds(at, target);
        // ++i)
        // at = MathUtils.getRandomPointInCircle(target.getLocation(),
        // target.getCollisionRadius());

        super.fireSelectedGroup(at);

        // TODO: Ammo restoration?
        // restoreAmmo();

        // ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() +
        // FLUX_PER_MX_PERFORMED);

        doingRepairs = true;
    }

    void restoreAmmo() {
        if (dontRestoreAmmoUntil > Global.getCombatEngine().getTotalElapsedTime(false))
            return;

        WeaponAPI winner = null;
        float lowestAmmo = 1;

        for (Iterator<WeaponAPI> iter = target.getAllWeapons().iterator(); iter.hasNext();) {
            WeaponAPI weapon = iter.next();

            if (!weapon.usesAmmo() || weapon.getSpec().getAmmoPerSecond() > 0)
                continue;

            float ammo = weapon.getAmmo() / (float) weapon.getMaxAmmo();

            if (ammo < lowestAmmo) {
                lowestAmmo = ammo;
                winner = weapon;
            }
        }

        if (winner == null) {
            dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + 1;
            return;
        }

        float op = winner.getSpec().getOrdnancePointCost(null);
        int ammoToRestore = (int) Math.max(1, Math.floor(winner.getMaxAmmo() / op));
        ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
        // Utils.print("%"+lowestAmmo*100+" "+winner.getId()+" "+ammoToRestore);
        winner.setAmmo(winner.getAmmo() + ammoToRestore);
        dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false)
                + COOLDOWN_PER_OP_OF_AMMO_RESTORED * ((ammoToRestore / (float) winner.getMaxAmmo()) * op);
    }

    void repairArmor() {
        if (cellToFix == null)
            return;

        float totalRepaired = 0;

        for (int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++x) {
            if (x < 0 || x >= gridWidth)
                continue;

            for (int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++y) {
                if (y < 0 || y >= gridHeight)
                    continue;

                float mult = (float) ((3 - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f);

                totalRepaired -= armorGrid.getArmorValue(x, y);
                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_AMOUNT * mult));
                totalRepaired += armorGrid.getArmorValue(x, y);
            }
        }

        IceUtils.showHealText(ship, ship.getLocation(), totalRepaired);
        ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() + totalRepaired);

        // Global.getCombatEngine().addFloatingDamageText(s.getLocation(),
        // totalRepaired, ICEModPlugin.HEAL_TEXT_COLOR, target, target);
    }

    void maintainCR(float amount) {
        if (target.losesCRDuringCombat()) {
            Float peakTimeRecovered = 0f;

            if (!peakCrRecovery.containsKey(target))
                peakCrRecovery.put(target, 0f);
            else
                peakTimeRecovered = (Float) peakCrRecovery.get(target);

            float t = target.getTimeDeployedForCRReduction() - peakTimeRecovered
                    - target.getHullSpec().getNoCRLossTime();

            peakTimeRecovered += (t > 0) ? t : 0;

            peakTimeRecovered += amount * (CR_PEAK_TIME_RECOVERY_RATE + target.getHullSpec().getCRLossPerSecond());
            peakTimeRecovered = Math.min(peakTimeRecovered, target.getTimeDeployedForCRReduction());
            target.getMutableStats().getPeakCRDuration().modifyFlat(AlkemiaIds.ALKEMIA_REPAIR_DRONE, peakTimeRecovered);

            peakCrRecovery.put(target, peakTimeRecovered);
        }
    }

    ShipAPI chooseTarget() {
        if (needsRefit()) {
            returning = true;
            ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux());
            return mothership;
        } else
            returning = false;

        if (mothership.getShipTarget() != null
                && mothership.getOwner() == mothership.getShipTarget().getOwner()
                && !mothership.getShipTarget().isDrone()
                && !mothership.getShipTarget().isFighter()) {
            return mothership.getShipTarget();
            // } else if (system.getDroneOrders() == DroneOrders.DEPLOY) {
            // return mothership;
        }

        float record = 0;
        ShipAPI leader = null;

        for (Iterator<ShipAPI> iter = priorityMap.keySet().iterator(); iter.hasNext();) {
            ShipAPI obj = iter.next();

            if (obj.getOwner() != ship.getOwner() || obj.isDrone() || obj.isFighter())
                continue;

            if (MathUtils.getDistance(mothership, obj) > roamRange)
                continue;

            float score = priorityMap.get(obj) / (500 + MathUtils.getDistance(ship, obj));

            if (score > record) {
                record = score;
                leader = obj;
            }
        }

        return (leader == null) ? mothership : leader;
    }

    void setTarget(ShipAPI newTarget) {
        if (target == newTarget)
            return;
        ship.setShipTarget(target = newTarget);
    }

    Vector2f getDestination() {
        return targetOffset;
    }

    void goToDestination() {
        Vector2f to = IceUtils.toAbsolute(target, targetOffset);
        float distance = MathUtils.getDistance(ship, to);

        // if (doingRepairs) {
        // if (distance < 100) {
        // float f = (1 - distance / 100) * 0.2f;
        // ship.getLocation().x = (to.x * f + ship.getLocation().x * (2 - f)) / 2;
        // ship.getLocation().y = (to.y * f + ship.getLocation().y * (2 - f)) / 2;
        // ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x *
        // (2 - f)) / 2;
        // ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y *
        // (2 - f)) / 2;
        // }
        // }

        if (doingRepairs && distance < repairRange) {
            Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, ship, SPARK_PITCH,
                    SPARK_VOLUME, ship.getLocation(), ship.getVelocity());

            if (targetFacingOffset == Float.MIN_VALUE) {
                targetFacingOffset = ship.getFacing() - target.getFacing();
            } else {
                ship.setFacing(MathUtils.clampAngle(targetFacingOffset + target.getFacing()));
            }

            if (Math.random() < SPARK_CHANCE) {
                Vector2f loc = new Vector2f(ship.getLocation());
                loc.x += cellSize * 0.5f - cellSize * (float) Math.random();
                loc.y += cellSize * 0.5f - cellSize * (float) Math.random();

                Vector2f vel = new Vector2f(ship.getVelocity());
                vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
                vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

                Global.getCombatEngine().addHitParticle(loc, vel,
                        (SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS),
                        SPARK_BRIGHTNESS,
                        SPARK_DURATION * (float) Math.random() + SPARK_DURATION,
                        SPARK_COLOR);
            }
        } else {
            targetFacingOffset = Float.MIN_VALUE;
            float angleDif = MathUtils.getShortestRotation(ship.getFacing(),
                    VectorUtils.getAngle(ship.getLocation(), to));

            if (Math.abs(angleDif) < 30) {
                accelerate();
            } else {
                turnToward(to);
                decelerate();
            }
            strafeToward(to);
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (target == null)
            return;

        if (doingRepairs) {
            repairArmor();
            // TODO: verify what happens here
            // maintainCR(amount);
            if (needsVent()) {
                ship.giveCommand(ShipCommand.VENT_FLUX, target, 0);
            }
            if (needsRefit()) {
                returning = true;
            }
        } else if (returning && !ship.isLanding()
                && MathUtils.getDistance(ship, mothership) < mothership.getCollisionRadius())
            ship.beginLandingAnimation(mothership);

        goToDestination();
    }

    @Override
    public boolean needsRefit() {
        return ship.getFluxTracker().getFluxLevel() >= CRIT_FLUX_LEVEL;
    }

    public boolean needsVent() {
        return ship.getFluxTracker().getFluxLevel() >= VENT_FLUX_LEVEL;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return shipwideFlags;
    }

    @Override
    public void cancelCurrentManeuver() {
        doingRepairs = false;
        returning = false;
    }

    @Override
    public ShipAIConfig getConfig() {
        return config;
    }
}
