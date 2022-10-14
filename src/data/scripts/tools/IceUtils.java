package data.scripts.tools;

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.campaign.econ.MarketAPI;
// import com.fs.starfarer.api.campaign.events.CampaignEventManagerAPI;
// import com.fs.starfarer.api.campaign.events.CampaignEventPlugin;
// import com.fs.starfarer.api.campaign.events.CampaignEventTarget;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
// import com.fs.starfarer.api.impl.campaign.ids.Events;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
// import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

/*
 * Based on Sundog's IceUtils
 * https://github.com/NateNBJ/IdoneusCitadelExiles
 */
public class IceUtils {
    static Map<String, Float> shieldUpkeeps;
    static Map<String, String> SystemIDs;
    static final float SAFE_DISTANCE = 600f;
    static final float DEFAULT_DAMAGE_WINDOW = 3f;
    static final Map<HullSize, Float> baseOverloadTimes = new HashMap<HullSize, Float>();
    static {
        baseOverloadTimes.put(HullSize.FIGHTER, 10f);
        baseOverloadTimes.put(HullSize.FRIGATE, 4f);
        baseOverloadTimes.put(HullSize.DESTROYER, 6f);
        baseOverloadTimes.put(HullSize.CRUISER, 8f);
        baseOverloadTimes.put(HullSize.CAPITAL_SHIP, 10f);
        baseOverloadTimes.put(HullSize.DEFAULT, 6f);
    }

    // FIXME: Events.SYSTEM_BOUNTY was removed
    // public static CampaignEventPlugin offerSystemBountyIfApt(MarketAPI market, float chance) {
    //     CampaignEventManagerAPI mgt = Global.getSector().getEventManager();

    //     if (market != null && Math.random() < chance && !market.getPrimaryEntity().isInHyperspace()
    //             && !mgt.isOngoing(new CampaignEventTarget(market), Events.SYSTEM_BOUNTY)) {
    //         return mgt.startEvent(new CampaignEventTarget(market), Events.SYSTEM_BOUNTY, null);
    //     }

    //     return null;
    // }

    public static float getEngineFractionDisabled(ShipAPI ship) {
        float maxThrust = 0;
        float onlineThrust = 0;

        for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
            if (engine.isSystemActivated())
                continue;
            else if (!engine.isDisabled())
                onlineThrust += engine.getContribution();

            maxThrust += engine.getContribution();
        }

        return onlineThrust / maxThrust;
    }

    public static void showHealText(ShipAPI anchor, Vector2f at, float repairAmount) {
        // Global.getCombatEngine().addFloatingDamageText(at, repairAmount,
        // ICEModPlugin.HEAL_TEXT_COLOR, anchor, anchor);
    }

    public static List<CombatEntityAPI> getCollideablesInRange(Vector2f at, float range) {
        List<CombatEntityAPI> retVal = new LinkedList<CombatEntityAPI>();

        retVal.addAll(CombatUtils.getAsteroidsWithinRange(at, range));

        for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
            if (!ship.isFighter())
                retVal.add(ship);
        }

        return retVal;
    }

    public static List<DamagingProjectileAPI> getProjectilesDamagedBy(ShipAPI ship) {
        List<DamagingProjectileAPI> retVal = new LinkedList<DamagingProjectileAPI>();

        for (DamagingProjectileAPI p : CombatUtils.getProjectilesWithinRange(ship.getLocation(),
                ship.getCollisionRadius())) {
            if (p.didDamage() && p.getDamageTarget() == ship)
                retVal.add(p);
        }

        return retVal;
    }

    public static Vector2f getVectorFromDegrees(float degrees) {
        return new Vector2f(
                (float) Math.cos(Math.toRadians(degrees)),
                (float) Math.sin(Math.toRadians(degrees)));
    }

    public static Vector2f getOffset(Vector2f from, float direction, float distance) {
        return getOffset(from, getVectorFromDegrees(direction), distance);
    }

    public static Vector2f getOffset(Vector2f from, Vector2f toward, float distance) {
        Vector2f retVal;
        retVal = VectorUtils.getDirectionalVector(from, toward);
        retVal.scale(distance);
        Vector2f.add(retVal, from, retVal);
        return retVal;
    }

    public static float estimateOptimalRange(ShipAPI ship) {
        float acc = 0, opAcc = 0;
        // Map<WeaponType, Float> rangeBunuses = new HashMap();
        // rangeBunuses.put(WeaponType.BALLISTIC,
        // ship.getMutableStats().getBallisticWeaponRangeBonus().getBonusMult());
        // rangeBunuses.put(WeaponType.ENERGY,
        // ship.getMutableStats().getEnergyWeaponRangeBonus().getBonusMult());
        // rangeBunuses.put(WeaponType.MISSILE,
        // ship.getMutableStats().getMissileWeaponRangeBonus().getBonusMult());
        // rangeBunuses.put(WeaponType.,
        // ship.getMutableStats().getBeamWeaponRangeBonus().getBonusMult());
        // rangeBunuses.put(WeaponType.BALLISTIC,
        // ship.getMutableStats().getBallisticWeaponRangeBonus().getBonusMult());

        for (WeaponAPI w : ship.getAllWeapons()) {
            float op = w.getSpec().getOrdnancePointCost(null);
            if (w.getDamageType() == DamageType.FRAGMENTATION)
                op *= 0.2f;
            opAcc += op;
            acc += op * w.getRange();

        }

        return acc / opAcc;
    }

    public static Vector2f getDirectionalVector(float degrees) {
        double radians = Math.toRadians(degrees);
        return new Vector2f((float) Math.cos(radians), (float) Math.sin(radians));
    }

    public static Vector2f getMidpoint(Vector2f from, Vector2f to, float d) {
        d *= 2;

        return new Vector2f(
                (from.x * (2 - d) + to.x * d) / 2,
                (from.y * (2 - d) + to.y * d) / 2);
    }

    public static Vector2f getMidpoint(Vector2f from, Vector2f to) {
        return getMidpoint(from, to, 0.5f);
    }

    public static Vector2f toRelative(CombatEntityAPI entity, Vector2f point) {
        Vector2f retVal = new Vector2f(point);
        Vector2f.sub(retVal, entity.getLocation(), retVal);
        VectorUtils.rotate(retVal, -entity.getFacing(), retVal);
        return retVal;
    }

    public static Vector2f toAbsolute(CombatEntityAPI entity, Vector2f point) {
        Vector2f retVal = new Vector2f(point);
        VectorUtils.rotate(retVal, entity.getFacing(), retVal);
        Vector2f.add(retVal, entity.getLocation(), retVal);
        return retVal;
    }

    public static void blink(Vector2f at) {
        Global.getCombatEngine().addHitParticle(at, new Vector2f(), 30, 1, 0.1f, Color.RED);
    }

    public static List<ShipAPI> getShipsOnSegment(Vector2f from, Vector2f to) {
        float distance = MathUtils.getDistance(from, to);
        Vector2f center = new Vector2f();
        center.x = (from.x + to.x) / 2;
        center.y = (from.y + to.y) / 2;

        List<ShipAPI> list = new ArrayList<ShipAPI>();

        for (ShipAPI s : CombatUtils.getShipsWithinRange(center, distance / 2)) {
            if (CollisionUtils.getCollisionPoint(from, to, s) != null)
                list.add(s);
        }

        return list;
    }

    public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to, CombatEntityAPI exception) {
        ShipAPI winner = null;
        float record = Float.MAX_VALUE;

        for (ShipAPI s : getShipsOnSegment(from, to)) {
            if (s == exception)
                continue;

            float dist2 = MathUtils.getDistanceSquared(s, from);

            if (dist2 < record) {
                record = dist2;
                winner = s;
            }
        }

        return winner;
    }

    public static ShipAPI getFirstShipOnSegment(Vector2f from, Vector2f to) {
        return getFirstShipOnSegment(from, to, null);
    }

    public static ShipAPI getShipInLineOfFire(WeaponAPI weapon) {
        Vector2f endPoint = weapon.getLocation();
        endPoint.x += Math.cos(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange();
        endPoint.y += Math.sin(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange();

        return getFirstShipOnSegment(weapon.getLocation(), endPoint, weapon.getShip());
    }

    public static float getShieldUpkeep(ShipAPI ship) throws JSONException, IOException {
        if (shieldUpkeeps == null) {
            shieldUpkeeps = new HashMap<String, Float>();

            JSONArray j = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "id", "data/hulls/ship_data.csv", "starsector-core");
            for (int i = 0; i < j.length(); ++i) {
                JSONObject s = j.getJSONObject(i);
                String id = s.getString("id");

                if (id.equals(""))
                    continue;

                float upkeep = (float) s.getDouble("shield upkeep");
                shieldUpkeeps.put(id, upkeep);
            }
        }

        return shieldUpkeeps.get(ship.getHullSpec().getHullId());
    }

    public static String getSystemID(String hullID) throws JSONException, IOException {
        if (SystemIDs == null) {
            SystemIDs = new HashMap<String, String>();

            JSONArray j = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "id", "data/hulls/ship_data.csv", "starsector-core");
            for (int i = 0; i < j.length(); ++i) {
                JSONObject s = j.getJSONObject(i);
                String id = s.getString("id");

                if (id.equals(""))
                    continue;

                String systemID = s.getString("system id");
                SystemIDs.put(id, systemID);
            }
        }

        return SystemIDs.get(hullID);
    }

    public static float getArmorPercent(ShipAPI ship) {
        float acc = 0;
        int width = ship.getArmorGrid().getGrid().length;
        int height = ship.getArmorGrid().getGrid()[0].length;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                acc += ship.getArmorGrid().getArmorFraction(x, y);
            }
        }

        return acc / (width * height);
    }

    public static void setArmorPercentage(ShipAPI ship, float armorPercent) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();

        armorPercent = Math.min(1, Math.max(0, armorPercent));

        for (int x = 0; x < armorGrid.getGrid().length; ++x) {
            for (int y = 0; y < armorGrid.getGrid()[0].length; ++y) {
                armorGrid.setArmorValue(x, y, armorGrid.getMaxArmorInCell() * armorPercent);
            }
        }
    }

    public static List<Vector2f> getCellLocations(ShipAPI ship) {
        int width = ship.getArmorGrid().getGrid().length;
        int height = ship.getArmorGrid().getGrid()[0].length;
        // List cellLocations = new ArrayList(width * height);
        // Not sure if above works the way I think it does.
        List<Vector2f> cellLocations = new ArrayList<Vector2f>();

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                cellLocations.add(getCellLocation(ship, x, y));
            }
        }

        return cellLocations;
    }

    public static void setLocation(CombatEntityAPI entity, Vector2f location) {
        Vector2f dif = new Vector2f(location);
        Vector2f.sub(location, entity.getLocation(), dif);
        Vector2f.add(entity.getLocation(), dif, entity.getLocation());
    }

    public static Vector2f getCellLocation(ShipAPI ship, float x, float y) {
        x -= ship.getArmorGrid().getGrid().length / 2f;
        y -= ship.getArmorGrid().getGrid()[0].length / 2f;
        float cellSize = ship.getArmorGrid().getCellSize();
        Vector2f cellLoc = new Vector2f();
        float theta = (float) (((ship.getFacing() - 90) / 360f) * (Math.PI * 2));
        cellLoc.x = (float) (x * Math.cos(theta) - y * Math.sin(theta)) * cellSize + ship.getLocation().x;
        cellLoc.y = (float) (x * Math.sin(theta) + y * Math.cos(theta)) * cellSize + ship.getLocation().y;

        return cellLoc;
    }

    public static void log(String str) {
        Global.getLogger(IceUtils.class).debug(str);
    }

    public static void print(String str) {
        print(Global.getCombatEngine().getPlayerShip(), str);
    }

    public static void print(ShipAPI at, String str) {
        if (at == null)
            return;

        Global.getCombatEngine().addFloatingText(at.getLocation(), str, 40, Color.green, at, 1, 5);
    }

    public static void print(Vector2f at, String str) {
        if (at == null)
            return;

        Global.getCombatEngine().addFloatingText(at, str, 40, Color.green, null, 1, 5);
    }

    public static void destroy(CombatEntityAPI entity) {
        Global.getCombatEngine().applyDamage(entity, entity.getLocation(),
                entity.getMaxHitpoints() * 10f, DamageType.HIGH_EXPLOSIVE, 0,
                true, true, entity);
    }

    public static float estimateIncomingDamage(ShipAPI ship) {
        return estimateIncomingDamage(ship, DEFAULT_DAMAGE_WINDOW);
    }

    public static float estimateIncomingDamage(ShipAPI ship, float damageWindowSeconds) {
        float accumulator = 0f;

        accumulator += estimateIncomingBeamDamage(ship, damageWindowSeconds);

        for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {

            if (proj.getOwner() == ship.getOwner())
                continue; // Ignore friendly projectiles

            Vector2f endPoint = new Vector2f(proj.getVelocity());
            endPoint.scale(damageWindowSeconds);
            Vector2f.add(endPoint, proj.getLocation(), endPoint);

            if ((ship.getShield() != null && ship.getShield().isWithinArc(proj.getLocation()))
                    || !CollisionUtils.getCollides(proj.getLocation(), endPoint,
                            new Vector2f(ship.getLocation()), ship.getCollisionRadius()))
                continue;

            accumulator += proj.getDamageAmount() + proj.getEmpAmount();// * Math.max(0, Math.min(1, Math.pow(1 -
                                                                        // MathUtils.getDistance(proj, ship) /
                                                                        // safeDistance, 2)));
        }

        return accumulator;
    }

    public static float estimateIncomingBeamDamage(ShipAPI ship, float damageWindowSeconds) {
        float accumulator = 0f;

        for (Iterator<BeamAPI> iter = Global.getCombatEngine().getBeams().iterator(); iter.hasNext();) {
            BeamAPI beam = (BeamAPI) iter.next();

            if (beam.getDamageTarget() != ship)
                continue;

            float dps = beam.getWeapon().getDerivedStats().getDamageOver30Sec() / 30;
            float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();

            accumulator += (dps + emp) * damageWindowSeconds;
        }

        return accumulator;
    }

    public static float estimateIncomingMissileDamage(ShipAPI ship) {
        float accumulator = 0f;
        DamagingProjectileAPI missile;

        for (Iterator<MissileAPI> iter = Global.getCombatEngine().getMissiles().iterator(); iter.hasNext();) {
            missile = iter.next();

            if (missile.getOwner() == ship.getOwner())
                continue; // Ignore friendly missiles

            float safeDistance = SAFE_DISTANCE + ship.getCollisionRadius();
            float threat = missile.getDamageAmount() + missile.getEmpAmount();

            if (ship.getShield() != null && ship.getShield().isWithinArc(missile.getLocation()))
                continue;

            accumulator += threat
                    * Math.max(0, Math.min(1, Math.pow(1 - MathUtils.getDistance(missile, ship) / safeDistance, 2)));
        }

        return accumulator;
    }

    public static float getHitChance(DamagingProjectileAPI proj, CombatEntityAPI target) {
        float estTimeTilHit = MathUtils.getDistance(target, proj.getLocation())
                / (float) Math.max(1, proj.getWeapon().getProjectileSpeed());

        Vector2f estTargetPosChange = new Vector2f(
                target.getVelocity().x * estTimeTilHit,
                target.getVelocity().y * estTimeTilHit);

        float estFacingChange = target.getAngularVelocity() * estTimeTilHit;

        Vector2f projVelocity = proj.getVelocity();

        target.setFacing(target.getFacing() + estFacingChange);
        Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());

        projVelocity.scale(estTimeTilHit * 3);
        Vector2f.add(projVelocity, proj.getLocation(), projVelocity);
        Vector2f estHitLoc = CollisionUtils.getCollisionPoint(proj.getLocation(),
                projVelocity, target);

        target.setFacing(target.getFacing() - estFacingChange);
        Vector2f.add(target.getLocation(), (Vector2f) estTargetPosChange.scale(-1), target.getLocation());

        if (estHitLoc == null)
            return 0;

        return 1;
    }

    public static float getHitChance(WeaponAPI weapon, CombatEntityAPI target) {
        float estTimeTilHit = MathUtils.getDistance(target, weapon.getLocation())
                / (float) Math.max(1, weapon.getProjectileSpeed());

        Vector2f estTargetPosChange = new Vector2f(
                target.getVelocity().x * estTimeTilHit,
                target.getVelocity().y * estTimeTilHit);

        float estFacingChange = target.getAngularVelocity() * estTimeTilHit;

        double theta = weapon.getCurrAngle() * (Math.PI / 180);
        Vector2f projVelocity = new Vector2f(
                (float) Math.cos(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().x,
                (float) Math.sin(theta) * weapon.getProjectileSpeed() + weapon.getShip().getVelocity().y);

        target.setFacing(target.getFacing() + estFacingChange);
        Vector2f.add(target.getLocation(), estTargetPosChange, target.getLocation());

        projVelocity.scale(estTimeTilHit * 3);
        Vector2f.add(projVelocity, weapon.getLocation(), projVelocity);
        Vector2f estHitLoc = CollisionUtils.getCollisionPoint(weapon.getLocation(),
                projVelocity, target);

        target.setFacing(target.getFacing() - estFacingChange);
        Vector2f.add(target.getLocation(), (Vector2f) estTargetPosChange.scale(-1), target.getLocation());

        if (estHitLoc == null)
            return 0;

        return 1;
    }

    public static float getFPWorthOfSupport(ShipAPI ship, float range) {
        float retVal = 0;

        for (Iterator<ShipAPI> iter = AIUtils.getNearbyAllies(ship, range).iterator(); iter.hasNext();) {
            ShipAPI ally = iter.next();
            if (ally == ship)
                continue;
            float colDist = ship.getCollisionRadius() + ally.getCollisionRadius();
            float distance = Math.max(0, MathUtils.getDistance(ship, ally) - colDist);
            float maxRange = Math.max(1, range - colDist);

            retVal += getFPStrength(ally) * (1 - distance / maxRange);
        }

        return retVal;
    }

    public static float getFPWorthOfHostility(ShipAPI ship, float range) {
        float retVal = 0;

        for (Iterator<ShipAPI> iter = AIUtils.getNearbyEnemies(ship, range).iterator(); iter.hasNext();) {
            ShipAPI enemy = iter.next();
            float colDist = ship.getCollisionRadius() + enemy.getCollisionRadius();
            float distance = Math.max(0, MathUtils.getDistance(ship, enemy) - colDist);
            float maxRange = Math.max(1, range - colDist);

            retVal += getFPStrength(enemy) * (1 - distance / maxRange);
        }

        return retVal;
    }

    public static float getStrengthInArea(Vector2f at, float range) {
        float retVal = 0;

        for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
            retVal += getFPStrength(ship);
        }

        return retVal;
    }

    public static float getStrengthInArea(Vector2f at, float range, int owner) {
        float retVal = 0;

        for (ShipAPI ship : CombatUtils.getShipsWithinRange(at, range)) {
            if (ship.getOwner() == owner)
                retVal += getFPStrength(ship);
        }

        return retVal;
    }

    public static float getFPStrength(ShipAPI ship) {
        DeployedFleetMemberAPI member = Global.getCombatEngine().getFleetManager(ship.getOwner())
                .getDeployedFleetMember(ship);
        return (member == null || member.getMember() == null)
                ? 0
                : member.getMember().getMemberStrength();
    }

    public static float getFP(ShipAPI ship) {
        DeployedFleetMemberAPI member = Global.getCombatEngine().getFleetManager(ship.getOwner())
                .getDeployedFleetMember(ship);
        return (member == null || member.getMember() == null)
                ? 0
                : member.getMember().getFleetPointCost();
    }

    public static float getBaseOverloadDuration(ShipAPI ship) {
        return (Float) baseOverloadTimes.get(ship.getHullSize());
    }

    public static float estimateOverloadDurationOnHit(ShipAPI ship, float damage, DamageType type) {
        if (ship.getShield() == null)
            return 0;

        float fluxDamage = damage * type.getShieldMult()
                * ship.getMutableStats().getShieldAbsorptionMult().getModifiedValue();
        fluxDamage += ship.getFluxTracker().getCurrFlux()
                - ship.getFluxTracker().getMaxFlux();

        if (fluxDamage <= 0)
            return 0;

        return Math.min(15, getBaseOverloadDuration(ship) + fluxDamage / 25);
    }

    public static float getLifeExpectancy(ShipAPI ship) {
        float damage = estimateIncomingDamage(ship);
        return (damage <= 0) ? 3600 : ship.getHitpoints() / damage;
    }
}
