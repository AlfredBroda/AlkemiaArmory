package data.scripts.plugins;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.PlanetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.OptionId;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Drops;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.util.Misc;

import data.scripts.AlkemiaIds;
import data.scripts.tools.KriegDefenderGen;
import data.scripts.world.KriegGen;
import data.scripts.world.systems.Relic;

public class KriegInteractionDialogPlugin implements InteractionDialogPlugin {

	private static final int SURVEY_CREW = 40;
	private static final int SURVEY_AMCHINERY = 10;
	private static final int SURVEY_SUPPLIES = 20;

	private static class Options {
		public static String INIT = "init";
		public static String SURVEY = "survey";
		public static String SURVEY_DONE = "survey_done";
		public static String SALVAGE = "salvage";
		public static String COMBAT = "combat";
		public static String MARKET = "market";
		public static String LEAVE = "leave";
	}

	protected InteractionDialogPlugin originalPlugin;
	protected Map<String, MemoryAPI> memoryMap;

	private InteractionDialogAPI dialog;
	private TextPanelAPI textPanel;
	private OptionPanelAPI options;
	private VisualPanelAPI visual;

	private SectorEntityToken planet;
	private SectorAPI sector;
	private CampaignFleetAPI playerFleet;

	private boolean inCombat = false;

	// Getting the color from settings.json
	private static final Color HIGHLIGHT_COLOR = Global.getSettings().getColor("buttonShortcut");

	private final Logger log;

	public KriegInteractionDialogPlugin() {
		sector = Global.getSector();
		playerFleet = Global.getSector().getPlayerFleet();

		log = Global.getLogger(getClass());
		log.info("construct");
	}

	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		textPanel = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		visual = dialog.getVisualPanel();

		planet = dialog.getInteractionTarget();

		visual.setVisualFade(0.25f, 0.25f);

		if (planet.getCustomInteractionDialogImageVisual() != null && isKriegRevealed()) {
			visual.showImageVisual(planet.getCustomInteractionDialogImageVisual());
		} else {
			visual.showImageVisual(new InteractionDialogImageVisual("illustrations", "above_clouds", 640, 400));
		}

		dialog.setOptionOnEscape("Leave", Options.LEAVE);

		optionSelected(null, Options.INIT);
	}

	public void backFromEngagement(EngagementResultAPI result) {
		updateOptions();
	}

	public void optionSelected(String text, Object optionData) {
		if (optionData == null)
			return;

		String option = (String) optionData;

		if (text != null) {
			textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
		}

		if (option == Options.INIT) {
			addText("Your fleet enters orbit around the planet.");

			Description desc = Global.getSettings().getDescription(planet.getCustomDescriptionId(), Type.CUSTOM);
			addText(desc.getText1FirstPara());

			addText("You watch the roiling clouds from the bridge of you flagship. Most of your fleet's Survey equipment is rendered useless by the strong magnetic field of the planet. From time to time vast structures can be seem on the surface, clearly indicating the presence of some previous habitation.");


			if (isKriegRevealed()) {
				addText("You've already surveyed this planet.");
			}
			updateOptions();
		} else if (option == Options.SURVEY_DONE) {
			addText("You send down a survey team and just after breaching the cloud cover they report spotting numerous signs of habitation!");
			FactionAPI krieg = Global.getSector().getFaction(AlkemiaIds.FACTION_KRIEG);
			addText(String.format(
					"Shortly the team also reports being intercepted and forced to land by unidentified airplanes. They are informed that %s is in control of this planet's airspace, which they have violated.",
					krieg.getDisplayNameLongWithArticle()));
			textPanel.highlightFirstInLastPara(krieg.getDisplayNameLongWithArticle(), krieg.getColor());

			revealKrieg();

			updateOptions();
		} else if (option == Options.SURVEY) {
			options.clearOptions();

			CargoAPI cargo = playerFleet.getCargo();
			boolean surveyAvailable = cargo.getCommodityQuantity(Commodities.CREW) >= SURVEY_CREW
					&& cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY) >= SURVEY_AMCHINERY
					&& cargo.getCommodityQuantity(Commodities.SUPPLIES) >= SURVEY_SUPPLIES;

			Misc.showCost(textPanel, "Resources: consumed (available)", true, -1f, null, null,
					new String[] { Commodities.CREW, Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES },
					new int[] { SURVEY_CREW, SURVEY_AMCHINERY, SURVEY_SUPPLIES },
					new boolean[] { false, true, true });

			options.addOption("Confirm", Options.SURVEY_DONE);
			options.setEnabled(Options.SURVEY_DONE, surveyAvailable);
			if (surveyAvailable)
				options.setTooltip(Options.SURVEY_DONE, "Assemble the survey team and send them to the surface");
			else
				options.setTooltip(Options.SURVEY_DONE, "Your cargo contains insufficient resources to perform a Survey");

			options.addOption("Leave", Options.LEAVE);
		} else if (option == Options.SALVAGE) {
			options.clearOptions();

			addText("As your fleet descends through the thick cloud cover, proximity alarms begin sounding.");
			addText("A large formation of airplanes is headed on an intercept course! They transmit no IFF codes and appear to be quite Low Tech from what you officers report.");

			options.addOption("Continue", Options.COMBAT);
			inCombat = true;
		} else if (option == Options.COMBAT) {
			if (inCombat)
				startEngagement();

			updateOptions();
		} else if (option == Options.MARKET) {
			startMarketInteraction();
		} else if (option == Options.LEAVE) {
			Global.getSector().setPaused(false);
			dialog.dismiss();
		}
	}

	private void startMarketInteraction() {
		InteractionDialogPlugin planetPlugin = new PlanetInteractionDialogPluginImpl();

		dialog.setPlugin(planetPlugin);
		planetPlugin.init(dialog);
	}

	private boolean isKriegRevealed() {
		return sector.getMemoryWithoutUpdate().getBoolean(AlkemiaIds.KEY_KRIEG_REVEALED);
	}

	private void revealKrieg() {
		if (!isKriegRevealed()) {
			Relic.addKriegMarket();
			KriegGen.addKriegAdmin();

			visual.showImageVisual(planet.getCustomInteractionDialogImageVisual());
		}

		sector.getMemory().set(AlkemiaIds.KEY_KRIEG_REVEALED, true);
	}

	private void updateOptions() {
		options.clearOptions();

		if (isKriegRevealed()) {
			options.addOption("Trade", Options.MARKET,
					"Land on the surface and begin trade");
		} else {
			options.addOption("Search Ruins", Options.SALVAGE,
					"Land on the surface to begin salvage operations");
			options.addOption("Survey Planet", Options.SURVEY,
					"Send a survey team to the planet");
		}

		options.addOption("Leave", Options.LEAVE, null);
	}

	private void startEngagement() {
		revealKrieg();

		final MemoryAPI memory = planet.getMemoryWithoutUpdate();
		final CampaignFleetAPI ambushers = KriegDefenderGen.getFleetForPlanet(planet, AlkemiaIds.FACTION_KRIEG, "Stratospheric Patrol");

		dialog.setInteractionTarget(ambushers);

		final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
		config.leaveAlwaysAvailable = false;
		config.showCommLinkOption = false;
		config.showEngageText = true;
		config.showFleetAttitude = false;
		config.showTransponderStatus = true;
		config.showWarningDialogWhenNotHostile = false;
		config.alwaysAttackVsAttack = true;
		config.impactsAllyReputation = false;
		config.impactsEnemyReputation = false;
		config.pullInAllies = true;
		config.pullInEnemies = false;
		config.pullInStations = false;
		config.lootCredits = false;

		config.firstTimeEngageOptionText = "Engage the ambushers";
		config.afterFirstTimeEngageOptionText = "Re-engage the ambushers";
		config.noSalvageLeaveOptionText = "Continue";

		config.dismissOnLeave = false;
		config.printXPToDialog = true;

		long seed = memory.getLong(MemFlags.SALVAGE_SEED);
		config.salvageRandom = Misc.getRandom(seed, 75);

		final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);
		final SectorEntityToken baseEntity = planet;
		final InteractionDialogPlugin originalPlugin = this;

		config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
			@Override
			public void notifyLeave(InteractionDialogAPI dialog) {
				// nothing in there we care about keeping; clearing to reduce savefile size
				ambushers.getMemoryWithoutUpdate().clear();
				// there's a "standing down" assignment given after a battle is finished that we don't care about
				ambushers.clearAssignments();
				ambushers.deflate();

				dialog.setInteractionTarget(baseEntity);

				// Global.getSector().getCampaignUI().clearMessages();

				if (plugin.getContext() instanceof FleetEncounterContext) {
					FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
					if (context.didPlayerWinEncounterOutright()) {
						SalvageGenFromSeed.SDMParams p = new SalvageGenFromSeed.SDMParams();
						p.entity = baseEntity;
						p.factionId = ambushers.getFaction().getId();

						SalvageGenFromSeed.SalvageDefenderModificationPlugin plugin = Global.getSector()
								.getGenericPlugins().pickPlugin(
										SalvageGenFromSeed.SalvageDefenderModificationPlugin.class, p);
						if (plugin != null) {
							plugin.reportDefeated(p, baseEntity, ambushers);
						}

						memory.unset("$hasDefenders");
						memory.unset("$defenderFleet");
						memory.set("$defenderFleetDefeated", true);
						baseEntity.removeScriptsOfClass(FleetAdvanceScript.class);

						dialog.setPlugin(originalPlugin);
						originalPlugin.optionSelected(null, Options.MARKET);
					} else {
						boolean persistDefenders = false;
						if (context.isEngagedInHostilities()) {
							persistDefenders |= !Misc.getSnapshotMembersLost(ambushers).isEmpty();

							for (FleetMemberAPI member : ambushers.getFleetData().getMembersListCopy()) {
								if (member.getStatus().needsRepairs()) {
									persistDefenders = true;
									break;
								}
							}
						}

						if (persistDefenders) {
							if (!baseEntity.hasScriptOfClass(FleetAdvanceScript.class)) {
								ambushers.setDoNotAdvanceAI(true);
								ambushers.setContainingLocation(baseEntity.getContainingLocation());
								// somewhere far off where it's not going to be in terrain or whatever
								ambushers.setLocation(1000000, 1000000);
								baseEntity.addScript(new FleetAdvanceScript(ambushers));
							}
							// defenders may have gotten damaged; persist them for a bit
							memory.expire("$defenderFleet", 10);

							if (baseEntity instanceof PlanetAPI)
								baseEntity.getMemoryWithoutUpdate().set("$defenderFleet", ambushers, 10f);
						}
						dialog.dismiss();
					}
				} else {
					dialog.dismiss();
				}
			}

			@Override
			public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
				bcc.aiRetreatAllowed = true;
				bcc.objectivesAllowed = false;
				bcc.enemyDeployAll = true;
			}

			@Override
			public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context,
					CargoAPI salvage) {
				FleetEncounterContextPlugin.DataForEncounterSide winner = context.getWinnerData();
				FleetEncounterContextPlugin.DataForEncounterSide loser = context.getLoserData();

				if (winner == null || loser == null)
					return;

				float playerContribMult = context.computePlayerContribFraction();

				List<SalvageEntityGenDataSpec.DropData> dropRandom = new ArrayList<SalvageEntityGenDataSpec.DropData>();
				List<SalvageEntityGenDataSpec.DropData> dropValue = new ArrayList<SalvageEntityGenDataSpec.DropData>();

				float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic()
						.getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
				float valueModShips = context.getSalvageValueModPlayerShips();

				for (FleetEncounterContextPlugin.FleetMemberData data : winner.getEnemyCasualties()) {
					if (data.getMember() != null && context.getBattle() != null) {
						CampaignFleetAPI fleet = context.getBattle().getSourceFleet(data.getMember());

						if (fleet != null &&
								fleet.getFaction()
										.getCustomBoolean(Factions.CUSTOM_NO_AI_CORES_FROM_AUTOMATED_DEFENSES)) {
							continue;
						}
					}
					if (config.salvageRandom.nextFloat() < playerContribMult) {
						SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
						drop.chances = 1;
						drop.value = -1;
						switch (data.getMember().getHullSpec().getHullSize()) {
							case CAPITAL_SHIP:
								drop.group = Drops.WEAPONS2;
								drop.chances = 2;
								break;
							case CRUISER:
								drop.group = Drops.WEAPONS1;
								break;
							case DESTROYER:
								drop.group = Drops.LOW_WEAPONS2;
								break;
							case FIGHTER:
							case FRIGATE:
								drop.group = Drops.LOW_WEAPONS1;
								break;
							case DEFAULT:
								drop.group = Drops.BASIC;
						}
						if (drop.group != null) {
							dropRandom.add(drop);
						}
					}
				}

				float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic()
						.getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);
				// float fuel = salvage.getFuel();
				// salvage.addFuel((int) Math.round(fuel * fuelMult));

				CargoAPI extra = SalvageEntity.generateSalvage(config.salvageRandom, valueMultFleet + valueModShips, 1f,
						1f, fuelMult, dropValue, dropRandom);
				for (CargoStackAPI stack : extra.getStacksCopy()) {
					if (stack.isFuelStack()) {
						stack.setSize((int) (stack.getSize() * fuelMult));
					}
					salvage.addFromStack(stack);
				}
			}
		};

		log.info("Begin combat");
		dialog.setPlugin(plugin);
		plugin.otherFleetWantsToFight(true);
		plugin.init(dialog);

		plugin.optionSelected(null, OptionId.INITIATE_BATTLE);
	}

	private void addText(String text) {
		textPanel.addParagraph(text);
	}

	public void advance(float amount) {
	}

	public Object getContext() {
		return null;
	}

	@Override
	public Map<String, MemoryAPI> getMemoryMap() {
		return memoryMap;
	}

	@Override
	public void optionMousedOver(String optionText, Object optionData) {
	}
}
