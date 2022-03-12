package data.hullmods;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;

//public class AlkemiaDroneConversion extends YunruShipAutomation {
public class AlkemiaDroneConversion extends BaseHullMod {

	public static final int CREW_REQ = -10;
	public static final int ALL_DRONE_COST_REDUCTION = -20;
	public static final int BAY_SPACE_GAIN = 1;
	public static final int REARM_SPEEDUP_PERCENT = -20;

	protected Logger log;
	protected List<String> removedWings = new ArrayList<String>();

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

		log = Global.getLogger(this.getClass());
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getFighterRefitTimeMult().modifyPercent(id, REARM_SPEEDUP_PERCENT);
		stats.getNumFighterBays().modifyFlat(id, BAY_SPACE_GAIN, "Expanded bays by drone conversion.");

		stats.getMinCrewMod().modifyFlat(id, CREW_REQ * stats.getNumFighterBays().getBaseValue());

		stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);

		// Make the ship only accept drones
		// TODO: It would be better to filter LPCs like Automated HullMod does 
		// (but no source is avaliable)
		ShipVariantAPI variant = stats.getVariant();
		List<String> wings = variant.getNonBuiltInWings();
		for (int i = 0; i < wings.size(); i++) {
			FighterWingSpecAPI currentWing = variant.getWing(i);
			if (currentWing != null && !currentWing.hasTag(Tags.AUTOMATED_FIGHTER)) {
				variant.setWingId(i, null);
				removedWings.add(currentWing.getId());
			}
		}

		if (removedWings != null && !removedWings.isEmpty()) {
			log.warn("Wings removed:");
			for (String wing : removedWings) {
				log.warn(" - " + wing);
			}
		}
		// stats.getListeners() maybe there is one for when wings are picked?
	}

	public void advanceInCampaign(FleetMemberAPI member, float amount) {
		if (removedWings != null && !removedWings.isEmpty()) {
			CargoAPI cargo = member.getFleetData().getFleet().getCargo();
			if (cargo != null) {
				log.info("Wings returned:");
				for (String wing : removedWings) {
					cargo.addFighters(wing, 1);
					log.info(" - " + wing);
				}
				removedWings.clear();
			} else {
				log.error("advanceInCampaign: Cannot get CargoAPI for " + member.getHullId());
			}
		}
	}

	private void addToCargo(CargoAPI cargo, String wing) {
		cargo.addFighters(wing, 1);
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null &&
				ship.getNumFighterBays() > 0 &&
				!ship.getVariant().hasHullMod(HullMods.PHASE_FIELD);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship != null && ship.getVariant().hasHullMod(HullMods.PHASE_FIELD))
			return "Can not be installed on a phase ship";

		return "Ship has no fighter bays to convert";
	}

	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		new AlkemiaImprovedManufactory().applyEffectsToFighterSpawnedByShip(fighter, ship, id);
	}

	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		if (index == 0)
			return "" + CREW_REQ;
		if (index == 1)
			return "" + BAY_SPACE_GAIN;
		if (index == 2)
			return "" + REARM_SPEEDUP_PERCENT + "%";

		return "" + ALL_DRONE_COST_REDUCTION + "%";
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
			boolean isForModSpec) {
	}
}
