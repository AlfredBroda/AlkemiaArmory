package data.scripts.tools;

import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

public class KriegDefenderGen {

    public static CampaignFleetAPI getFleetForPlanet(SectorEntityToken planet, String factionID) {
        Logger log = Global.getLogger(KriegDefenderGen.class);
        
        CampaignFleetAPI defenders = planet.getMemoryWithoutUpdate().getFleet("$defenderFleet");

        if (defenders != null) {
            log.info("returning existing planet defenders");
            return defenders;
        } else {
            log.info("creating new defenders");
            defenders = getNewFleet(planet, factionID);
        }

        return defenders;
    }

    public static CampaignFleetAPI getNewFleet(SectorEntityToken planet, String factionID){
        MarketAPI m = planet.getMarket();

        CampaignFleetAPI defenders = createDefenderFleet(m, factionID);

        defenders.getFleetData().sort();

        return defenders;
    }

    public static final float MIN_FLEET_SIZE = 60;
    public static final float MAX_HAZARD = 200f;

    public static CampaignFleetAPI createDefenderFleet(MarketAPI market, String factionId){
        float defenderBonus = 1 + Math.min(1, (MAX_HAZARD - market.getHazardValue()) / 100f);

        long seed = market.getPrimaryEntity().getMemoryWithoutUpdate().getLong(MemFlags.SALVAGE_SEED);
        Random random = Misc.getRandom(seed, 1);

        FleetParamsV3 fParams = new FleetParamsV3(null, null,
                factionId,
                1f,
                FleetTypes.PATROL_LARGE,
                (int) MIN_FLEET_SIZE * defenderBonus,
                0, 0, 0, 0, 0, 0);

        FactionAPI faction = Global.getSector().getFaction(factionId);

        fParams.withOfficers = faction.getCustomBoolean(Factions.CUSTOM_OFFICERS_ON_AUTOMATED_DEFENSES);
        fParams.random = random;
        fParams.ignoreMarketFleetSizeMult = true;
        fParams.withOfficers = true;
        fParams.officerNumberMult = 1.5f;

        CampaignFleetAPI defenders = FleetFactoryV3.createFleet(fParams);

        defenders.getInflater().setRemoveAfterInflating(false);
        defenders.setName("Krieg Air Force Ambushers");
        defenders.clearAbilities();

        return defenders;
    }
}
