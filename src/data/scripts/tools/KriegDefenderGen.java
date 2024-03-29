package data.scripts.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;

public class KriegDefenderGen {

    public static CampaignFleetAPI getFleetForPlanet(SectorEntityToken planet, int basePoints) {
        Logger log = Global.getLogger(KriegDefenderGen.class);

        CampaignFleetAPI defenders = planet.getMemoryWithoutUpdate().getFleet("$defenderFleet");

        float defenderBonus = 1 + Math.min(1, (MAX_HAZARD - planet.getMarket().getHazardValue()) / 100f);

        if (defenders != null && defenders.getFleetPoints() >= basePoints) {
            log.info("returning existing planet defenders");
            return defenders;
        } else {
            log.info("creating new defenders");
            int fleetPoints = Math.round(basePoints * defenderBonus);
            String factionID = planet.getMarket().getFactionId();
            defenders = getNewFleet(planet, factionID, "Krieg Defense Fleet", fleetPoints);
        }

        return defenders;
    }

    public static CampaignFleetAPI getNewFleet(SectorEntityToken planet, String factionID, String name, int minSize) {
        CampaignFleetAPI defenders = createDefenderFleet(planet.getMarket(), factionID, name, minSize);

        defenders.getFleetData().sort();

        return defenders;
    }

    public static final int MIN_FLEET_SIZE = 60;
    public static final float MAX_HAZARD = 200f;

    public static CampaignFleetAPI createDefenderFleet(MarketAPI market, String fID, String name, int fPts) {
        Logger log = Global.getLogger(KriegDefenderGen.class);

        long seed = market.getPrimaryEntity().getMemoryWithoutUpdate().getLong(MemFlags.SALVAGE_SEED);
        Random random = Misc.getRandom(seed, 1);

        String fleetType = FleetTypes.PATROL_SMALL;
        if (fPts > 30) {
            fleetType = FleetTypes.PATROL_MEDIUM;
        }
        if (fPts > 50) {
            fleetType = FleetTypes.PATROL_LARGE;
        }

        FleetParamsV3 params = new FleetParamsV3(
                null, // LocInHyper
                fID,
                1.0f,
                fleetType,
                fPts, // CombatPts
                0f, // FreighterPts
                0f, // TankerPts
                0f, // TransportPts
                0f, // LinerPts
                0f, // UtilityPts
                0f // QualityMod
        );
        params.random = random;
        params.ignoreMarketFleetSizeMult = true;
        params.withOfficers = true;
        params.officerNumberMult = 0.9f;
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        List<FleetMemberAPI> fleetList = new ArrayList<>(fleet.getFleetData().getMembersListCopy());
        Collections.sort(fleetList, Helpers.COMPARE_PRIORITY);

        log.info(String.format("generated points: %d/%d", fleet.getFleetPoints(), fPts));

        fleet.getInflater().setRemoveAfterInflating(false);
        fleet.setName(name);
        fleet.clearAbilities();

        return fleet;
    }
}
