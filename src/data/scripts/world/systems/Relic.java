package data.scripts.world.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch.SearchData;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.CustomConstellationParams;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import data.scripts.AlkemiaIds;
import data.scripts.AlkemiaModPlugin;
import data.scripts.tools.Helpers;

public class Relic {

    protected final Random genRandom;
    protected final Logger log;

    protected transient SearchData search = new SearchData();

    public Relic() {
        genRandom = new Random();
        log = Global.getLogger(getClass());
    }

    /**
     * @return
     */
    public StarSystemAPI generateSystem() {
        StarSystemAPI system = findGateSystemWithPlanets();

        if (system == null) {
            StarSystemGenerator gen = new StarSystemGenerator(new CustomConstellationParams(StarAge.YOUNG));
            Constellation newConstel = gen.generate();
            system = newConstel.getSystemWithMostPlanets();
        }
        system.addTag(Tags.THEME_INTERESTING);

        PlanetAPI star = system.getStar();

        OrbitGap kriegOrbit = Helpers.findEmptyOrbit(system, 5000, 8000, 1000);

        PlanetAPI krieg = system.addPlanet(
                "krieg_planet",
                star,
                "Krieg",
                "terran-eccentric",
                360f * (float) Math.random(),
                90,
                kriegOrbit.start,
                260);

        int beltWidth = 800;
        OrbitGap beltOrbit = Helpers.findEmptyOrbit(system, 2000, 7000, beltWidth);

        system.addAsteroidBelt(
                star,
                500,
                Helpers.getMiddle(beltOrbit),
                beltWidth,
                600,
                400,
                Terrain.ASTEROID_BELT,
                "The Line");

        system.addPlanet("krieg_moon", krieg, "Kanshi-sha", "barren",
                360f * (float) Math.random(), 20, 400, 20);

        Misc.generatePlanetConditions(system, StarAge.YOUNG);

        // krieg.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(krieg, 0);
        krieg.setDetectionRangeDetailsOverrideMult(0.1f);
        krieg.addTag(Tags.BEACON_MEDIUM);

        MarketAPI market = Helpers.addMarketplace("krieg", krieg, null, "Krieg", 6, getPlanetConditions(),
                getPlanetIndustries(),
                getPlanetSubmarkets(), 0f, false);

        market.setHidden(true);
        // market.addTag(Tags.NO_MARKET_INFO);

        Global.getSector().getMemory().set(AlkemiaIds.KRIEG_EXISTS, true);

        return system;
    }

    private static List<String> getPlanetSubmarkets() {
        List<String> subs = new ArrayList<>();
        subs.add("krieg_surplus");
        subs.add(Submarkets.SUBMARKET_OPEN);
        subs.add(Submarkets.SUBMARKET_STORAGE);
        // if (AlkemiaModPlugin.hasRoider) {
        // subs.add("roider_resupplyMarket");
        // }
        return subs;
    }

    private static List<String> getPlanetConditions() {
        ArrayList<String> conditions = new ArrayList<>();
        conditions.add(Conditions.HABITABLE);
        conditions.add(Conditions.MILD_CLIMATE);
        conditions.add(Conditions.DENSE_ATMOSPHERE);

        conditions.add(Conditions.VOLATILES_DIFFUSE);
        conditions.add(Conditions.ORE_ABUNDANT);
        conditions.add(Conditions.FARMLAND_POOR);
        conditions.add(Conditions.ORGANICS_COMMON);

        conditions.add(Conditions.POPULATION_6);
        // conditions.add(Conditions.INDUSTRIAL_POLITY);
        conditions.add(Conditions.DISSIDENT);
        conditions.add(Conditions.POLLUTION);

        return conditions;
    }

    private static List<String> getPlanetIndustries() {
        List<String> industries = new ArrayList<String>();
        industries.add(Industries.POPULATION);
        industries.add(Industries.FARMING);
        // industries.add(Industries.FUELPROD);
        industries.add(Industries.MINING);
        industries.add(Industries.LIGHTINDUSTRY);
        // industries.add(Industries.HEAVYINDUSTRY);
        industries.add(Industries.GROUNDDEFENSES);
        industries.add(Industries.MILITARYBASE);
        // industries.add(Industries.HIGHCOMMAND);
        if (AlkemiaModPlugin.hasIndEvo) {
            industries.add("IndEvo_Ruins");
            // industries.add("IndEvo_RuinedInfra");
            industries.add("IndEvo_AdManuf");
        }
        return industries;
    }

    private StarSystemAPI findGateSystemWithPlanets() {
        WeightedRandomPicker<StarSystemAPI> selected = new WeightedRandomPicker<>(genRandom);

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
}
