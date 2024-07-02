package data.hullmods;

import org.lwjgl.opengl.AMDDebugOutputCallback;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AlkemiaMissileIntegration extends BaseHullMod {
    public static final float COST_MOD = 10;
    public static final float AMMO_MOD = 50;
    public static final float ROF_MOD = 0.25f;

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return "" + (int) COST_MOD;
        if (index == 1)
            return "" + (int) AMMO_MOD + "%";
        if (index == 2)
            return "" + (int) (100f * ROF_MOD) + "%";
        return null;
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD).modifyFlat(id, -COST_MOD);
        stats.getMissileAmmoBonus().modifyPercent(id, AMMO_MOD);
        stats.getMissileRoFMult().modifyMult(id, 1f + ROF_MOD);
        stats.getMissileAmmoRegenMult().modifyPercent(id, AMMO_MOD);
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }
}
