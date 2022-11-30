package data.scripts.plugins.dialog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.RelationshipAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import data.scripts.AlkemiaIds;
import data.scripts.tools.KriegDefenderGen;
import data.scripts.world.KriegGen;
import data.scripts.world.systems.Relic;

public class KriegInteractionDialogPlugin implements InteractionDialogPlugin {

	private static final int SURVEY_CREW = 40;
	private static final int SURVEY_MACHINERY = 10;
	private static final int SURVEY_SUPPLIES = 20;

	private static class Options {
		public static final String INIT = "init";
		public static final String SURVEY = "survey";
		public static final String SURVEY_SENT = "survey_sent";
		public static final String SURVEY_SELECT = "survey_select";
		public static final String SURVEY_COMPLY = "survey_comply";
		public static final String SURVEY_FIGHT = "survey_fight";
		public static final String SURVEY_EXIT = "survey_abort";
		public static final String SEIZED = "seized";
		public static final String SEARCH = "search";
		public static final String COMBAT = "combat";
		public static final String ENCOUNTER_DONE = "encounter_done";
		public static final String MARKET = "market";
		public static final String RECONSIDER = "reconsider";
		public static final String LEAVE = "leave";
		public static final String REPORT = "report";
		public static final String SEIZED_EXCHANGE = "seized_exchange";
		public static final String SEIZED_EXCHANGE_CONFIRM = "seized_exchange_confirm";
		public static final String SEIZED_DEAL = "seized_deal";
		public static final String SEIZED_NEGOTIATE = "seized_negotiate";
		public static final String SURVEY_ESCAPE = "survey_escape";
	}

	protected InteractionDialogPlugin originalPlugin;

	private Map<String, MemoryAPI> memoryMap;

	private InteractionDialogAPI dialog;
	private TextPanelAPI textPanel;
	private OptionPanelAPI options;
	private VisualPanelAPI visual;

	private SectorEntityToken planet;
	private SectorAPI sector;
	private CampaignFleetAPI playerFleet;

	// Getting the color from settings.json
	private static final Color LOW_TECH_COLOR = Global.getSettings().getDesignTypeColor("Low Tech");

	private final Logger log;

	private final FactionAPI kriegFaction = Global.getSector().getFaction(AlkemiaIds.FACTION_KRIEG);

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

		showPlanetVisual();

		dialog.setOptionOnEscape("Leave", Options.LEAVE);

		optionSelected(null, Options.INIT);
	}

	private void showPlanetVisual() {
		if (planet.getCustomInteractionDialogImageVisual() != null && isKriegRevealed()) {
			visual.showImageVisual(planet.getCustomInteractionDialogImageVisual());
		} else {
			visual.showImageVisual(new InteractionDialogImageVisual("illustrations", "above_clouds", 640, 400));
		}
	}

	public void backFromEngagement(EngagementResultAPI result) {
	}

	private boolean surveyorLost = false;
	private boolean encounterDone = false;

	private FleetMemberAPI selectedSurveyor = null;
	private FleetMemberAPI seizedSurveyor = null;
	private List<FleetMemberAPI> remainingFleet = new ArrayList<>();

	private RelationshipAPI preBattleRelation = kriegFaction.getRelToPlayer();

	public void optionSelected(String text, Object optionData) {
		if (optionData == null)
			return;

		String option = (String) optionData;

		if (text != null) {
			textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
		}

		CargoAPI cargo = playerFleet.getCargo();
		PersonAPI captain = null;

		switch (option) {
			case Options.INIT:
				addTextf("Your fleet approaches %s.", planet.getName());

				Description desc = Global.getSettings().getDescription(planet.getCustomDescriptionId(), Type.CUSTOM);
				addText(desc.getText1FirstPara());

				if (!isKriegRevealed()) {
					addText("You watch the roiling iridescent clouds from the bridge of you flagship. Most of your fleet's Sensor Equipment is rendered useless by the strong atmospheric interference.");
					appendText(
							"Occasionaly, vast structures can be seen on the surface, clearly indicating the presence of some previous habitation.");
				} else {
					addText(desc.getText2());
				}

				updateOptions();
				break;
			case Options.SEARCH:
				options.clearOptions();

				String fleetOrShip = (playerFleet.getFleetSizeCount() > 1) ? "fleet" : "ship";
				addTextf("As your %s descends through the stratospheric cloud cover, the view finally clears.",
						fleetOrShip);
				appendText(
						"What appeared to be ruins from orbit now is clearly active factories and bustling cities. There are no starport beacons, but there appear to be airships in the sky.");

				addText("One of you bridge crew draws your attention to scanner display: a squadron of large Low Tech aircraft is headed on an intercept course.");
				appendText(
						"They fly in an attack formation and transmit no identification codes.");
				textPanel.highlightInLastPara(LOW_TECH_COLOR, "Low Tech");

				options.addOption("Continue", Options.COMBAT);

				break;
			case Options.COMBAT:
				options.clearOptions();

				revealKrieg();

				startEngagement(60, Options.RECONSIDER, Options.ENCOUNTER_DONE);
				break;
			case Options.RECONSIDER:
				options.clearOptions();

				updateOptions();
				break;
			case Options.SURVEY:
				initFleetMemberPicker(Options.SURVEY_SELECT, Options.RECONSIDER);
				break;
			case Options.SURVEY_SELECT:
				options.clearOptions();

				addTextf("The survey team prepares aboard %s.", selectedSurveyor.getShipName());
				textPanel.highlightFirstInLastPara(selectedSurveyor.getShipName(), Misc.getHighlightColor());

				TooltipMakerAPI tt = textPanel.beginTooltip();
				tt.addShipList(1, 1, Math.min((int) Math.ceil((dialog.getTextWidth() * 0.8f) / 1), 40f),
						Misc.getBasePlayerColor(), Collections.singletonList(selectedSurveyor), 10f);
				textPanel.addTooltip();

				boolean surveyAvailable = cargo.getCommodityQuantity(Commodities.CREW) >= SURVEY_CREW
						&& cargo.getCommodityQuantity(Commodities.HEAVY_MACHINERY) >= SURVEY_MACHINERY
						&& cargo.getCommodityQuantity(Commodities.SUPPLIES) >= SURVEY_SUPPLIES;

				Misc.showCost(textPanel, "Resources: consumed (available)", true, -1f, null, null,
						new String[] { Commodities.CREW, Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES },
						new int[] { SURVEY_CREW, SURVEY_MACHINERY, SURVEY_SUPPLIES },
						new boolean[] { false, true, true });

				options.addOption("Confirm", Options.SURVEY_SENT);
				options.setEnabled(Options.SURVEY_SENT, surveyAvailable);
				if (surveyAvailable)
					options.setTooltip(Options.SURVEY_SENT,
							"Assemble the survey team and send them to the surface");
				else
					options.setTooltip(Options.SURVEY_SENT,
							"Your cargo contains insufficient resources to perform this Survey");

				options.addOption("Abort", Options.RECONSIDER);
				break;
			case Options.SURVEY_SENT:
				options.clearOptions();

				addText("Just after their ship breaches the multi-colour cloud cover they report spotting numerous active industraial sites. Even in the sky some Low Tech aircraft can be detected.");
				textPanel.highlightInLastPara(LOW_TECH_COLOR, "Low Tech");

				addText("Shortly, a few armed aircraft intercept the survey team's ship.");
				addTextf(
						"They are informed over a very crackly commlink that they have violated airspace controlled by %s and are ordered to land.",
						kriegFaction.getDisplayNameLongWithArticle());
				textPanel.highlightFirstInLastPara(kriegFaction.getDisplayNameLongWithArticle(),
						kriegFaction.getColor());

				options.addOption("Comply", Options.SURVEY_COMPLY,
						"Land on the surface as instructed");
				options.addOption("Escape", Options.SURVEY_ESCAPE,
						"Attempt to escape back into space");

				break;
			case Options.SURVEY_COMPLY:
				options.clearOptions();

				addText("You order your survey team to comply over a breaking commlink - then the connection is severed completely.");
				addText("Tense minutes follow.");

				surveyorLost = true;
				encounterDone = true;

				options.addOption("Continue", Options.SEIZED);
				break;
			case Options.SURVEY_ESCAPE:
				options.clearOptions();

				revealKrieg();

				remainingFleet.clear();
				List<FleetMemberAPI> fleetMemberListAll = playerFleet.getFleetData().getMembersListCopy();
				for (FleetMemberAPI member : fleetMemberListAll) {
					if (member != selectedSurveyor) {
						playerFleet.getFleetData().removeFleetMember(member);
						remainingFleet.add(member);
					}
				}

				captain = getSurveyCaptain();
				if (captain == null) {
					captain = getOfficer(sector.getPlayerFaction());
					// TODO: Create officer candidate?
					captain.setRankId(Ranks.POST_OFFICER);

					selectedSurveyor.setCaptain(captain);
				}
				visual.showFleetMemberInfo(selectedSurveyor);

				addTextf(
						"You order %s to go back to orbit. However when the ship attempts to escape the aircraft give chase, clearly trying to shoot it down!",
						captain.getNameString());
				textPanel.highlightInLastPara(captain.getNameString());

				options.addOption("Continue", Options.SURVEY_FIGHT);
			case Options.SURVEY_FIGHT:
				options.clearOptions();

				int baseFP = selectedSurveyor.getFleetPointCost();
				startEngagement(baseFP, Options.SURVEY_EXIT, Options.ENCOUNTER_DONE);

				break;
			case Options.SURVEY_EXIT:
				options.clearOptions();

				addTextf("You hear reports of hull breaches on %s and then the comlink goes dark...",
						selectedSurveyor.getShipName());
				textPanel.highlightInLastPara(selectedSurveyor.getShipName());

				showPlanetVisual();

				surveyorLost = true;

				optionSelected(null, Options.ENCOUNTER_DONE);
				break;
			case Options.SEIZED:
				options.clearOptions();

				revealKrieg();

				captain = getSurveyCaptain();
				addText("The connection is re-established after a while.");
				appendTextf("It appears the local authorities have seized the ship, but are willing to release %s.",
						getCaptainAndCrew(captain));
				textPanel.highlightInLastPara(captain.getNameString());

				visual.showSecondPerson(captain);

				captain.getStats().addXP(2000, textPanel);
				addTextf(
						"\"Captain, there is someone from the local authorities who wants to speak with you. I believe this might be an opportunity to improve our relations with %s.\"",
						kriegFaction.getDisplayNameWithArticle());
				textPanel.highlightInLastPara(kriegFaction.getColor(), kriegFaction.getDisplayNameWithArticle());

				options.addOption("Continue", Options.SEIZED_NEGOTIATE);
				break;
			case Options.SEIZED_NEGOTIATE:
				options.clearOptions();

				addText("The commlink flickers and another person appears on the screen.");

				PersonAPI negotiator = null;
				if (sectorMemoryContains(AlkemiaIds.KEY_KRIEG_LEADER)) {
					Object duke = Global.getSector().getMemory().get(AlkemiaIds.KEY_KRIEG_LEADER);
					if (duke != null && duke instanceof PersonAPI) {
						negotiator = (PersonAPI) duke;
					} else {
						addTextf("[%s wrong class]", AlkemiaIds.KEY_KRIEG_LEADER);
						negotiator = getOfficer(kriegFaction);
					}
				} else {
					addTextf("[%s unavailable]", AlkemiaIds.KEY_KRIEG_LEADER);
					negotiator = getOfficer(kriegFaction);
				}
				visual.showPersonInfo(negotiator);
				addTextf(
						"\"Captain, I know what loss of a ship is. Nevertheless you must understand the situation we are in.\" %s pauses.",
						negotiator.getHeOrShe());
				addText("\"We have been stranded here for centuries and finally we can fill in the technological blanks that keep us here.");
				appendTextf("It is therefore my duty to the nation of %s that I confiscate this ship.\"",
						planet.getName());
				textPanel.highlightInLastPara(kriegFaction.getColor(), planet.getName());

				addTextf("\"I assure you, %s are free to go.\"", getCaptainAndCrew(captain));

				options.addOption("Agree", Options.SEIZED_DEAL);
				options.addOption("Offer a diffrent Ship", Options.SEIZED_EXCHANGE);
				break;
			case Options.SEIZED_EXCHANGE:
				seizedSurveyor = selectedSurveyor;
				for (FleetMemberAPI ship : remainingFleet) {
					playerFleet.getFleetData().addFleetMember(ship);
				}
				remainingFleet.clear();

				initFleetMemberPicker(Options.SEIZED_EXCHANGE_CONFIRM, Options.SEIZED_DEAL);
				break;
			case Options.SEIZED_EXCHANGE_CONFIRM:
				if (seizedSurveyor != null) {
					playerFleet.getFleetData().addFleetMember(seizedSurveyor);
				}
				break;
			case Options.SEIZED_DEAL:
				// unlock planetary interactions
				setSectorMemory(AlkemiaIds.KEY_KRIEG_UNLOCKED, true);

				showPlanetVisual();

				addText("Eventually, the fleet recieves coordinates for a safe pickup and a shuttle collects your survey team.");
				addTextf("The %s will remain with %s for whatever purpose they need it for.",
						selectedSurveyor.getShipName(), kriegFaction.getDisplayNameLongWithArticle());
				Highlights h = new Highlights();
				h.setText(selectedSurveyor.getShipName(), kriegFaction.getDisplayNameLongWithArticle());
				h.setColors(Misc.getHighlightColor(), kriegFaction.getColor());
				textPanel.setHighlightsInLastPara(h);

				preBattleRelation.adjustRelationship(selectedSurveyor.getFleetPointCost()/100, RepLevel.FRIENDLY);

			case Options.ENCOUNTER_DONE:
				options.clearOptions();

				kriegFaction.setRelationship(Factions.PLAYER, preBattleRelation.getRel());
				// Recombine fleet
				for (FleetMemberAPI ship : remainingFleet) {
					playerFleet.getFleetData().addFleetMember(ship);
				}
				remainingFleet.clear();
				if (surveyorLost) {
					playerFleet.getFleetData().removeFleetMember(selectedSurveyor);

					addText("Ship lost:");
					TooltipMakerAPI lost = textPanel.beginTooltip();
					lost.addShipList(1, 1, Math.min((int) Math.ceil((dialog.getTextWidth() * 0.8f) / 1), 40f),
							Misc.getBasePlayerColor(), Collections.singletonList(selectedSurveyor), 10f);
					textPanel.addTooltip();
				}

				updateOptions();
				break;
			case Options.REPORT:
				options.clearOptions();

				if (surveyorLost) {
					addTextf(
							"The returning survey team reports that there is no spaceport on %s. Instead, there are several large military air bases.",
							planet.getName());
					textPanel.highlightFirstInLastPara(planet.getName(), kriegFaction.getColor());
				}

				addTextf(
						"Your officers report that the local military, %s, is heavily armed and employs large aircraft that share many similarities with pre-Collapse trans-atmospheric Low Tech designs.",
						kriegFaction.getDisplayName());
				textPanel.highlightFirstInLastPara(kriegFaction.getDisplayName(), kriegFaction.getColor());
				textPanel.highlightInLastPara(LOW_TECH_COLOR, "Low Tech");

				addText("They also managed to acquire a scan of one of the encountered patrols.");

				TooltipMakerAPI intel = textPanel.beginTooltip();
				List<FleetMemberAPI> kriegDesigns = new ArrayList<>();

				Map<String, FleetMemberAPI> seen = new WeakHashMap<>();
				CampaignFleetAPI exampleFleet = KriegDefenderGen.createDefenderFleet(planet.getMarket(),
						kriegFaction.getId(), "Stratospheric Patrol", 30);
				for (FleetMemberAPI design : exampleFleet.getMembersWithFightersCopy()) {
					if (!seen.containsKey(design.getHullId())) {
						seen.put(design.getHullId(), design);
						kriegDesigns.add(design);
					}
				}

				intel.addShipList(8, (int) Math.ceil(kriegDesigns.size() / 8),
						Math.min((int) Math.ceil((dialog.getTextWidth() * 0.8f) / 1), 40f),
						Misc.getBasePlayerColor(), kriegDesigns, 10f);
				textPanel.addTooltip();

				updateOptions();
				break;
			case Options.MARKET:
				InteractionDialogPlugin planetPlugin = new RuleBasedInteractionDialogPluginImpl();

				dialog.setPlugin(planetPlugin);
				planetPlugin.init(dialog);
				break;
			case Options.LEAVE:
				setSectorMemory(AlkemiaIds.KEY_ATMOSPHERIC, false);
				sector.setPaused(false);
				dialog.dismiss();
		}

	}

	private PersonAPI getOfficer(FactionAPI faction) {
		return OfficerManagerEvent.createOfficer(
				faction, 1,
				SkillPickPreference.ANY,
				true, null, false, false, -1,
				MathUtils.getRandom());
	}

	private String getCaptainAndCrew(PersonAPI captain) {
		if (captain != null)
			return String.format("captain %s and %s crew", captain.getNameString(), captain.getHisOrHer());
		return "the crew";
	}

	private void updateOptions() {
		options.clearOptions();

		if (isKriegRevealed()) {
			if (isKriegUnlocked()) {
				options.addOption("Enter Orbit", Options.MARKET,
						"Attempt communication or trade");
			}
		} else {
			options.addOption("Search Planet", Options.SEARCH,
					"Land on the surface to begin salvage operations");
			options.addOption("Send Survey Ship", Options.SURVEY,
					"Send a survey ship to the planet");
		}
		if (encounterDone) {
			options.addOption("Survey Report", Options.REPORT,
					"Review the intelligence report");
		}

		options.addOption("Leave", Options.LEAVE, null);
	}

	private boolean sectorMemoryContains(String key) {
		return sector.getMemory().contains(key);
	}

	private void setSectorMemory(String key, Object value) {
		sector.getMemory().set(key, value);
	}

	private boolean getSectorBool(String key) {
		return sector.getMemoryWithoutUpdate().getBoolean(key);
	}

	private PersonAPI getSurveyCaptain() {
		if (selectedSurveyor != null) {
			return selectedSurveyor.getCaptain();
		}
		return null;
	}

	private boolean isKriegRevealed() {
		return getSectorBool(AlkemiaIds.KEY_KRIEG_REVEALED);
	}

	private boolean isKriegUnlocked() {
		return getSectorBool(AlkemiaIds.KEY_KRIEG_UNLOCKED);
	}

	private void revealKrieg() {
		if (!isKriegRevealed()) {
			Relic.addKriegMarket();
			KriegGen.addKriegAdmin();

			visual.showImageVisual(planet.getCustomInteractionDialogImageVisual());
		}

		setSectorMemory(AlkemiaIds.KEY_KRIEG_REVEALED, true);
	}

	private void startEngagement(int baseFP, String winOption, String loseOption) {
		setSectorMemory(AlkemiaIds.KEY_ATMOSPHERIC, true);

		final MemoryAPI memory = planet.getMemoryWithoutUpdate();
		final CampaignFleetAPI ambushers = KriegDefenderGen.getFleetForPlanet(planet, AlkemiaIds.FACTION_KRIEG,
				"Stratospheric Patrol", baseFP);

		kriegFaction.setRelationship(Factions.PLAYER, -0.5f);

		dialog.setInteractionTarget(ambushers);

		final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
		config.leaveAlwaysAvailable = false;
		config.showCommLinkOption = false;
		config.showEngageText = true;
		config.showFleetAttitude = false;
		config.showTransponderStatus = false;
		config.showWarningDialogWhenNotHostile = false;
		config.alwaysAttackVsAttack = false;
		config.impactsAllyReputation = false;
		config.impactsEnemyReputation = true;
		config.pullInAllies = true;
		config.pullInEnemies = false;
		config.pullInStations = false;
		config.lootCredits = false;
		config.withSalvage = false;

		config.firstTimeEngageOptionText = "Engage the patrol";
		config.afterFirstTimeEngageOptionText = "Re-engage the patrol";
		config.noSalvageLeaveOptionText = "Continue";

		config.dismissOnLeave = false;
		config.printXPToDialog = true;

		final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);
		final SectorEntityToken baseEntity = planet;
		final InteractionDialogPlugin originalPlugin = this;
		final String win = winOption;
		final String lose = loseOption;

		config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
			@Override
			public void notifyLeave(InteractionDialogAPI dialog) {
				// nothing in there we care about keeping; clearing to reduce savefile size
				ambushers.getMemoryWithoutUpdate().clear();
				// there's a "standing down" assignment given after a battle is finished that we
				// don't care about
				ambushers.clearAssignments();
				ambushers.deflate();

				dialog.setInteractionTarget(baseEntity);

				// Global.getSector().getCampaignUI().clearMessages();

				if (plugin.getContext() instanceof FleetEncounterContext) {
					FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
					if (context.didPlayerWinEncounterOutright()) {
						memory.unset("$hasDefenders");
						memory.unset("$defenderFleet");
						memory.set("$defenderFleetDefeated", true);
						baseEntity.removeScriptsOfClass(FleetAdvanceScript.class);

						dialog.setPlugin(originalPlugin);
						originalPlugin.optionSelected(null, win);
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

						dialog.setPlugin(originalPlugin);
						originalPlugin.optionSelected(null, lose);
					}
				} else {
					dialog.setPlugin(originalPlugin);
					originalPlugin.optionSelected(null, Options.INIT);
				}
			}

			@Override
			public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
				bcc.aiRetreatAllowed = true;
				bcc.objectivesAllowed = false;
				bcc.enemyDeployAll = true;
			}
		};

		log.info("Begin combat");
		dialog.setPlugin(plugin);
		// plugin.otherFleetWantsToFight(true);
		plugin.init(dialog);

		plugin.optionSelected(null, FleetInteractionDialogPluginImpl.OptionId.INITIATE_BATTLE);
	}

	public void initFleetMemberPicker(String selected, String cancel) {
		final String selectedOption = selected;
		final String cancelOption = cancel;
		List<FleetMemberAPI> fleetMemberListAll = Global.getSector().getPlayerFleet().getFleetData()
				.getMembersListCopy();
		List<FleetMemberAPI> selectList = new ArrayList<>();

		// remove quest relevant ships from selectables
		for (FleetMemberAPI ship : fleetMemberListAll) {
			if (ship.getHullSpec().getTags().contains(Tags.SHIP_CAN_NOT_SCUTTLE)
					|| ship.getVariant().getTags().contains(Tags.SHIP_CAN_NOT_SCUTTLE))
				continue;
			if (!ship.isMothballed() && (ship.isFrigate() || ship.isDestroyer())) {
				selectList.add(ship);
			}
		}

		int shipsPerRow = 8;
		int rows = selectList.size() > shipsPerRow ? (int) Math.ceil(selectList.size() / (float) shipsPerRow) : 1;

		dialog.showFleetMemberPickerDialog("Select a ship", "Confirm", "Cancel", rows,
				shipsPerRow, 80f, true, true, selectList, new FleetMemberPickerListener() {
					@Override
					public void pickedFleetMembers(List<FleetMemberAPI> members) {
						if (members != null && !members.isEmpty()) {
							selectedSurveyor = members.get(0);

							optionSelected(null, selectedOption);
						}
					}

					@Override
					public void cancelledFleetMemberPicking() {
						optionSelected(null, cancelOption);
					}
				});
	}

	private void addText(String text) {
		textPanel.addParagraph(text);
	}

	private void addTextf(String format, Object... args) {
		textPanel.addParagraph(String.format(format, args));
	}

	private void appendText(String text) {
		textPanel.appendToLastParagraph(" " + text);
	}

	private void appendTextf(String format, Object... args) {
		textPanel.appendToLastParagraph(" " + String.format(format, args));
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
