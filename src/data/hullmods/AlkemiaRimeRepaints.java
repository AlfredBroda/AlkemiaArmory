package data.hullmods;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.AlkemiaIds;

public class AlkemiaRimeRepaints extends BaseHullMod {

	public static String RIME_ORIGINAL = "alkemia_rime_racer";
	public static String RIME_ORANGE = "alkemia_rime_orange";
	public static String RIME_RED = "alkemia_rime_red";
	public static String RIME_ROSEN = "alkemia_rime_rosenritter";
	public static String RIME_JOURNEY = "alkemia_rime_journey";

	protected Logger log;

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

		log = Global.getLogger(this.getClass());
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		ShipVariantAPI variant = stats.getVariant();
		// String selectedSkin = getSkinId();

		if (variant != null) {
			//add the proper hullmod
			// variant.addMod(selectedSkin);
			// ShipHullSpecAPI hull = variant.getHullSpec();
		}
	}

	public String getSkinId() {
		return RIME_ORIGINAL;
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		
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
	}
}
