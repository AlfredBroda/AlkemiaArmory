package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.AlkemiaIds;

public class AlkemiaRiggerProw extends BaseHullMod {

	public static final float RADIUS_MULT = 0.25f;
	public static final float DAMAGE_MULT = 0.1f;
	public static final float REPAIR_BONUS = 50f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(id, DAMAGE_MULT);
		stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(id, RADIUS_MULT);

		stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS * 0.01f);
		
		stats.getDynamic().getStat(Stats.MODULE_DETACH_CHANCE_MULT).modifyFlat(id, 100f);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getVariant().getHullSpec().getBaseHullId().startsWith(AlkemiaIds.ALKEMIA_RIGGER);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Only applicable to the Rigger";
	}

	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		return "";
	}

	@Override
	public boolean affectsOPCosts() {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
			boolean isForModSpec) {
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) 
			return String.format("%d%", (int) REPAIR_BONUS);
		return null;
	}
}
