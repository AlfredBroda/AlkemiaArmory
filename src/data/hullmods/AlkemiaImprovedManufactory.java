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

	/*
	@Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 10f;
        float padS = 2f;
	tooltip.addSectionHeading("Details", Alignment.MID, pad);
	tooltip.addPara("%s " + getString("MonoblocDesc1"), pad, Misc.getHighlightColor(), "-", "25%");
	tooltip.addPara("%s " + getString("MonoblocDesc2"), padS, Misc.getHighlightColor(), "-", "50%");
        //tooltip.addPara("%s " + getString("MonoblocDesc3"), padS, Misc.getHighlightColor(), "-", "50%", "600", "700", "800", "1000");
        tooltip.addPara("%s " + getString("MonoblocDesc3"), padS, Misc.getHighlightColor(), "-", "25su");
        tooltip.addSectionHeading("Incompatibilities", Alignment.MID, pad);
        TooltipMakerAPI text = tooltip.beginImageWithText("graphics/ISTL/icons/tooltip/hullmod_incompatible.png", 40);
            text.addPara(getString("DMEAllIncomp"), padS);
            text.addPara("- Heavy Armor", Misc.getNegativeHighlightColor(), padS);
            if (Global.getSettings().getModManager().isModEnabled("apex_design")) {
                text.addPara("- Nanolaminate Plating", Misc.getNegativeHighlightColor(), 0f);
                text.addPara("- Cryocooled Armor Lattice", Misc.getNegativeHighlightColor(), 0f);
            }
            text.addPara("- Converted Hangar", Misc.getNegativeHighlightColor(), 0f);
            if (Global.getSettings().getModManager().isModEnabled("roider")) {
                text.addPara("- Fighter Clamps", Misc.getNegativeHighlightColor(), 0f);
            }
        tooltip.addImageWithText(pad);
    }
	*/
}
