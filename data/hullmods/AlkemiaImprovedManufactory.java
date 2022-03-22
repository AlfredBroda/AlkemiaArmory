package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AlkemiaImprovedManufactory extends BaseHullMod {

	public static float SPEED_IMPROVEMENT = 0.10f;
	public static float DAMAGE_DECREASE = -0.10f;
	public static float SUPPLY_USE_MULT = 0.2f;

	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		float effect = ship.getMutableStats().getDynamic().getValue(Stats.DMOD_EFFECT_MULT);

		MutableShipStatsAPI stats = fighter.getMutableStats();

		stats.getMaxSpeed().modifyMult(id, 1f + SPEED_IMPROVEMENT * effect);
		// stats.getFighterRefitTimeMult().modifyPercent(id, SPEED_IMPROVEMENT);

		stats.getArmorDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);
		stats.getShieldDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);
		stats.getHullDamageTakenMult().modifyPercent(id, DAMAGE_DECREASE * 100f * effect);
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float effect = stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT);
		stats.getSuppliesPerMonth().modifyPercent(id, Math.round((SUPPLY_USE_MULT) * effect * 100f));
	}

	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		float effect = 1f;
		if (ship != null)
			effect = ship.getMutableStats().getDynamic().getValue(Stats.DMOD_EFFECT_MULT);

		if (index == 0)
			return "" + (int) Math.round(SPEED_IMPROVEMENT * 100f * effect) + "%";
		if (index == 1)
			return "" + (int) Math.round(DAMAGE_DECREASE * 100f * effect) + "%";
		if (index == 2)
			return "" + (int) Math.round(DAMAGE_DECREASE * 100f * effect) + "%";
		if (index == 3)
			return "" + (int) Math.round(DAMAGE_DECREASE * 100f * effect) + "%";
		if (index == 4)
			return "" + (int) Math.round(SUPPLY_USE_MULT * 100f * effect) + "%";
		return null;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null &&
				ship.getNumFighterBays() > 0 &&
				!ship.getVariant().hasHullMod(HullMods.PHASE_FIELD);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship != null && ship.getVariant().hasHullMod(HullMods.PHASE_FIELD))
			return "Can not be installed on a phase ship";

		return "Ship has no manufactory to improve";
	}
}
