package data.scripts.plugins;

import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
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

        super.initBattle(context, api);
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
    }

    @Override
    protected void createMap(Random random) {
        log.info("createMap()");
        // Generate map
        float width = 12000f;
        float height = 12000f;
        loader.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        String[] backdrops = {
                "graphics/alkemia/backgrounds/fields1.jpg",
                "graphics/alkemia/backgrounds/desert1.jpg",
                "graphics/alkemia/backgrounds/desert2.jpg",
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
