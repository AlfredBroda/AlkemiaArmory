package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.CompromisedStructure;

public class AlkemiaImprovedManufactory extends BaseHullMod {

	public static float SPEED_IMPROVEMENT = 0.10f;
	public static float DAMAGE_DECREASE = -0.10f;
	
	
	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		float effect = ship.getMutableStats().getDynamic().getValue(Stats.DMOD_EFFECT_MULT);
		
		MutableShipStatsAPI stats = fighter.getMutableStats();
		
		stats.getMaxSpeed().modifyMult(id, 1f + SPEED_IMPROVEMENT * effect);

		stats.getArmorDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);
		stats.getShieldDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);
		stats.getHullDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);

		//fighter.setHeavyDHullOverlay();
		//fighter.setLightDHullOverlay();
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		CompromisedStructure.modifyCost(hullSize, stats, id);
	}
	
		
	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		float effect = 1f;
		if (ship != null) effect = ship.getMutableStats().getDynamic().getValue(Stats.DMOD_EFFECT_MULT);
		
		if (index == 0) return "" + (int) Math.round(SPEED_IMPROVEMENT * 100f * effect) + "%";
		if (index == 1) return "" + (int) Math.round(DAMAGE_DECREASE * 100f * effect) + "%";
		return null;
	}
}




