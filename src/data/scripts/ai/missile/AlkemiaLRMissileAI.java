package data.scripts.ai.missile;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import data.scripts.util.MagicTargeting;

public class AlkemiaLRMissileAI extends BaseSmartMissileAI {

    public AlkemiaLRMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        SEARCH_CONE = 270;
        STAGE_ONE_EXPLODE = false;
        STAGE_ONE_FLARE = false;
        STAGE_ONE_TRANSFER_MOMENTUM = true;
        ACTIVE_SEEKER = true;
    }

    @Override
    public CombatEntityAPI selectTarget() {
        ShipAPI newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(), SEARCH_CONE, 0, 1, 2, 3, 3, true);

        if (DEBUG && (newTarget != null)) {
            engine.addFloatingText(newTarget.getLocation(), "LOCK", 30.0F,
                    COLOR_RED, newTarget, 0.1f, 0.5f);
        }
        return newTarget;
    }
}
