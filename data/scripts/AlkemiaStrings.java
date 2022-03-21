package data.scripts;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.util.Misc;

public class AlkemiaStrings {
    public static final String TECTONIC_REASON = "Connot be built due to tectonic activity.";
    public static final String NOT_CONDITIONS_FORMAT = "Can only be built on %s planets.";
    public static final String NOT_PLANET_REASON = "Can only be built on planets.";

    public static String getNeededConditions(List<String> conditionIDs) {
        List<String> conditions = new ArrayList<String>();
        for (String id : conditionIDs) {
            MarketConditionSpecAPI mc = Global.getSettings().getMarketConditionSpec(id);
            conditions.add(mc.getName());
        }
        return Misc.getJoined("or", conditions);
    }
}
