package data.hullmods;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.FighterOPCostModifier;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import org.lazywizard.lazylib.campaign.MessageUtils;

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
		// stats.getNumFighterBays().modifyFlat(id, BAY_SPACE_GAIN, "Expanded bays by drone conversion.");

		stats.getMinCrewMod().modifyFlat(id, CREW_REQ * stats.getNumFighterBays().getBaseValue());

		stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);
		stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyPercent(id, ALL_DRONE_COST_REDUCTION);

		// Filter out any installed non-drones and return them to cargo
		ShipVariantAPI variant = stats.getVariant();
		List<String> wings = variant.getNonBuiltInWings();
		for (int i = 0; i <= wings.size(); i++) {
			FighterWingSpecAPI currentWing = variant.getWing(i);
			if (currentWing != null && !currentWing.hasTag(Tags.AUTOMATED_FIGHTER)) {
				variant.setWingId(i, null);
				if (currentWing != null && isInPlayerFleet(stats)) {
					removedWings.add(currentWing.getId());

					log.info("Removed wing: " + currentWing.getId());
					MessageUtils.showMessage("Removed non-drone wing: " + currentWing.getWingName());
				}
			}
		}
		returnWings(removedWings);

		if (removedWings != null && !removedWings.isEmpty()) {
			log.warn("Wings not returned:");
			for (String wing : removedWings) {
				log.warn(" - " + wing);
			}
		}

		// Make the ship only accept drones
		stats.addListener(new FighterOPCostModifier() {
			public int getFighterOPCost(MutableShipStatsAPI stats, FighterWingSpecAPI fighter, int currCost) {
				if (!fighter.hasTag(Tags.AUTOMATED_FIGHTER)) {
					return 1000;
				}
				return currCost;
			}
		});
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		// Sanity check for number of bays if using BAY_SPACE_GAIN
		/*
		Boolean legal = checkModStillLegal(ship);
		if (!legal) {
			if (isInPlayerFleet(ship)) {
				returnWings(removedWings);
			} else {
				removedWings.clear();
			}
		} 
		*/
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
		// if (index == 1)
		// 	return "" + BAY_SPACE_GAIN;
		if (index == 1)
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

	/*
	private Boolean checkModStillLegal(ShipAPI ship) {
		ShipVariantAPI variant = ship.getVariant();
		String modId = this.spec.getId();
		if (variant.hasHullMod(modId) && ship.getNumFighterBays() == BAY_SPACE_GAIN) {
			log.warn("This hullmod is the only reason this ship still has any bays (" + variant.getWings().size()
					+ ")! Removing.");
			List<String> wings = variant.getNonBuiltInWings();
			FighterWingSpecAPI currentWing = null;
			for (int i = 0; i < wings.size(); i++) {
				currentWing = variant.getWing(i);
				if (currentWing != null) {
					removedWings.add(currentWing.getId());
					variant.setWingId(i, null);
					log.warn("Removing wing: " + currentWing.getId());
				}
			}
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),
					this.spec.getId(), this.spec.getId());

			return false;
		}
		return true;
	}
	*/

	private boolean addFightersToCargo(String wing, int num) {
		SectorAPI sector = Global.getSector();
		String empty = "SectorAPI";
		if (sector != null) {
			empty = "CampaignFleetAPI";
			CampaignFleetAPI fleet = sector.getPlayerFleet();
			if (fleet != null) {
				empty = "CargoAPI";
				CargoAPI cargo = fleet.getCargo();
				if (cargo != null) {
					cargo.addFighters(wing, num);
					return true;
				}
			}
		}
		log.error("addFightersToCargo: Cannot get " + empty + "!");
		return false;
	}

	private void returnWings(List<String> removedWings) {
		if (removedWings != null && !removedWings.isEmpty()) {
			log.info("Returning wings:");
			boolean errors = false;
			for (String wing : removedWings) {
				if (addFightersToCargo(wing, 1)) {
					log.info(" - " + wing);
				} else {
					errors = true;
					log.error(" # " + wing);
				}
			}
			if (!errors) {
				log.info("Clearing wing list.");
				removedWings.clear();
			}
		}
	}
}
