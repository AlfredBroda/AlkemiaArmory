package data.hullmods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.AlkemiaIds;

// import starship_legends.RepRecord;

public class AlkemiaRimeRepaints extends BaseHullMod {

	public static String RIME_ORIGINAL = "alkemia_rime_racer";
	public static String RIME_ORANGE = "alkemia_rime_orange";
	public static String RIME_RED = "alkemia_rime_red";
	public static String RIME_ROSEN = "alkemia_rime_rosen";
	public static String RIME_JOURNEY = "alkemia_rime_journey";

	protected Logger log;

	protected String modId;

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

		modId = spec.getId();

		log = Global.getLogger(this.getClass());
	}

	public String getSkinId() {
		return RIME_ORIGINAL;
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		log.info(String.format("Repainting into %s", getSkinId()));
		replaceVariant(ship.getMutableStats(), ship.getVariant(), getSkinId());
	}

	public void replaceVariant(MutableShipStatsAPI stats, ShipVariantAPI oldVariant, String selectedSkin) {
		ShipVariantAPI newVariant = Global.getSettings().getVariant(selectedSkin + "_Hull");

		newVariant.setOriginalVariant(oldVariant.getHullVariantId());

		FleetMemberAPI oldShip = stats.getFleetMember();

		Map<String, Boolean> modsToOmmit = new HashMap<>();
		modsToOmmit.put(modId, false);
		transferDMods(oldShip, newVariant);

		for (String smod : oldVariant.getSMods()) {
			modsToOmmit.put(smod, true);
			newVariant.addPermaMod(smod, true);
		}

		for (String mod : oldVariant.getHullMods()) {
			if (!modsToOmmit.containsKey(mod))
				newVariant.addMod(mod);
		}

		transferWings(oldVariant, newVariant);

		for (String slot : oldVariant.getFittedWeaponSlots()) {
			newVariant.addWeapon(slot, oldVariant.getWeaponId(slot));
		}

		newVariant.setNumFluxVents(oldVariant.getNumFluxVents());
		newVariant.setNumFluxCapacitors(oldVariant.getNumFluxCapacitors());

		stats.getFleetMember().setVariant(newVariant, true, true);
	}

	private static void transferWings(ShipVariantAPI oldVariant, ShipVariantAPI newVariant) {
		int i = 0;
		for (String wing : oldVariant.getFittedWings()) {
			newVariant.setWingId(i, wing);
			i++;
		}
	}

	public static void transferDMods(FleetMemberAPI oldShip, ShipVariantAPI targetVariant) {
		Collection<String> hullmods = oldShip.getVariant().getHullMods();
		List<HullModSpecAPI> dMods = new ArrayList<>();

		for (HullModSpecAPI mod : DModManager.getModsWithTags("dmod")) {
			if (hullmods.contains(mod.getId()))
				dMods.add(mod);
		}

		if (dMods.isEmpty())
			return;

		DModManager.setDHull(targetVariant);

		int sourceDMods = dMods.size();
		DModManager.removeUnsuitedMods(targetVariant, dMods);
		int addedDMods = dMods.size();

		if (!dMods.isEmpty()) {
			for (HullModSpecAPI mod : dMods) {
				targetVariant.addPermaMod(mod.getId());
			}

			// Crudely add random d-mods if some were unsuitable.
			if (sourceDMods > addedDMods) {
				DModManager.addDMods(targetVariant, false, sourceDMods - addedDMods, null);
			}
		} else if (sourceDMods > 0) { // All were unsuitable
			// Crudely add random d-mods
			DModManager.addDMods(targetVariant, false, sourceDMods - addedDMods, null);
		}
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getVariant().getHullSpec().getBaseHullId().startsWith(AlkemiaIds.ALKEMIA_RIME_PREFIX);
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Only applicable to Rime Racer variants";
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
		tooltip.addPara("Return in a while to review the repaint.",10f);
	}
}
