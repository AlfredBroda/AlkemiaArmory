package data.scripts.tools;

import static com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin.auroraColors;
import static com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin.baseColors;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class Helpers {

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity,
            List<SectorEntityToken> connectedEntities, String name,
            int size, List<String> marketConditions, List<String> industries, List<String> submarkets, float tarrif,
            Boolean withJunk) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();

        MarketAPI newMarket = primaryEntity.getMarket();
        if (newMarket == null) {
            newMarket = Global.getFactory().createMarket(primaryEntity.getId(), name, size);
            newMarket.setPrimaryEntity(primaryEntity);
        } else {
            newMarket.setSize(size);
        }

        newMarket.setFactionId(factionID);
        if (factionID != null) {
            newMarket.getTariff().modifyFlat("default_tariff", newMarket.getFaction().getTariffFraction());
        }

        if (null != submarkets) {
            for (String market : submarkets) {
                if (!newMarket.hasSubmarket(market))
                    newMarket.addSubmarket(market);
            }
        }

        if (null != marketConditions) {
            for (String condition : marketConditions) {
                if (!newMarket.hasCondition(condition))
                    newMarket.addCondition(condition);
            }
        }

        if (null != industries) {
            for (String industry : industries) {
                if (!newMarket.hasIndustry(industry))
                    newMarket.addIndustry(industry);
            }
        }

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                if (entity != null) {
                    newMarket.getConnectedEntities().add(entity);
                }
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

    public static MarketAPI addConditionsMarket(PlanetAPI planet, String name, List<String> planetConditions) {
        MarketAPI market = Global.getFactory().createMarket("market_" + planet.getId(), name, 1);
        market.setPlanetConditionMarketOnly(true);
        market.setFactionId(Factions.NEUTRAL);
        market.setHidden(true);
        planet.setMarket(market);

        market.reapplyConditions();

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
                        flareProbability, // probability to spawn aurora sequence, checked once/day when no aurora in
                                          // progress
                        auroraColors[auroraIndex]));
        magField.setCircularOrbit(token, 0, 0, 100);
    }

    public static final Comparator<FleetMemberAPI> COMPARE_PRIORITY = new Comparator<FleetMemberAPI>() {
        // -1 means member1 is first, 1 means member2 is first
        @Override
        public int compare(FleetMemberAPI member1, FleetMemberAPI member2) {
            if (!member1.isCivilian()) {
                if (member2.isCivilian()) {
                    return -1;
                }
            } else if (!member2.isCivilian()) {
                return 1;
            }

            int sizeCompare = member2.getHullSpec().getHullSize().compareTo(member1.getHullSpec().getHullSize());
            if (sizeCompare != 0) {
                return sizeCompare;
            }

            if (member1.getFleetPointCost() > member2.getFleetPointCost()) {
                return -1;
            } else if (member1.getFleetPointCost() < member2.getFleetPointCost()) {
                return 1;
            }

            return MathUtils.getRandomNumberInRange(-1, 1);
        }
    };

    public static String getRandomElement(List<String> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    public static String getRandomElement(String[] list) {
        return list[new Random().nextInt(list.length)];
    }

    public static void makeDiscoverable(SectorEntityToken entity, float range, float profile, float xp) {
        entity.setDiscoverable(true);
        entity.setDiscoveryXP(xp);
        entity.setSensorProfile(profile);
        entity.getDetectedRangeMod().modifyFlat("gen", range);
    }

    public static StarSystemAPI findGateSystemWithPlanets() {
        WeightedRandomPicker<StarSystemAPI> selected = new WeightedRandomPicker<>(new Random());

        List<SectorEntityToken> gates = Global.getSector().getEntitiesWithTag(Tags.GATE);
        for (SectorEntityToken gate : gates) {
            StarSystemAPI system = gate.getStarSystem();

            // must be unknown
            if (system.isEnteredByPlayer())
                continue;
            // avoid pulsars
            if (Misc.hasPulsar(system))
                continue;
            // not otherwise populated
            if (Misc.getMarketsInLocation(system).size() > 0)
                continue;

            List<SectorEntityToken> planets = system.getEntitiesWithTag(Tags.PLANET);
            float weight = 2;
            if (planets.size() > 0)
                weight = weight / planets.size();

            if (system.hasTag(Tags.THEME_INTERESTING)) {
                weight *= 0.5f;
            } else if (system.hasTag(Tags.THEME_INTERESTING_MINOR)) {
                weight *= 0.75f;
            }

            if (system.hasTag(Tags.BEACON_HIGH)) {
                weight *= 0.1f;
            } else if (system.hasTag(Tags.BEACON_MEDIUM)) {
                weight *= 0.75f;
            }

            selected.add(system, weight);
        }

        return selected.pick();
    }

    /**
     * @param market
     * @param condition
     * @return if the condition was already present
     */
    public static boolean addCondition(MarketAPI market, String condition) {
        if (!market.hasCondition(condition)) {
            market.addCondition(condition);
            return false;
        }
        return true;
    }

    public static Object getGreek(int i) {
        switch (i) {
            case 1:
                return "Alpha";
            case 2:
                return "Beta";
            case 3:
                return "Gamma";
            case 4:
                return "Delta";
            case 5:
                return "Epsilon";
            case 6:
                return "Zeta";
            case 7:
                return "Eta";
            case 8:
                return "Theta";
            case 9:
                return "Iota";
            case 10:
                return "Kappa";
            case 11:
                return "Lambda";
            case 12:
                return "Mu";
            case 13:
                return "Nu";
            case 14:
                return "Xi";
        }
        return "Omega";
    }

    public static float evenSpreadAngle(float allAngle, int seqNum, int allNum) {
        float spreadAngle = allAngle / allNum;
        return (spreadAngle * seqNum + spreadAngle / 2) - allAngle / 2;
    }
}
