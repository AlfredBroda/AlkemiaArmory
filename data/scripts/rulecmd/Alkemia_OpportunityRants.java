package scripts.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Author: SafariJohn
 */
public class Alkemia_OpportunityRants extends BaseCommandPlugin {
    public static final String DEFAULT_KEY = "$alkemia_opp_defaultRantPick";
    public static final String SP_KEY = "$alkemia_opp_spRantPick";
    public static final String INHOSP_KEY = "$alkemia_opp_inhospRantPick";

    private float getEventDays() {
        return Alkemia_Opportunity.EVENT_DAYS;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        // Default checks standard rant
        if (params.isEmpty()) {
            String id = memoryMap.get(MemKeys.LOCAL).getString(DEFAULT_KEY);
            if (id == null || id.isEmpty()) return new Random().nextBoolean();

            return id.equals(ruleId);
        }

		String command = params.get(0).getString(memoryMap);

        // default sets standard rant
        if (command.equals("set")) {
            String id = memoryMap.get(MemKeys.LOCAL).getString(DEFAULT_KEY);
            if (id == null || id.isEmpty()) {
                memoryMap.get(MemKeys.LOCAL).set(DEFAULT_KEY, ruleId, getEventDays());
                return true;
            }

            return false;
        }

        // "sp" checks SP rants
        if (command.equals("sp")) {
            String id = memoryMap.get(MemKeys.LOCAL).getString(SP_KEY);

            // set rant id
            if (params.size() == 2 && (id == null || id.isEmpty())) {
                memoryMap.get(MemKeys.LOCAL).set(SP_KEY, ruleId, getEventDays());
                return true;
            }

            if (id == null) return new Random().nextBoolean();

            return id.equals(ruleId);
        }

        // "inhosp" checks inhospitable rants
        if (command.equals("inhosp")) {
            String id = memoryMap.get(MemKeys.LOCAL).getString(INHOSP_KEY);

            // set rant id
            if (params.size() == 2 && (id == null || id.isEmpty())) {
                memoryMap.get(MemKeys.LOCAL).set(INHOSP_KEY, ruleId, getEventDays());
                return true;
            }

            if (id == null) return new Random().nextBoolean();

            return id.equals(ruleId);
        }

        return false;
    }

}
