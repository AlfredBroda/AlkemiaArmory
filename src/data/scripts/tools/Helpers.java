package data.scripts.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;

import static com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin.auroraColors;
import static com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin.baseColors;

import org.lazywizard.lazylib.MathUtils;

public class Helpers {

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity,
            List<SectorEntityToken> connectedEntities, String name,
            int size, List<String> marketConditions, List<String> Industries, List<String> submarkets, float tarrif,
            Boolean withJunk) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();

        MarketAPI newMarket = primaryEntity.getMarket();
        if (newMarket == null) {
            newMarket = Global.getFactory().createMarket(primaryEntity.getId(), name, size);
        }

        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        if (factionID != null) {
            newMarket.getTariff().modifyFlat("default_tariff", newMarket.getFaction().getTariffFraction());
        }

        if (null != submarkets) {
            for (String market : submarkets) {
                if (!newMarket.hasSubmarket(market))
                    newMarket.addSubmarket(market);
            }
        }

        for (String condition : marketConditions) {
            if (!newMarket.hasCondition(condition)) 
                newMarket.addCondition(condition);
        }

        if (null != Industries) {
            for (String industry : Industries) {
                if (!newMarket.hasIndustry(industry))
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

    public static MarketAPI addConditionsMarket(PlanetAPI planet, String name, List<String> planetConditions, int size,
            boolean hidden) {
        MarketAPI market = Global.getFactory().createMarket(planet.getId(), name, size);
        market.setHidden(hidden);
        market.setPlanetConditionMarketOnly(true);

        return market;
    }

    public static void addMagneticField(SectorEntityToken token, float flareProbability, float width, boolean jp) {
        StarSystemAPI system = token.getStarSystem();

        int baseIndex = (int) (baseColors.length * StarSystemGenerator.random.nextFloat());
        int auroraIndex = (int) (auroraColors.length * StarSystemGenerator.random.nextFloat());

        float bandWidth = token.getRadius() + width;
        float midRadius = jp ? token.getRadius() / 2f : (token.getRadius() + width) / 2f;
        float visStartRadius = token.getRadius();
        float visEndRadius = token.getRadius() + width + 50f;

        SectorEntityToken magField = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldTerrainPlugin.MagneticFieldParams(bandWidth, // terrain effect band width
                        midRadius, // terrain effect middle radius
                        token, // entity that it's around
                        visStartRadius, // visual band start
                        visEndRadius, // visual band end
                        baseColors[baseIndex], // base color
                        flareProbability, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                        auroraColors[auroraIndex]
                ));
        magField.setCircularOrbit(token, 0, 0, 100);
    }
}
