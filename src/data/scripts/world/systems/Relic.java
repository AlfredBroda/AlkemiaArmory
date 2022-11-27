package data.scripts.world.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch.SearchData;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.CustomConstellationParams;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.OrbitGap;
import com.fs.starfarer.api.util.Misc;

import data.scripts.AlkemiaIds;
import data.scripts.AlkemiaModPlugin;
import data.scripts.tools.Helpers;

public class Relic {

    protected final Logger log;

    protected transient SearchData search = new SearchData();
    private StarSystemAPI system = null;

    public Relic() {
        log = Global.getLogger(getClass());
    }

    public void generate(SectorAPI sector) {
        system = Helpers.findGateSystemWithPlanets();

        if (system == null) {
            StarSystemGenerator gen = new StarSystemGenerator(new CustomConstellationParams(StarAge.YOUNG));
            Constellation newConstel = gen.generate();
            system = newConstel.getSystemWithMostPlanets();
        }
        system.addTag(Tags.THEME_INTERESTING);

        PlanetAPI star = system.getStar();

        OrbitGap kriegOrbit = Helpers.findEmptyOrbit(system, 5000, 8000, 1000);

        PlanetAPI kriegPlanet = system.addPlanet(
                AlkemiaIds.KRIEG_PLANET,
                star,
                "Krieg",
                "terran-eccentric",
                360f * (float) Math.random(),
                90,
                kriegOrbit.start + 400,
                260);
        kriegPlanet.setInteractionImage("illustrations", "dieselpunk_city");
        kriegPlanet.setCustomDescriptionId("krieg_planet_desc");
        Helpers.addMagneticField(kriegPlanet, 0.2f, 180, false);
        // Helpers.addConditionsMarket(kriegPlanet, "Krieg", getPlanetConditions());
        PlanetConditionGenerator.generateConditionsForPlanet(kriegPlanet, system.getAge());
        Helpers.makeDiscoverable(kriegPlanet, 3000, 100, 500);

        MarketAPI tempMarket = kriegPlanet.getMarket();
        if (tempMarket != null) {
            tempMarket.addCondition(Conditions.SOLAR_ARRAY);
            tempMarket.addCondition("alkemia_ionized_atmosphere");
        }

        int beltWidth = 800;
        OrbitGap beltOrbit = Helpers.findEmptyOrbit(system, 2000, 7000, beltWidth);

        system.addAsteroidBelt(
                star,
                1500,
                Helpers.getMiddle(beltOrbit),
                beltWidth,
                400,
                600,
                Terrain.ASTEROID_BELT,
                "The Line");

        PlanetAPI kriegMoon = system.addPlanet("krieg_moon", kriegPlanet, "Kanshi-sha", "barren-bombarded",
                360f * (float) Math.random(), 20, 400, 20);
        Helpers.makeDiscoverable(kriegMoon, 3000, 10, 100);

        PlanetConditionGenerator.generateConditionsForPlanet(kriegMoon, system.getAge());

        SectorEntityToken pirateStation = system.addCustomEntity(AlkemiaIds.KRIEG_BURROW,
                "Hemera Station", "station_burrow", Factions.PIRATES);
        pirateStation.setCircularOrbitPointingDown(star, 360f * (float) Math.random(), Helpers.getMiddle(beltOrbit),
                500);
        pirateStation.setCustomDescriptionId("krieg_pirate_station_desc");
        Helpers.makeDiscoverable(pirateStation, 2000, 1, 200);
        pirateStation.addTag(Tags.STATION);

        MarketAPI pirateMarket = Helpers.addMarketplace(Factions.PIRATES, pirateStation, null, "Hemera Station", 3,
                getStationConditions(), getStationIndustries(), getStationSubmarkets(), 0f, false);
        pirateMarket.setHidden(true);

        // pirateMarket.addTag(Tags.NO_MARKET_INFO);

        Global.getSector().getMemory().set(AlkemiaIds.KEY_KRIEG_EXISTS, true);
    }

    private List<String> getStationConditions() {
        ArrayList<String> conditions = new ArrayList<>();
        conditions.add(Conditions.NO_ATMOSPHERE);

        conditions.add(Conditions.VOLATILES_DIFFUSE);
        conditions.add(Conditions.ORE_ABUNDANT);

        conditions.add(Conditions.POPULATION_3);

        return conditions;
    }

    private List<String> getStationSubmarkets() {
        List<String> subs = new ArrayList<>();
        subs.add(Submarkets.SUBMARKET_OPEN);
        // subs.add(Submarkets.SUBMARKET_STORAGE);
        subs.add(Submarkets.SUBMARKET_BLACK);
        // if (AlkemiaModPlugin.hasRoider) {
        // subs.add("roider_resupplyMarket");
        // }
        return subs;

    }

    private List<String> getStationIndustries() {
        List<String> industries = new ArrayList<String>();
        industries.add(Industries.POPULATION);
        industries.add(Industries.SPACEPORT);
        industries.add(Industries.ORBITALSTATION);
        industries.add(Industries.PATROLHQ);
        industries.add(Industries.MINING);

        return industries;
    }

    public static void addKriegMarket() {
        SectorEntityToken krieg = Global.getSector().getEntityById(AlkemiaIds.KRIEG_PLANET);
        MarketAPI market = Helpers.addMarketplace("krieg", krieg, null, "Krieg", 7, getPlanetConditions(),
                getPlanetIndustries(),
                getPlanetSubmarkets(), 0f, false);
        market.setHidden(false);

        Global.getSector().getMemory().set(AlkemiaIds.KEY_KRIEG_REVEALED, true);
        krieg.setDiscoverable(false);
        // market.addTag(Tags.NO_MARKET_INFO);
    }

    private static List<String> getPlanetSubmarkets() {
        List<String> subs = new ArrayList<>();
        subs.add("krieg_surplus");
        // subs.add(Submarkets.SUBMARKET_OPEN);
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
        conditions.add("alkemia_ionized_atmosphere");

        conditions.add(Conditions.VOLATILES_DIFFUSE);
        conditions.add(Conditions.ORE_ABUNDANT);
        conditions.add(Conditions.FARMLAND_POOR);
        conditions.add(Conditions.ORGANICS_COMMON);

        conditions.add(Conditions.POPULATION_7);
        conditions.add(Conditions.RURAL_POLITY);
        conditions.add(Conditions.DISSIDENT);
        conditions.add(Conditions.SOLAR_ARRAY);
        // conditions.add(Conditions.POLLUTION);

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

    public StarSystemAPI getSystem() {
        return system;
    }
}
