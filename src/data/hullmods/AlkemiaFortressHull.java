package data.hullmods;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class AlkemiaFortressHull extends BaseHullMod {

	protected Logger log;

	private static Map<String, String> moduleSlots = new HashMap<String, String>();
	static {
		moduleSlots.put("TS0001", "alkemia_fortress_tail_left_Base");
		moduleSlots.put("TS0002", "alkemia_fortress_tail_right_Base");
	}

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

		log = Global.getLogger(this.getClass());
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		ShipVariantAPI variant = stats.getVariant();

		// Assemble the Fortress if not complete
		if (variant != null && variant.getModuleSlots() != null && variant.getModuleSlots().size() > 0) {
			log.info("Fortress incomplete, reassembling...");
			for (Map.Entry<String, String> entry : moduleSlots.entrySet()) {
				String slot = entry.getKey();
				String module = entry.getValue();
				if (variant.getModuleVariant(entry.getKey()) == null) {
					variant.setModuleVariant(slot, Global.getSettings().getVariant(module));
				}
			}
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getVariant().getHullSpec().getBaseHullId() == AlkemiaIds.ALKEMIA_FORTRESS;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Only applicable to the Rigger";
	}
}
