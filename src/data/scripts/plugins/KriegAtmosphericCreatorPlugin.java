package data.scripts.plugins;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

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

        List<String> backdrops = new ArrayList<>();
        backdrops.add(getBackgroundSprite("fields1"));
        backdrops.add(getBackgroundSprite("basin1"));
        backdrops.add(getBackgroundSprite("desert1"));
        backdrops.add(getBackgroundSprite("desert2"));
        backdrops.add(getBackgroundSprite("ocean1"));
        backdrops.add(getBackgroundSprite("ocean2"));

        loader.setBackgroundSpriteName(Helpers.getRandomElement(backdrops));

        loader.setNebulaTex(Global.getSettings().getSpriteName("terrain", "nebula_clouds"));
        loader.setBackgroundGlowColor(Color.BLUE);
        
        for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width / 2;
			float y = (float) Math.random() * height - height / 2;
			float radius = 100f + (float) Math.random() * 1000f;
			loader.addNebula(x, y, radius);
		}
    }

    private String getBackgroundSprite(String name) {
        return Global.getSettings().getSpriteName("backgrounds", name);
    }
}
