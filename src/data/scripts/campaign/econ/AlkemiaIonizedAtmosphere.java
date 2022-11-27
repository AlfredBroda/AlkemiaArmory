package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class AlkemiaIonizedAtmosphere extends BaseHazardCondition {
    public static final float ACCESS_PENALTY = 10f;

    @Override
    public boolean showIcon() {
        return true;
    }

    public void apply(String id) {
        super.apply(id);
        market.getAccessibilityMod().modifyFlat(id, -ACCESS_PENALTY / 100f, "Ionized Atmosphere");
    }

    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        tooltip.addPara("%s accessibility",
                10f, Misc.getHighlightColor(),
                "-" + (int) ACCESS_PENALTY + "%");
    }
}
