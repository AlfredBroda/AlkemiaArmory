package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
// import org.dark.shaders.light.LightData;
// import org.dark.shaders.util.ShaderLib;
// import org.dark.shaders.util.TextureData;

public class AlkemiaModPlugin extends BaseModPlugin
{
    public static final String SETTINGS_FILE = "alkemia_options.ini";

    public static final boolean isExerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
    public static boolean versionCheckerNag;
    public static boolean vcNagDone = false;

    @Override
    public void onApplicationLoad()
    {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib)
        {
            throw new RuntimeException("Alkemia Armoury requires LazyLib!"
                    + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
        }

        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib)
        {
            throw new RuntimeException("Alkemia Armoury requires MagicLib!"
                    + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718");
        }

        // boolean hasShaderLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        // if (!hasShaderLib)
        // {
        //     throw new RuntimeException("Alkemia Armoury requires GraphicsLib!"
        //             + "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=10982");
        // }
        // else
        // {
        //     initShaderLib();
        // }

    }

    // static void initShaderLib()
    // {
    //     ShaderLib.init();
        // LightData.readLightDataCSV("data/lights/tiandong_light_data.csv");
        // TextureData.readTextureDataCSV("data/lights/tiandong_texture_data.csv");
    // }

    @Override
    public void onGameLoad(boolean newGame)
    {
        Global.getLogger(this.getClass()).info("On game load");
    }

    @Override
    public void onNewGame()
    {
        Global.getLogger(this.getClass()).info("On new game");
    }

    @Override
    public void onNewGameAfterEconomyLoad()
    {
    }
}
