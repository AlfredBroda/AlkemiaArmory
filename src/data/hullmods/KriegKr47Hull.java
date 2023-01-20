package data.hullmods;

import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class KriegKr47Hull extends BaseModuleRegenHullMod {

	@Override
	public void init(HullModSpecAPI spec) {
		moduleSlots.put("MS0001", "krieg_kr47_tail_Base");

		super.init(spec);
	}

	@Override
	public String getName() {
        return "Krieg Kr-47";
    }

	@Override
    public String getId() {
        return AlkemiaIds.KRIEG_KR47;
    }
}
