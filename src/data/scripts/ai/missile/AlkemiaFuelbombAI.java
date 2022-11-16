package data.scripts.ai.missile;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import data.scripts.util.MagicTargeting;

public class AlkemiaFuelbombAI extends BaseSmartMissileAI {

    public AlkemiaFuelbombAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        SEARCH_CONE = 90;
        PROXIMITY_FUSE_DISTANCE = 200;
        STAGE_ONE_EXPLODE = true;
        STAGE_ONE_FLARE = false;
        ACTIVE_SEEKER = false;
    }

    @Override
    public CombatEntityAPI selectTarget() {
        ShipAPI newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(), SEARCH_CONE, 0, 0, 1, 2, 3, true);

        if (DEBUG_TARGET && (newTarget != null)) {
            engine.addFloatingText(newTarget.getLocation(), "LOCK", 30.0F,
                    COLOR_RED, newTarget, 0.1f, 0.5f);
        }
        return newTarget;
    }

    @Override
    public void advance(float amount) {
        // skip the AI if the game is paused
        if (engine.isPaused()) {
            return;
        }
        if (missile.getHitpoints() < 10) {
            // go out with a bamg!
            proximityFuse();
            return;
        }

        // business as usual
        super.advance(amount);
    }
}
