package data.scripts.campaign.econ;

import java.util.Collection;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

/**
 * Author: Frederoo
 */
public class AlkemiaWorkshop extends BaseIndustry {

    public static void aliasAttributes(XStream x) {
    }

    public static final float ALPHA_DISCOUNT = 10f;
    public static final String INFODUMP_FLAG = "$market_infodump";
    private static final String TECTONIC_REASON = "Connot be built due to tectonic activity.";

    @Override
    public void apply() {
        Logger log = Global.getLogger(this.getClass());

        float accessability = 1.0f;
        if (this.market.hasCondition(Conditions.LOW_GRAVITY)) {
            accessability = 1.5f;
        }
        if (this.market.hasCondition(Conditions.HIGH_GRAVITY)) {
            accessability = 0.5f;
        }

        int size = this.market.getSize();
        int baseProduction = Math.round((size - 2) * accessability);

        supply(Commodities.SHIPS, baseProduction);

        demand(Commodities.RARE_METALS, baseProduction - 3);
        demand(Commodities.HEAVY_MACHINERY, baseProduction - 2);
        int baseDemand = baseProduction - 1;
        if (baseDemand < 1) {
            baseDemand = 1;
        }
        demand(Commodities.METALS, baseDemand);
        demand(Commodities.CREW, baseDemand);

        /*
         * FIXME: this does not play well with janino:
         * Caused by: org.codehaus.commons.compiler.CompileException: File
         * 'data/scripts/campaign/econ/AlkemiaWorkshop.java', Line 39, Column 18: Cannot
         * cast "java.lang.Object" to "int"
         * 
         * Pair<String, Integer> deficit = getMaxDeficit(
         * Commodities.SUPPLIES,
         * Commodities.SHIPS,
         * Commodities.HEAVY_MACHINERY,
         * Commodities.RARE_METALS);
         * int maxDeficit = size - 2; // to allow *some* production so economy doesn't
         * get into an unrecoverable state
         * if (deficit.two > maxDeficit)
         * deficit.two = maxDeficit;
         * 
         * applyDeficitToProduction(2, deficit,
         * Commodities.SUPPLIES,
         * Commodities.METALS,
         * Commodities.HEAVY_MACHINERY,
         * Commodities.RARE_METALS);
         */

        super.apply(true);

        if (!isFunctional()) {
            supply.clear();
            unapply();
        }
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        // if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
    }

    @Override
    protected void applyAlphaCoreModifiers() {
        super.applyAlphaCoreModifiers();
    }

    @Override
    protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
        super.addAlphaCoreDescription(tooltip, mode);

        // tooltip.addPara("Reduces retrofit costs by %s.", 0f,
        // Misc.getHighlightColor(),
        // (int) ALPHA_DISCOUNT + "%");
    }

    @Override
    public boolean canImprove() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY) || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return false;
        }
        return super.isAvailableToBuild();
    }

    @Override
    public String getUnavailableReason() {
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY) || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return TECTONIC_REASON;
        }
        return super.getUnavailableReason();
    }

    // <editor-fold defaultstate="collapsed" desc="Cleanup code in various methods">
    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    protected void notifyDisrupted() {
        super.notifyDisrupted();
    }

    @Override
    protected void disruptionFinished() {
        super.disruptionFinished();
    }

    @Override
    public void notifyBeingRemoved(MarketAPI.MarketInteractionMode mode, boolean forUpgrade) {
        super.notifyBeingRemoved(mode, forUpgrade);
    }

    @Override
    protected void applyGammaCoreModifiers() {
        super.applyGammaCoreModifiers();
    }

    @Override
    protected void applyBetaCoreModifiers() {
        super.applyBetaCoreModifiers();
    }

    @Override
    protected void applyNoAICoreModifiers() {
        super.applyNoAICoreModifiers();
    }
    // </editor-fold>

}
