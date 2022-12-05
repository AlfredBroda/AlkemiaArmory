package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import data.scripts.AlkemiaIds;
import data.scripts.plugins.dialog.KriegInteractionDialogPlugin;

public class KriegCampaignPlugin extends BaseCampaignPlugin {

	public String getId() {
		return "krieg_campaign_plugin";
	}

	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken target) {
		if (target.hasTag(AlkemiaIds.TAG_KRIEG_DIALOG)) {
			return new PluginPick<InteractionDialogPlugin>(new KriegInteractionDialogPlugin(),
					PickPriority.MOD_SPECIFIC);
		}
		return null;
	}

	public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
		boolean inAtmosphere = Global.getSector().getMemoryWithoutUpdate().getBoolean(AlkemiaIds.KEY_ATMOSPHERIC);
		if (inAtmosphere) {
			return new PluginPick<BattleCreationPlugin>(new KriegAtmosphericCreatorPlugin(), PickPriority.MOD_SPECIFIC);
		}
		return null;
	}
}
