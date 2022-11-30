package data.scripts.plugins;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import data.scripts.AlkemiaIds;
import data.scripts.plugins.dialog.KriegInteractionDialogPlugin;

public class KriegCampaignPlugin extends BaseCampaignPlugin {

	public String getId() {
		return "krieg_campaign_plugin";
	}

	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
		String id = interactionTarget.getId();
		if (interactionTarget instanceof PlanetAPI && id == AlkemiaIds.KRIEG_PLANET) {
			return new PluginPick<InteractionDialogPlugin>(new KriegInteractionDialogPlugin(),
					PickPriority.MOD_SPECIFIC);
		}
		return null;
	}

	public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
		Logger log = Global.getLogger(getClass());
		log.info(opponent.getName());
		log.info(opponent.getFaction().toString());

		boolean inAtmosphere = Global.getSector().getMemoryWithoutUpdate().getBoolean(AlkemiaIds.KEY_ATMOSPHERIC);
		if (inAtmosphere) {
			log.info("using KriegAtmosphericCreatorPlugin");
			return new PluginPick<BattleCreationPlugin>(new KriegAtmosphericCreatorPlugin(), PickPriority.MOD_SPECIFIC);
		}
		log.info("using null");
		return null;
	}
}
