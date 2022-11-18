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

public class BaseModuleRegenHullMod extends BaseHullMod {

	protected Logger log;

	public Map<String, String> moduleSlots = new HashMap<String, String>();

	@Override
	public void init(HullModSpecAPI spec) {
		super.init(spec);

        moduleSlots.put("TS0001", "module_Variant");

		log = Global.getLogger(this.getClass());
	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		ShipVariantAPI variant = stats.getVariant();

		// Assemble the Fortress if not complete
		if (variant != null && variant.getModuleSlots() != null && variant.getModuleSlots().size() > 0) {
			log.debug(String.format("%s incomplete, reassembling...", getName()));
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
		return ship.getVariant().getHullSpec().getBaseHullId() == getId();
	}

	public String getUnapplicableReason(ShipAPI ship) {
		return String.format("Only applicable to the %s", getName());
	}

    public String getName() {
        return "[ship name]";
    }

    public String getId() {
        return "[ship_id]";
    }
}
