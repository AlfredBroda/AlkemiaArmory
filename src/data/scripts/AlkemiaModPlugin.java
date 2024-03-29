package data.scripts;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin.InstallableItemDescriptionMode;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.plugins.AlkemiaRiggerPlugin;
import data.plugins.AntiMissileEffectPlugin;
import data.plugins.FuelBombEffectPlugin;
import data.scripts.ai.drone.RepairDroneAI;
import data.scripts.ai.missile.AlkemiaEmpMissileAI;
import data.scripts.ai.missile.AlkemiaFuelbombAI;
import data.scripts.ai.missile.AlkemiaPDMissileAI;
import data.scripts.plugins.KriegCampaignPlugin;
import data.scripts.world.KriegGen;
// import exerelin.campaign.SectorManager;

// import org.dark.shaders.light.LightData;
// import org.dark.shaders.util.ShaderLib;
// import org.dark.shaders.util.TextureData;

public class AlkemiaModPlugin extends BaseModPlugin {
    private static final ModManagerAPI MOD_MANAGER = Global.getSettings().getModManager();

    public static final String SETTINGS_FILE = "alkemia_options.ini";

    public static final boolean hasExerelin = MOD_MANAGER.isModEnabled("nexerelin");
    public static final boolean hasIndEvo = MOD_MANAGER.isModEnabled("IndEvo");
    public static final boolean hasStarshipLegends = MOD_MANAGER.isModEnabled("sun_starship_legends");
    public static final boolean hasRoider = MOD_MANAGER.isModEnabled("roider");

    private static boolean DEBUG = false;

    private static Logger log = Global.getLogger(AlkemiaModPlugin.class);

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = MOD_MANAGER.isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Alkemia Armoury requires LazyLib!"
                    + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
        }

        boolean hasMagicLib = MOD_MANAGER.isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("Alkemia Armoury requires MagicLib!"
                    + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718");
        }

        // boolean hasShaderLib =
        // Global.getSettings().getModManager().isModEnabled("shaderLib");
        // if (!hasShaderLib)
        // {
        // throw new RuntimeException("Alkemia Armoury requires GraphicsLib!"
        // + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=10982");
        // }
        // else
        // {
        // initShaderLib();
        // }

        log.debug("Alkemia onApplicationLoad()");
    }

    // static void initShaderLib()
    // {
    // ShaderLib.init();
    // LightData.readLightDataCSV("data/lights/alkemia_light_data.csv");
    // TextureData.readTextureDataCSV("data/lights/alkemia_texture_data.csv");
    // }

    public void info(String mesg) {
        log.info(mesg);
    }

    public void info(String format, Object... args) {
        log.info(String.format(format, args));
    }

    public void warn(String mesg) {
        log.warn(mesg);
    }

    public void debug(String mesg) {
        if (!log.isDebugEnabled() && DEBUG) {
            log.info(mesg);
        } else {
            log.debug(mesg);
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        // FIXME: Is this correct?
        ItemEffectsRepo.ITEM_EFFECTS.put(AlkemiaIds.ALKEMIA_HULLMOD_NANOFORGE, new BoostIndustryInstallableItemEffect(
                AlkemiaIds.ALKEMIA_HULLMOD_NANOFORGE, AlkemiaStats.ALKEMIA_NANOFORGE_PROD, 0) {
            public void apply(Industry industry) {
                super.apply(industry);
                industry.getMarket().getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                        .modifyFlat("nanoforge", AlkemiaStats.ALKEMIA_NANOFORGE_QUALITY,
                                Misc.ucFirst(spec.getName().toLowerCase()));
            }

            public void unapply(Industry industry) {
                super.unapply(industry);
                industry.getMarket().getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                        .unmodifyFlat("nanoforge");
            }

            protected void addItemDescriptionImpl(Industry industry, TooltipMakerAPI text, SpecialItemData data,
                    InstallableItemDescriptionMode mode, String pre, float pad) {
                String industryName = "starship workshop's ";
                if (mode == InstallableItemDescriptionMode.MANAGE_ITEM_DIALOG_LIST) {
                    industryName = "";
                }
                text.addPara(pre + "Increases ship production quality by %s. " +
                        "Increases " + industryName + "production by %s unit." +
                        "Removes demand for spare machinery.",
                        pad, Misc.getHighlightColor(),
                        "" + (int) Math.round(AlkemiaStats.ALKEMIA_NANOFORGE_QUALITY * 100f) + "%",
                        "" + (int) AlkemiaStats.ALKEMIA_NANOFORGE_PROD);
            }
        });

        Global.getSector().addTransientListener(new ReportPlayerEngagementCampaignEventListener());

        AntiMissileEffectPlugin.cleanSlate();
        FuelBombEffectPlugin.cleanSlate();

        // Allows creation in existing games
        if (!kriegPlanetExists()) {
            new KriegGen().generate(Global.getSector());
            KriegGen.addBurrowAdmin();
        }
        Global.getSector().registerPlugin(new KriegCampaignPlugin());
        Global.getSector().getMemory().set(AlkemiaIds.KEY_ATMOSPHERIC, false);
    }

    private boolean kriegPlanetExists() {
        SectorEntityToken planet = Global.getSector().getEntityById(AlkemiaIds.KRIEG_PLANET);
        if (planet != null) {
            log.debug(planet.getFullName() + " in " + planet.getContainingLocation().getName());
            return true;
        }
        return false;
    }

    @Override
    public void onNewGame() {
        if (!hasExerelin) { // || !SectorManager.getManager().isCorvusMode()) {
            // new AlkemiaGen().generate(Global.getSector());
            new KriegGen().generate(Global.getSector());
            // Global.getSector().addScript(new MS_fleetFighterFinagler());
            return;
        }

        // Global.getSector().addScript(new MS_fleetFighterFinagler());
        // SharedData.getData().getPersonBountyEventData().addParticipatingFaction("shadow_industry");
        // MS_specialItemInitializer.run();

        debug("Alkemia Armoury onNewGame()");
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        new KriegGen().generate(Global.getSector());
        KriegGen.addBurrowAdmin();
    }

    ////////////////////////////////////////
    //                                    //
    //       MISSILES AI OVERRIDES        //
    //                                    //
    ////////////////////////////////////////

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String id = missile.getProjectileSpecId();
        switch (id) {
            case AlkemiaIds.ALKEMIA_SWARM:
                debug(String.format("antiMissileAI assigned for %s", id));
                return new PluginPick<MissileAIPlugin>(new AlkemiaPDMissileAI(missile, launchingShip),
                        CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case AlkemiaIds.ALKEMIA_EMP:
                debug(String.format("AlkemiaEmpMissileAI assigned for %s", id));
                return new PluginPick<MissileAIPlugin>(new AlkemiaEmpMissileAI(missile, launchingShip),
                        CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case AlkemiaIds.KRIEG_FUELBOMB:
                debug(String.format("AlkemiaFuelbombAI assigned for %s", id));
                return new PluginPick<MissileAIPlugin>(new AlkemiaFuelbombAI(missile, launchingShip),
                        CampaignPlugin.PickPriority.MOD_SPECIFIC);

        }
        return super.pickMissileAI(missile, launchingShip);
    }

    ////////////////////////////////////////
    //                                    //
    //          SHIP AI OVERRIDES         //
    //                                    //
    ////////////////////////////////////////

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.isFighter() && ship.getHullSpec() != null && ship.getWing() != null) {
            String id = ship.getHullSpec().getHullId();
            switch (id) {
                case AlkemiaIds.ALKEMIA_REPAIR_DRONE:
                    debug(String.format("RepairDroneAI assigned for %s", id));

                    ShipAPI mothership = ship.getWing().getSourceShip();
                    return new PluginPick<ShipAIPlugin>(new RepairDroneAI(ship, mothership),
                            CampaignPlugin.PickPriority.MOD_SPECIFIC);
            }
        }
        return super.pickShipAI(member, ship);
    }

    private static class ReportPlayerEngagementCampaignEventListener extends BaseCampaignEventListener {
        public ReportPlayerEngagementCampaignEventListener() {
            super(false);
        }

        @Override
        public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {
            clearState();
        }

        @Override
        public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
            clearState();
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            clearState();
        }

        private void clearState() {
            AntiMissileEffectPlugin.cleanSlate();
            FuelBombEffectPlugin.cleanSlate();
            AlkemiaRiggerPlugin.clear();
        }
    }
}
