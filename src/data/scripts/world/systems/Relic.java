package data.scripts.world.systems;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
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

    private static final int MAX_ARRAY = 12;

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

        OrbitGap kriegOrbit = Helpers.findEmptyOrbit(system, 5000, 7000, 1400);

        PlanetAPI kriegPlanet = system.addPlanet(
                AlkemiaIds.KRIEG_PLANET,
                star,
                "Krieg",
                "terran-eccentric",
                360f * (float) Math.random(),
                130,
                Helpers.getMiddle(kriegOrbit),
                260);
        kriegPlanet.setInteractionImage("illustrations", "dieselpunk_city");
        kriegPlanet.setCustomDescriptionId("krieg_planet_desc");
        kriegPlanet.addTag(AlkemiaIds.TAG_KRIEG_DIALOG);

        OrbitGap beaconGap = new OrbitGap();
        beaconGap.start = kriegPlanet.getRadius() + 180f;
        beaconGap.end = 600;
        // Misc.addWarningBeacon(kriegPlanet, beaconGap, Tags.BEACON_LOW);

        Helpers.addMagneticField(kriegPlanet, 0.2f, 180, false);
        PlanetConditionGenerator.generateConditionsForPlanet(kriegPlanet, system.getAge());
        reapplyConditions(kriegPlanet, getPlanetConditions());
        Helpers.makeDiscoverable(kriegPlanet, 5000, 2000, 500);

        MarketAPI tempMarket = kriegPlanet.getMarket();
        if (tempMarket != null) {
            Helpers.addCondition(tempMarket, "alkemia_ionized_atmosphere");
            boolean hadArray = Helpers.addCondition(tempMarket, Conditions.SOLAR_ARRAY);
            if (!hadArray) {
                int arraySize = MathUtils.getRandomNumberInRange(3, 6);
                int shades = Math.round(arraySize / 3);
                int mirrors = arraySize - shades;
                if (kriegPlanet.hasCondition(Conditions.POOR_LIGHT)) {
                    shades = 0;
                    mirrors = arraySize;
                } else if (kriegPlanet.hasCondition(Conditions.HOT)) {
                    mirrors = Math.round(arraySize / 3);
                    shades = arraySize - mirrors;
                    ;
                } else if (kriegPlanet.hasCondition(Conditions.VERY_HOT)) {
                    mirrors = 0;
                    shades = arraySize;
                }
                addSolarArray(kriegPlanet, beaconGap.start, shades, mirrors);
            }
        }

        PlanetAPI kriegMoon = system.addPlanet("krieg_moon", kriegPlanet, "Sentinel", "barren-bombarded",
                360f * (float) Math.random(), 30, beaconGap.end, 20);
        Helpers.makeDiscoverable(kriegMoon, 3000, 800, 100);
        PlanetConditionGenerator.generateConditionsForPlanet(kriegMoon, system.getAge());

        // Place the belt behind the planet
        OrbitGap beltOrbit = Helpers.findEmptyOrbit(system, kriegOrbit.end, kriegOrbit.end + 5000, 1400);

        system.addAsteroidBelt(
                star,
                1500,
                Helpers.getMiddle(beltOrbit),
                800,
                400,
                600,
                Terrain.ASTEROID_BELT,
                "The Line");

        SectorEntityToken pirateStation = system.addCustomEntity(AlkemiaIds.KRIEG_BURROW,
                "Hemera Station", "station_burrow", Factions.PIRATES);
        pirateStation.setCircularOrbitPointingDown(star, 360f * (float) Math.random(), Helpers.getMiddle(beltOrbit),
                500);
        pirateStation.setCustomDescriptionId("krieg_burrow_station_desc");
        Helpers.makeDiscoverable(pirateStation, 2000, 1, 200);
        pirateStation.addTag(Tags.STATION);

        MarketAPI pirateMarket = Helpers.addMarketplace(Factions.PIRATES, pirateStation, null, "Hemera Station", 3,
                getStationConditions(), getStationIndustries(), getStationSubmarkets(), 0f, false);
        pirateMarket.setHidden(true);

        // pirateMarket.addTag(Tags.NO_MARKET_INFO);

        Global.getSector().getMemory().set(AlkemiaIds.KEY_KRIEG_EXISTS, true);
    }

    /**
     * @param planet
     * @param conditions
     */
    private void reapplyConditions(PlanetAPI planet, List<String> conditions) {
        MarketAPI market = planet.getMarket();
        if (market != null) {
            for (String cond : conditions) {
                market.reapplyCondition(cond);
            }
        }
    }

    /**
     * @param planet
     * @param orbit
     * @param numShades
     * @param numMirrors
     */
    private void addSolarArray(PlanetAPI planet, float orbitRadius, int numShades, int numMirrors) {
        int maxMirrors = numMirrors;
        int maxShades = numShades;
        if ((numMirrors + numShades) > MAX_ARRAY) {
            float mirrorPerc = maxMirrors / (numMirrors + numShades);
            maxMirrors = Math.round(MAX_ARRAY * mirrorPerc);
            maxShades = MAX_ARRAY - maxMirrors;
        }

        float orbitDays = planet.getCircularOrbitPeriod();
        float baseAngle = planet.getCircularOrbitAngle();
        // TODO: Consider using a light source
        // SectorEntityToken sun = planet.getLightSource();
        // float baseAngle = Misc.getAngleInDegrees(planet.getLocation(),
        // sun.getLocation());
        float orbitAngle = 0;

        CustomEntitySpecAPI mirror = Global.getSettings().getCustomEntitySpec("stellar_mirror");
        for (int i = 0; i < maxMirrors; i++) {
            orbitAngle = MathUtils.clampAngle(baseAngle + Helpers.evenSpreadAngle(120f, i, maxMirrors));
            createArrayElement(planet, mirror, i, orbitAngle, orbitRadius, orbitDays);
        }

        CustomEntitySpecAPI shade = Global.getSettings().getCustomEntitySpec("stellar_shade");
        baseAngle += 180;
        for (int j = 0; j < maxShades; j++) {
            orbitAngle = MathUtils.clampAngle(baseAngle + Helpers.evenSpreadAngle(90f, j, maxShades));
            createArrayElement(planet, shade, j, orbitAngle, orbitRadius, orbitDays);
        }
    }

    /**
     * @param planet
     * @param entity
     * @param seqNum
     * @param orbitAngle
     * @param orbitRadius
     * @param orbitDays
     */
    private void createArrayElement(SectorEntityToken planet, CustomEntitySpecAPI entity, int seqNum, float orbitAngle,
            float orbitRadius, float orbitDays) {
        String id = String.format("%s_%s_%d", planet.getId(), entity.getId(), seqNum);
        String displayName = String.format("%s %s %s", planet.getName(), entity.getDefaultName(),
                Helpers.getGreek(seqNum + 1));

        SectorEntityToken token = system.addCustomEntity(id, displayName, entity.getId(), Factions.NEUTRAL);
        token.setCircularOrbitPointingDown(planet, orbitAngle, orbitRadius, orbitDays);
        token.setCustomDescriptionId(entity.getCustomDescriptionId());
        Helpers.makeDiscoverable(token, 2000, 30, 100);
    }

    private List<String> getStationConditions() {
        ArrayList<String> conditions = new ArrayList<>();
        conditions.add(Conditions.NO_ATMOSPHERE);

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
        MarketAPI market = Helpers.addMarketplace("krieg", krieg, null, "Krieg", 6, getPlanetConditions(),
                getPlanetIndustries(),
                getPlanetSubmarkets(), 0f, false);
        market.setHidden(false);
        market.setPlanetConditionMarketOnly(false);
        Helpers.revalidateConditions(market);
        market.reapplyConditions();

        Helpers.setSurveyed(market);

        Global.getSector().getMemory().set(AlkemiaIds.KEY_KRIEG_REVEALED, true);
        krieg.setDiscoverable(false);
        // market.addTag(Tags.NO_MARKET_INFO);
    }

    private static List<String> getPlanetSubmarkets() {
        List<String> subs = new ArrayList<>();
        subs.add("krieg_surplus");
        // subs.add(Submarkets.SUBMARKET_OPEN);
        subs.add(Submarkets.SUBMARKET_STORAGE);
        subs.add(Submarkets.GENERIC_MILITARY);
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

        conditions.add(Conditions.POPULATION_6);
        conditions.add(Conditions.RURAL_POLITY);
        conditions.add(Conditions.DISSIDENT);
        conditions.add(Conditions.SOLAR_ARRAY);

        return conditions;
    }

    private static List<String> getPlanetIndustries() {
        List<String> industries = new ArrayList<String>();
        industries.add("krieg_airbase");
        industries.add(Industries.POPULATION);
        industries.add(Industries.FARMING);
        industries.add(Industries.MINING);
        industries.add(Industries.HEAVYBATTERIES);
        industries.add(Industries.MILITARYBASE);
        // industries.add(Industries.HIGHCOMMAND);
        if (AlkemiaModPlugin.hasIndEvo) {
            industries.add("IndEvo_Ruins");
            // industries.add("IndEvo_RuinedInfra");
            industries.add("IndEvo_AdManuf");
        } else {
            // industries.add(Industries.FUELPROD);
            industries.add(Industries.LIGHTINDUSTRY);
            // industries.add(Industries.HEAVYINDUSTRY);
        }
        return industries;
    }

    public StarSystemAPI getSystem() {
        return system;
    }
}
