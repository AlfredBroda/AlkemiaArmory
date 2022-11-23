package data.hullmods;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.plugins.AlkemiaRiggerPlugin;
import data.scripts.AlkemiaIds;
import data.scripts.AlkemiaModPlugin;

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

		// Assemble the Rigger if not complete
		if (variant != null) {
			if (variant.getModuleSlots() != null && variant.getModuleSlots().size() > 0) {
				for (Map.Entry<String, String> entry : moduleSlots.entrySet()) {
					String slot = entry.getKey();
					String module = entry.getValue();
					if (variant.getModuleVariant(entry.getKey()) == null) {
						variant.setModuleVariant(slot, Global.getSettings().getVariant(module));
					}
				}
			}
			if (AlkemiaModPlugin.hasRoider) {
				if (!variant.hasHullMod(AlkemiaIds.CONVERSION_DOCK)) {
					log.warn("Roider mod enabled, adding Conversion Dock");
					variant.addPermaMod(AlkemiaIds.CONVERSION_DOCK);
				}
				if (variant.hasHullMod(AlkemiaIds.SALVAGE_GANTRY)) {
					// Remove as otherwise we get an 80% salvage bonus
					variant.removePermaMod(AlkemiaIds.SALVAGE_GANTRY);
				}
			} else {
				if (!variant.hasHullMod(AlkemiaIds.SALVAGE_GANTRY)) {
					log.warn("Adding Salvage Gantry");
					variant.addPermaMod(AlkemiaIds.SALVAGE_GANTRY);
				}
			}
		}

		super.applyEffectsBeforeShipCreation(hullSize, stats, id);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		CombatEngineAPI combat = Global.getCombatEngine();
		if (combat != null) {
			AlkemiaRiggerPlugin.manage(ship);
		}
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship.getVariant().getHullSpec().getBaseHullId() == AlkemiaIds.ALKEMIA_RIGGER;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return "Only applicable to the Rigger";
	}
}
