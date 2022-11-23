package data.scripts.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;

import org.lazywizard.lazylib.MathUtils;

public class Helpers {

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity,
            List<SectorEntityToken> connectedEntities, String name,
            int size, List<String> marketConditions, List<String> Industries, List<String> submarkets, float tarrif,
            Boolean withJunk) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID;

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        if (factionID != null) {
            newMarket.getTariff().modifyFlat("default_tariff", newMarket.getFaction().getTariffFraction());
        }

        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        if (null != Industries) {
            for (String industry : Industries) {
                newMarket.addIndustry(industry);
            }
        }

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, withJunk);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        return newMarket;
    }

    public static OrbitGap findEmptyOrbit(StarSystemAPI system, float minPad, float maxDist, float minWidth) {
        List<OrbitGap> gaps = BaseThemeGenerator.findGaps(system.getStar(), minPad, maxDist, minWidth);

        float increment = 1000;
        for (int i = 0; gaps.size() == 0 && i < 3; i++) {
            maxDist += increment * i;
            gaps = BaseThemeGenerator.findGaps(system.getStar(), minPad, maxDist, minWidth);
        }
        if (gaps.size() > 0) {
            return gaps.get(gaps.size() - 1);
        }

        OrbitGap outerEdge = new OrbitGap();
        outerEdge.start = maxDist;
        outerEdge.end = maxDist + 3000;
        return outerEdge;
    }

    public static float getMiddle(OrbitGap gap) {
        return gap.start + (gap.end - gap.start) / 2;
    }
}
