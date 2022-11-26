package data.scripts.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
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

    public static CampaignFleetAPI getFleetForPlanet(SectorEntityToken planet, String factionID, String name) {
        Logger log = Global.getLogger(KriegDefenderGen.class);
        
        CampaignFleetAPI defenders = planet.getMemoryWithoutUpdate().getFleet("$defenderFleet");

        if (defenders != null) {
            log.info("returning existing planet defenders");
            return defenders;
        } else {
            log.info("creating new defenders");
            defenders = getNewFleet(planet, factionID, name);
        }

        return defenders;
    }

    public static CampaignFleetAPI getNewFleet(SectorEntityToken planet, String factionID, String name){
        MarketAPI m = planet.getMarket();

        CampaignFleetAPI defenders = createDefenderFleet(m, factionID, name);

        defenders.getFleetData().sort();

        return defenders;
    }

    public static final float MIN_FLEET_SIZE = 60;
    public static final float MAX_HAZARD = 200f;

    public static CampaignFleetAPI createDefenderFleet(MarketAPI market, String factionId, String name){
        float defenderBonus = 1 + Math.min(1, (MAX_HAZARD - market.getHazardValue()) / 100f);
        Logger log = Global.getLogger(KriegDefenderGen.class);

        long seed = market.getPrimaryEntity().getMemoryWithoutUpdate().getLong(MemFlags.SALVAGE_SEED);
        Random random = Misc.getRandom(seed, 1);
        float fleetP = MIN_FLEET_SIZE * defenderBonus;

		FleetParamsV3 params = new FleetParamsV3(
				null, // LocInHyper
				factionId,
				1.0f,
				FleetTypes.PATROL_MEDIUM,
				fleetP, // CombatPts
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

        log.info(String.format("generated: bonus %.2f, points: %d/%.2f", defenderBonus, fleet.getFleetPoints(), fleetP));

        fleet.getInflater().setRemoveAfterInflating(false);
        fleet.setName(name);
        fleet.clearAbilities();

        return fleet;
    }
}
