package data.hullmods;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import data.scripts.AlkemiaIds;

public class KriegFortressHull extends BaseModuleRegenHullMod {

	protected Logger log;

	@Override
	public void init(HullModSpecAPI spec) {
		moduleSlots.put("TS0001", "krieg_fortress_tail_left_Base");
		moduleSlots.put("TS0002", "krieg_fortress_tail_right_Base");

		super.init(spec);

		log = Global.getLogger(this.getClass());
	}

	@Override
	public String getName() {
        return "Krieg Fortress";
    }

	@Override
    public String getId() {
        return AlkemiaIds.KRIEG_FORTRESS;
    }

}
