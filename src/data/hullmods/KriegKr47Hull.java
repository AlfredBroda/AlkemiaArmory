package data.hullmods;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class KriegKr47Hull extends BaseModuleRegenHullMod {

	protected Logger log;

	@Override
	public void init(HullModSpecAPI spec) {
		moduleSlots.put("TS0001", "krieg_kr47_tail_Base");

		super.init(spec);

		log = Global.getLogger(this.getClass());
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
