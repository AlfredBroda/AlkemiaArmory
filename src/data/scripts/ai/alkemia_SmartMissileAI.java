package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import data.scripts.util.MagicTargeting;

import org.apache.log4j.Logger;

public class alkemia_SmartMissileAI extends MagicMissileAI {

    protected final MagicTargeting.targetSeeking seeking = MagicTargeting.targetSeeking.LOCAL_RANDOM;
    protected Logger log = Global.getLogger(this.getClass());

    public alkemia_SmartMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }
}
