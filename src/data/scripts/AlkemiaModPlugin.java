package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin.InstallableItemDescriptionMode;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.ai.SCY_antiMissileAI;
import data.scripts.ai.alkemia_SmartMissileAI;
import data.scripts.plugins.SCY_projectilesEffectPlugin;

// import org.dark.shaders.light.LightData;
// import org.dark.shaders.util.ShaderLib;
// import org.dark.shaders.util.TextureData;

public class AlkemiaModPlugin extends BaseModPlugin {
    public static final String SETTINGS_FILE = "alkemia_options.ini";

    public static final boolean isExerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
    public static boolean versionCheckerNag;
    public static boolean vcNagDone = false;

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Alkemia Armoury requires LazyLib!"
                    + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
        }

        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
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

    }

    // static void initShaderLib()
    // {
    // ShaderLib.init();
    // LightData.readLightDataCSV("data/lights/alkemia_light_data.csv");
    // TextureData.readTextureDataCSV("data/lights/alkemia_texture_data.csv");
    // }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getLogger(this.getClass()).info("On game load");

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

        SCY_projectilesEffectPlugin.cleanSlate();
    }

    @Override
    public void onNewGame() {
        Global.getLogger(this.getClass()).info("On new game");
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
    }

    public static final String locust_ID = "alkemia_locust";
    public static final String locustFighter_ID = "alkemia_locust_fighter";
    public static final String harpoon_ID = "alkemia_harpoon_fighter";
    public static final String pilum_pod_ID = "alkemia_pilumpod";
    public static final String pilum_ID = "pilum";

    ////////////////////////////////////////
    // //
    // MISSILES AI OVERRIDES //
    // //
    ////////////////////////////////////////

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (missile.getProjectileSpecId() == locust_ID || missile.getProjectileSpecId() == locustFighter_ID) {
            return new PluginPick<MissileAIPlugin>(new SCY_antiMissileAI(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (missile.getProjectileSpecId() == pilum_ID || missile.getProjectileSpecId() == pilum_pod_ID) {
            return new PluginPick<MissileAIPlugin>(new alkemia_SmartMissileAI(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    public PluginPick<ShipAIPlugin> pickDroneAI(ShipAPI drone,
            ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        return null;
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
            SCY_projectilesEffectPlugin.cleanSlate();
        }
    }

}
