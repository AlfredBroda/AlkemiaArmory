package data.scripts.ai.missile;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;

import data.scripts.util.MagicTargeting;

public class AlkemiaEmpMissileAI extends BaseSmartMissileAI {

    public AlkemiaEmpMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);

        SEARCH_CONE = 90;
        ACTIVE_SEEKER = true;
        PROXIMITY_FUSE_DISTANCE = 200;
    }

    @Override
    public CombatEntityAPI selectTarget() {
        ShipAPI newTarget = MagicTargeting.pickTarget(missile, MagicTargeting.targetSeeking.NO_RANDOM,
                (int) missile.getWeapon().getRange(), SEARCH_CONE, 0, 0, 1, 2, 3, true);

        if (DEBUG && (newTarget != null)) {
            engine.addFloatingText(newTarget.getLocation(), "LOCK", 30.0F,
                    COLOR_RED, newTarget, 0.1f, 0.5f);
        }
        return newTarget;
    }

    @Override
    public void postDetonate(Vector2f site, Vector2f dir) {
        missile.explode();

        if (DEBUG) {
            engine.addFloatingText(site, "BOOM!", 30.0F,
                    COLOR_RED, missile, 0.1f, 0.5f);
        }
    }

    @Override
    public void lostTarget() {
        missile.giveCommand(ShipCommand.DECELERATE);
    }
}
