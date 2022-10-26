package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class AlkemiaRimeRepaintRosen extends AlkemiaRimeRepaints {

	@Override
	public String getSkinId() {
		return RIME_ROSEN;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,
			boolean isForModSpec) {
	}
}
