package data.scripts.campaign.econ;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import data.scripts.AlkemiaStrings;

/**
 * Author: Frederoo
 */
public class AlkemiaOasisTowers extends BaseIndustry implements MarketImmigrationModifier {

    public static List<String> SUPPRESSED_CONDITIONS = new ArrayList<String>();
    static {
        SUPPRESSED_CONDITIONS.add(Conditions.HOT);
        SUPPRESSED_CONDITIONS.add(Conditions.VERY_HOT);
        SUPPRESSED_CONDITIONS.add(Conditions.INIMICAL_BIOSPHERE);
    }

    public static List<String> NEEDED_CONDITIONS = new ArrayList<String>();
    static {
        NEEDED_CONDITIONS.add(Conditions.HOT);
        NEEDED_CONDITIONS.add(Conditions.VERY_HOT);
        NEEDED_CONDITIONS.add(Conditions.INIMICAL_BIOSPHERE);
    }

    @Override
    public void apply() {
        for (String cid : SUPPRESSED_CONDITIONS) {
            market.suppressCondition(cid);
        }

        int size = market.getSize();
        int minimumDemand = ((int) Math.ceil((size - 2) / 2));
        demand(Commodities.SUPPLIES, minimumDemand);
        demand(Commodities.HEAVY_MACHINERY, minimumDemand);
        demand(Commodities.ORGANICS, size - 2);

        super.apply(true);
        if (!isFunctional()) {
            supply.clear();
            unapply();
        }
    }

    public void unapply(String id) {
        for (String cid : SUPPRESSED_CONDITIONS) {
            market.unsuppressCondition(cid);
        }
    }

    @Override
    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        if (isFunctional()) {
            incoming.getWeight().modifyFlat(getModId(), getPopulationGrowthBonus(), getNameForModifier());
        }
    }

    protected String getDescriptionOverride() {
        PlanetAPI planet = market.getPlanetEntity(); // Stations don't have a planet
        String planetType = "desert";
        if (planet != null) {
            if (isAvailableToBuild()) {
                planetType = planet.getSpec().getPlanetType();
            }
        }
        String desc = spec.getDesc();
        return String.format(desc, planetType);
    }

    @Override
    protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
        return mode != IndustryTooltipMode.NORMAL || isFunctional();
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
            Color h = Misc.getHighlightColor();

            float bonus = getPopulationGrowthBonus();
            float max = getMaxPopGrowthBonus();

            float opad = 10f;
            tooltip.addPara("Population growth improvement is %s (max for colony size is %s)", opad, h,
                    "+" + Math.round(bonus),
                    "+" + Math.round(max));

            float spad = 1f;
            tooltip.addPara("When built counters the effects of:", spad);
            tooltip.setBulletedListMode(BaseIntelPlugin.INDENT);
            for (String id : SUPPRESSED_CONDITIONS) {
                MarketConditionSpecAPI mc = Global.getSettings().getMarketConditionSpec(id);
                if (market.hasCondition(id)) {
                    tooltip.addPara(mc.getName(), Misc.getHighlightColor(), spad);
                } else {
                    tooltip.addPara(mc.getName(), spad);
                }
            }
            tooltip.setBulletedListMode(null);
        }
    }

    @Override
    public boolean canImprove() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        if (market.getPlanetEntity() == null) {
            return false;
        }
        if (!this.market.hasCondition(Conditions.HOT) && !this.market.hasCondition(Conditions.VERY_HOT)) {
            return false;
        }
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY)
                || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return false;
        }
        return super.isAvailableToBuild();
    }

    @Override
    public String getUnavailableReason() {
        if (market.getPlanetEntity() == null) {
            return AlkemiaStrings.NOT_PLANET_REASON;
        }
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY)
                || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return AlkemiaStrings.TECTONIC_REASON;
        }
        if (!this.market.hasCondition(Conditions.HOT) && !this.market.hasCondition(Conditions.VERY_HOT)) {
            return String.format(AlkemiaStrings.NOT_CONDITIONS_FORMAT,
                    AlkemiaStrings.getNeededConditions(NEEDED_CONDITIONS));
        }
        return super.getUnavailableReason();
    }

    protected float getPopulationGrowthBonus() {
        Pair<String, Integer> deficit = getMaxDeficit(Commodities.ORGANICS);
        float want = getDemand(Commodities.ORGANICS).getQuantity().getModifiedValue();
        float def = ((Integer) deficit.two).floatValue();
        if (def > want)
            def = want;

        float mult = 1f;
        if (def > 0 && want > 0) {
            mult = (want - def) / want;
        }

        return getMaxPopGrowthBonus() * mult;
    }

    protected float getMaxPopGrowthBonus() {
        // return market.getSize() * 2.0f;
        return getSizeMult();
    }

    @Override
    public String getCurrentImage() {
        PlanetAPI planet = market.getPlanetEntity();
        if (planet != null && planet.getSpec().getPlanetType() == "jungle") {
            return Global.getSettings().getSpriteName("industry", "alkemia_outpost_jungle");
        }

        return super.getCurrentImage();
    }

}
