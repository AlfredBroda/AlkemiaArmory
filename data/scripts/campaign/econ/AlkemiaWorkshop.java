package data.scripts.campaign.econ;

import java.util.Collections;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import data.scripts.AlkemiaIds;
import data.scripts.AlkemiaStats;
import data.scripts.AlkemiaStrings;

/**
* Author: Frederoo
 */
public class AlkemiaWorkshop extends BaseIndustry {

    public static final float ALPHA_DISCOUNT = 10f;
    public static final String INFODUMP_FLAG = "$market_infodump";    

    @Override
    public void apply() {
        float accessability = 1.0f;
        if (this.market.hasCondition(Conditions.LOW_GRAVITY)) {
            accessability = AlkemiaStats.WORKSHOP_LOW_GRAVITY_MULT;
        }
        if (this.market.hasCondition(Conditions.HIGH_GRAVITY)) {
            accessability = AlkemiaStats.WORKSHOP_HIGH_GRAVITY_MULT;
        }

        int size = this.market.getSize();
        int baseProduction = Math.round((size - 2) * accessability);

        int bonusProduction = 0;
        if (special != null) {
            bonusProduction = AlkemiaStats.ALKEMIA_NANOFORGE_PROD;
        }
        supply(Commodities.SHIPS, baseProduction + bonusProduction);

        demand(Commodities.RARE_METALS, baseProduction - 2);
        if (special == null) {
            demand(Commodities.HEAVY_MACHINERY, baseProduction - 2);
        }
        
        int baseDemand = baseProduction - 1;
        if (baseDemand < 1) {
            baseDemand = 1;
        }
        demand(Commodities.METALS, baseDemand);
        demand(Commodities.CREW, baseDemand);

        super.apply(true);

        if (!isFunctional()) {
            supply.clear();
            unapply();
        }
    }

    @Override
    protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
        if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
            float opad = 10f;
            if (this.market.hasCondition(Conditions.LOW_GRAVITY)) {
                tooltip.addPara("Low gravity of this planet increases ship production to %s.",
                opad, Misc.getHighlightColor(), 
                "" + (int) Math.round(AlkemiaStats.WORKSHOP_LOW_GRAVITY_MULT * 100f) + "%");

            }
            if (this.market.hasCondition(Conditions.HIGH_GRAVITY)) {
                tooltip.addPara("High gravity of this planet decreases ship production by %s.",
                opad, Misc.getHighlightColor(), 
                "" + (int) Math.round(AlkemiaStats.WORKSHOP_HIGH_GRAVITY_MULT * 100f) + "%");
            }
        }
        super.addPostDemandSection(tooltip, hasDemand, mode);
    }

    @Override
    public boolean canImprove() {
        return true;
    }

    @Override
    public boolean isAvailableToBuild() {
        if (!this.market.hasCondition(Conditions.HABITABLE)) {
            return false;
        }
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY) || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return false;
        }
        return super.isAvailableToBuild();
    }

    @Override
    public String getUnavailableReason() {
        if (!this.market.hasCondition(Conditions.HABITABLE)) {
            return AlkemiaStrings.getNeededConditions(Collections.singletonList(Conditions.HABITABLE));
        }
        if (this.market.hasCondition(Conditions.TECTONIC_ACTIVITY) || this.market.hasCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)) {
            return AlkemiaStrings.TECTONIC_REASON;
        }
        return super.getUnavailableReason();
    }

    @Override
	public boolean wantsToUseSpecialItem(SpecialItemData data) {
        Global.getLogger(this.getClass()).warn(special.getId());;
		if (special != null && AlkemiaIds.ALKEMIA_NANOFORGE.equals(special.getId()) && data != null) {
			return true;
		}
		return super.wantsToUseSpecialItem(data);
	}

	@Override
	public void setSpecialItem(SpecialItemData special) {
		super.setSpecialItem(special);
	}

    protected float daysWithNanoforge = 0f;

    @Override
	public void advance(float amount) {
		super.advance(amount);
		
		if (special != null) {
			float days = Global.getSector().getClock().convertToDays(amount);
			daysWithNanoforge += days;
		}
	}

    @Override
	public String getCurrentImage() {
        if (market.hasCondition(Conditions.LOW_GRAVITY)) {
			return Global.getSettings().getSpriteName("industry", "alkemia_workshop_low");
		}
        if (this.market.hasCondition(Conditions.HIGH_GRAVITY)) {
			return Global.getSettings().getSpriteName("industry", "alkemia_workshop_high");
		}
		
		return super.getCurrentImage();
	}

    // <editor-fold defaultstate="collapsed" desc="Cleanup code in various methods">
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
