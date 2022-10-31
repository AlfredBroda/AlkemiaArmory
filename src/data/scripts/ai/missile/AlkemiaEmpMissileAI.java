package data.scripts.ai.missile;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;

import data.scripts.util.MagicTargeting;

public class AlkemiaEmpMissileAI extends BaseSmartMissileAI {

    private boolean BIAS_APPLIED = false;

    public AlkemiaEmpMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        DEBUG_TARGET = false;
        DEBUG_DAMAGE = false;

        SEARCH_CONE = 90;
        ACTIVE_SEEKER = true;
        PROXIMITY_FUSE_DISTANCE = 300;
    }

    @Override
    public CombatEntityAPI selectTarget() {
        ShipAPI newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(), SEARCH_CONE, 0, 0, 1, 2, 3, true);

        if (DEBUG_TARGET && (newTarget != null)) {
            engine.addFloatingText(newTarget.getLocation(), "LOCK", 30.0F,
                    COLOR_RED, newTarget, 0.1f, 0.5f);
        }
        BIAS_APPLIED = false;
        return newTarget;
    }

    @Override
    public void lostTarget() {
        missile.giveCommand(ShipCommand.DECELERATE);
        if (!BIAS_APPLIED) {
            if (BIAS > 0) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
            BIAS_APPLIED = true;
        }
    }
}
