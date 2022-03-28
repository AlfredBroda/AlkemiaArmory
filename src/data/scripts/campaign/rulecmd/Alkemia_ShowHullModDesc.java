package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Alkemia_ShowHullModDesc <id/memvar>
 * Author: Frederoo
 */
public class Alkemia_ShowHullModDesc extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        Misc.Token modParam = params.get(0);

		String modId = modParam.getStringWithTokenReplacement(ruleId, dialog, memoryMap);
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);

        if (spec == null) {
            modId = (String) Global.getSector().getMemoryWithoutUpdate().get(modParam.string);
            spec = Global.getSettings().getHullModSpec(modId);
        }

        if (spec == null) return false;

        TextPanelAPI text = dialog.getTextPanel();
        TooltipMakerAPI tooltip = text.beginTooltip();
        TooltipMakerAPI desc = tooltip.beginImageWithText(spec.getSpriteName(), 32);
        desc.addTitle(spec.getDisplayName());
        tooltip.addImageWithText(10f);
        tooltip.addPara(spec.getDescription(ShipAPI.HullSize.CRUISER), 10f);
        text.addTooltip();

        return true;
    }

}
