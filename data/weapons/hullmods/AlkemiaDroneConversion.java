package data.hullmods;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AlkemiaDroneConversion extends BaseHullMod {

	public static final int CREW_REQ = -15;
	public static final int ALL_FIGHTER_COST_PERCENT = 60;
	
	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FRIGATE, 0f);
		mag.put(HullSize.DESTROYER, 75f);
		mag.put(HullSize.CRUISER, 50f);
		mag.put(HullSize.CAPITAL_SHIP, 25f);
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getFighterRefitTimeMult().modifyPercent(id, ((Float) mag.get(hullSize)));
		stats.getNumFighterBays().modifyFlat(id, 1f);

		stats.getMinCrewMod().modifyFlat(id, CREW_REQ * stats.getNumFighterBays());
		stats.getDynamic().getMod(Stats.ALL_FIGHTER_COST_MOD).modifyPercent(id, ALL_FIGHTER_COST_PERCENT);
		// TODO: Make the ship only accept dornes now. Maybe something to do with AUTOMATED_FIGHTER?
	}
	
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && !ship.isFrigate() && ship.getHullSpec().getFighterBays() > 0 &&
								!ship.getVariant().hasHullMod(HullMods.CONVERTED_BAY) &&
								!ship.getVariant().hasHullMod(HullMods.PHASE_FIELD);
	}
	
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship != null && ship.isFrigate()) return "Can not be installed on a frigate";
		if (ship != null && ship.getHullSpec().getFighterBays() <= 0) return "Ship has no standard fighter bays";
		if (ship != null && ship.getVariant().hasHullMod(HullMods.CONVERTED_BAY)) return "Ship has converted fighter bays";
		return "Can not be installed on a phase ship";
	}
	
	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		new AlkemiaImprovedManufactory().applyEffectsToFighterSpawnedByShip(fighter, ship, id);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		if (index == 2) return "" + CREW_REQ;
		if (index == 3) return "" + ALL_FIGHTER_COST_PERCENT + "%";
		return new AlkemiaImprovedManufactory().getDescriptionParam(index, hullSize, ship);
	}
	
	@Override
	public boolean affectsOPCosts() {
		return true;
	}
}



