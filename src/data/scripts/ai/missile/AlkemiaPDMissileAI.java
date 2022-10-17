package data.scripts.ai.missile;

import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import data.scripts.plugins.SCY_projectilesEffectPlugin;
import data.scripts.util.MagicTargeting;

public class AlkemiaPDMissileAI extends BaseSmartMissileAI {

    public AlkemiaPDMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        SEARCH_CONE = 360;
        ACTIVE_SEEKER = true;
        PROXIMITY_FUSE_DISTANCE = 50;
    }

    @Override
    public CombatEntityAPI selectTarget() {
        // target = AIUtils.getNearestEnemyMissile(missile);
        CombatEntityAPI newTarget = findRandomMissileWithinRange(missile);

        // if none, target nearest enemy fighter/frigate instead
        if (newTarget == null) {
            newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.FULL_RANDOM,
                    (int) missile.getWeapon().getRange(), SEARCH_CONE, 3, 2, 1, 1, 1, true);
        }

        if (DEBUG && (newTarget != null)) {
            engine.addFloatingText(newTarget.getLocation(), "o", 20.0F,
                    COLOR_RED, newTarget, 0.2f, 0.8f);
        }
        return newTarget;
    }

    private CombatEntityAPI findRandomMissileWithinRange(MissileAPI missile) {
        MissileAPI newTarget;
        ShipAPI launchingShip = missile.getSource();
        WeightedRandomPicker<MissileAPI> targets = new WeightedRandomPicker<>();

        List<MissileAPI> TARGETTED = SCY_projectilesEffectPlugin.getAntimissiles();

        // getRemainingRange(missile)
        for (MissileAPI m : AIUtils.getNearbyEnemyMissiles(launchingShip, missile.getWeapon().getRange())) {
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

        newTarget = targets.pick();
        if (newTarget != null) {
            SCY_projectilesEffectPlugin.addAntimissiles(missile, newTarget);
        }

        return newTarget;
    }

    @Override
    public void postDetonate(Vector2f site, Vector2f direction) {
        // remove the missile from the master list
        SCY_projectilesEffectPlugin.forceCheck();
        engine.addSmoothParticle(site, direction, 20.0f, 0.9f, 5.0f, FLARE_COLOR);
    }

    @Override
    public void lostTarget() {
        super.lostTarget();

        // target has vanished, remove the missile from the master list
        SCY_projectilesEffectPlugin.forceCheck();
    }
}
