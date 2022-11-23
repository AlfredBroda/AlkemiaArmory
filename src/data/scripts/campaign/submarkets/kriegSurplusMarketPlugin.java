package data.scripts.campaign.submarkets;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Misc;

public class kriegSurplusMarketPlugin extends BaseSubmarketPlugin {

    private final RepLevel MIN_STANDING = RepLevel.SUSPICIOUS;

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }


    @Override
    public float getTariff() {
        if (Misc.getCommissionFactionId() == "krieg") {
            return 0.1f;
        } else {
            return 0.2f;
        }
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));

        return super.getTooltipAppendix(ui);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;

            getCargo().getMothballedShips().clear();

            FactionDoctrineAPI doctrineOverride = submarket.getFaction().getDoctrine().clone();
            addShips(submarket.getFaction().getId(),
                    60f, // combat
                    15f, // freighter
                    10f, // tanker
                    15f, // transport
                    0f, // liner
                    0f, // utilityPts
                    null, // qualityOverride
                    0f, // qualityMod
                    ShipPickMode.PRIORITY_THEN_ALL,
                    doctrineOverride);

            pruneWeapons(0.2f);

            addWeapons(3, 10, 1, submarket.getFaction().getId());
            addFighters(3, 5, 1, submarket.getFaction().getId());		
        }

        getCargo().sort();
    }

    
	@Override
	public int getStockpileLimit(CommodityOnMarketAPI com) {
		int demand = com.getMaxDemand();
		int available = com.getAvailable();

		float limit = BaseIndustry.getSizeMult(available) - BaseIndustry.getSizeMult(Math.max(0, demand - 2));
		limit *= com.getCommodity().getEconUnit();

		//limit *= com.getMarket().getStockpileMult().getModifiedValue();

		Random random = new Random(market.getId().hashCode() + submarket.getSpecId().hashCode() + Global.getSector().getClock().getMonth() * 170000);
		limit *= 0.9f + 0.2f * random.nextFloat();

		float sm = market.getStabilityValue() / 10f;
		limit *= 0.2f * sm;

		if (limit < 0) limit = 0;

		return (int) limit;
	}

    @Override
    public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
        return com.getId().equals(Commodities.SUPPLIES)
                    || com.getId().equals(Commodities.FUEL)
                    || com.getId().equals(Commodities.CREW);
    }

    @Override
	public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return !(commodityId.equals(Commodities.SUPPLIES)
                    || commodityId.equals(Commodities.FUEL)
                    || commodityId.equals(Commodities.CREW));
	}

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    // @Override
    // public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
    //     return action == TransferAction.PLAYER_SELL;
    // }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "Sales only!";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "Sales only!";
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }
}
