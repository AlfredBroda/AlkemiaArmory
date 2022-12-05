package data.scripts.plugins;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
// import com.fs.starfarer.combat.CombatEngine;

import data.scripts.tools.Helpers;

public class KriegAtmosphericCreatorPlugin extends BattleCreationPluginImpl {

    protected BattleCreationContext context;
    protected MissionDefinitionAPI loader;

    private Logger log;

    @Override
    public void initBattle(final BattleCreationContext context, MissionDefinitionAPI api) {
        this.context = context;
        this.loader = api;

        log = Global.getLogger(getClass());
        log.info("initBattle()");

        CampaignFleetAPI playerFleet = context.getPlayerFleet();
        CampaignFleetAPI otherFleet = context.getOtherFleet();
        // FleetGoal playerGoal = context.getPlayerGoal();
        FleetGoal playerGoal = FleetGoal.ESCAPE;
        // FleetGoal enemyGoal = context.getEnemyGoal();
        FleetGoal enemyGoal = FleetGoal.ATTACK;

        Random random = Misc.getRandom(Misc.getSalvageSeed(otherFleet) *
                (long) otherFleet.getFleetData().getNumMembers(), 23);

        escape = playerGoal == FleetGoal.ESCAPE || enemyGoal == FleetGoal.ESCAPE;

        int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");

        loader.initFleet(FleetSide.PLAYER, "ISS", playerGoal, false,
                context.getPlayerCommandPoints() - baseCommandPoints,
                (int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
        loader.initFleet(FleetSide.ENEMY, "", enemyGoal, true,
                (int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);

        List<FleetMemberAPI> playerShips = playerFleet.getFleetData().getCombatReadyMembersListCopy();
        if (playerGoal == FleetGoal.ESCAPE) {
            playerShips = playerFleet.getFleetData().getMembersListCopy();
        }
        for (FleetMemberAPI member : playerShips) {
            loader.addFleetMember(FleetSide.PLAYER, member);
        }

        List<FleetMemberAPI> enemyShips = otherFleet.getFleetData().getCombatReadyMembersListCopy();
        if (enemyGoal == FleetGoal.ESCAPE) {
            enemyShips = otherFleet.getFleetData().getMembersListCopy();
        }
        for (FleetMemberAPI member : enemyShips) {
            loader.addFleetMember(FleetSide.ENEMY, member);
        }

        width = 12000f;
        height = 18000f;

        createMap(random);

        context.setInitialDeploymentBurnDuration(1.5f);
        context.setNormalDeploymentBurnDuration(6f);
        context.setEscapeDeploymentBurnDuration(1.5f);

        if (escape) {
            api.addObjective((float) width * 0.5f, (float) height * 0.5f, "nav_buoy");
            api.addObjective((float) width * 0.5f, (float) height * 0.9f, "nav_buoy");    

            context.setInitialEscapeRange(Global.getSettings().getFloat("escapeStartDistance"));
            context.setFlankDeploymentDistance(Global.getSettings().getFloat("escapeFlankDistance"));

            loader.addPlugin(new EscapeRevealPlugin(context));
        }
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
    }

    @Override
    protected void createMap(Random random) {
        log.info("createMap()");
        // Generate map
        loader.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        String[] backdrops = {
                "graphics/alkemia/backgrounds/fields1.jpg",
                // "graphics/alkemia/backgrounds/desert1.jpg",
                // "graphics/alkemia/backgrounds/desert2.jpg",
                "graphics/alkemia/backgrounds/ocean1.jpg",
                "graphics/alkemia/backgrounds/ocean2.jpg",
                "graphics/alkemia/backgrounds/basin.jpg"
        };

        String randomBackground = Helpers.getRandomElement(backdrops);
        loader.setBackgroundSpriteName(randomBackground);
        // CombatEngine.replaceBackground(randomBackground, false);

        loader.setNebulaTex(Global.getSettings().getSpriteName("terrain", "nebula_clouds"));

        for (int i = 0; i < 15; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 1000f;
            loader.addNebula(x, y, radius);
        }
    }
}
