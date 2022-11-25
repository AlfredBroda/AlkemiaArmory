package data.scripts.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import data.scripts.AlkemiaIds;

public class KriegCampaignPlugin extends BaseCampaignPlugin {

	public String getId() {
		return "krieg_campaign_plugin";
	}

	// If marked as not transient this plugin would be persisted in a savegame
	public boolean isTransient() {
		return true;
	}

	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
		String id = interactionTarget.getId();
		if (interactionTarget instanceof PlanetAPI && id == AlkemiaIds.KRIEG_PLANET) {
			return new PluginPick<InteractionDialogPlugin>(new KriegInteractionDialogPlugin(),
					PickPriority.MOD_GENERAL);
		}
		return null;
	}
}
