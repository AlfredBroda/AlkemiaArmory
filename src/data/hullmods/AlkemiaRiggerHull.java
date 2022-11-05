package data.hullmods;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class AlkemiaRiggerHull extends AlkemiaDroneConversion {

	protected Logger log;

	private static Map<String, String> moduleSlots = new HashMap<String, String>();
	static {
		moduleSlots.put("MS0001", "alkemia_rigger_left_base");
		moduleSlots.put("MS0002", "alkemia_rigger_right_base");
	}

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

		log = Global.getLogger(this.getClass());
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		ShipVariantAPI variant = stats.getVariant();
		// String selectedSkin = getSkinId();

		// Assemble the Rigger if not complete
		if (variant != null && variant.getModuleSlots() != null && variant.getModuleSlots().size() > 0) {
			for (Map.Entry<String, String> entry : moduleSlots.entrySet()) {
				String slot = entry.getKey();
				String module = entry.getValue();
				if (variant.getModuleVariant(entry.getKey()) == null) {
					// || !variant.getModuleVariant(slot).getHullVariantId().startsWith(AlkemiaIds.ALKEMIA_RIGGER)) {
					variant.setModuleVariant(slot, Global.getSettings().getVariant(module));
				}
			}
		}

		super.applyEffectsBeforeShipCreation(hullSize, stats, id);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getVariant().getHullSpec().getBaseHullId() == AlkemiaIds.ALKEMIA_RIGGER;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Only applicable to the Rigger";
	}
}
