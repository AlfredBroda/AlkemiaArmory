package data.hullmods;

import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class KriegFortressHull extends BaseModuleRegenHullMod {

	@Override
	public void init(HullModSpecAPI spec) {
		moduleSlots.put("TS0001", "krieg_fortress_tail_left_Base");
		moduleSlots.put("TS0002", "krieg_fortress_tail_right_Base");

		super.init(spec);
	}

	@Override
	public String getName() {
        return "Krieg Mutterganz";
    }

	@Override
    public String getId() {
        return AlkemiaIds.KRIEG_FORTRESS;
    }

}
