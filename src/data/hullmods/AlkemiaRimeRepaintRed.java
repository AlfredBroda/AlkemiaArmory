package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class AlkemiaRimeRepaintRed extends AlkemiaRimeRepaints {

	@Override
	public String getSkinId() {
		return RIME_RED;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
			boolean isForModSpec) {
	}
}
