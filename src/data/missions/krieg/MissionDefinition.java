package data.missions.krieg;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;

import data.scripts.tools.Helpers;

import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

public class MissionDefinition implements MissionDefinitionPlugin {

	private SettingsAPI settings;
	protected Logger log;

	private final Random rand = new Random();

	public void defineMission(MissionDefinitionAPI api) {
		settings = Global.getSettings();
		log = Global.getLogger(getClass());

		FactionAPI player = Global.getSettings().createBaseFaction("krieg");

		api.initFleet(FleetSide.PLAYER, "KAF", FleetGoal.ATTACK, true);
		api.setFleetTagline(FleetSide.PLAYER, "Krieg Air Force");

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Shoot down the Invaders!");

		int fleetPoints = 110;
		generateFleet(player, api, FleetSide.PLAYER, fleetPoints, 0);

		// Select enemy faction
		List<FactionAPI> acceptableFactions = new ArrayList<>();
		for (FactionAPI faction : Global.getSector().getAllFactions()) {
			if (faction.getAlwaysKnownShips().size() < 5) {
				log.info(String.format("Rejecting faction \"%s\" (%s)", faction.getDisplayName(), faction.getId()));
				continue;
			}
			acceptableFactions.add(faction);
		}

		FactionAPI enemy = acceptableFactions.get(rand.nextInt(acceptableFactions.size()));
		api.initFleet(FleetSide.ENEMY, enemy.getShipNamePrefix(), FleetGoal.ESCAPE, false);
		String tagLine = String.format("%s Invasion Fleet", enemy.getDisplayName());

		api.setFleetTagline(FleetSide.ENEMY, tagLine);

		generateFleet(enemy, api, FleetSide.ENEMY, fleetPoints, 0.15f);

		// Generate map
		float width = 12000f;
		float height = 12000f;
		api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

		// This does not work. Needs to be defined in descriptor.json
		List<String> backdrops = new ArrayList<>();
		backdrops.add(settings.getSpriteName("backgrounds", "fields1"));
		backdrops.add(settings.getSpriteName("backgrounds", "basin1"));
		backdrops.add(settings.getSpriteName("backgrounds", "desert1"));
		backdrops.add(settings.getSpriteName("backgrounds", "desert2"));
		backdrops.add(settings.getSpriteName("backgrounds", "ocean1"));
		backdrops.add(settings.getSpriteName("backgrounds", "ocean2"));

		api.setBackgroundSpriteName(Helpers.getRandomElement(backdrops));

		api.setNebulaTex(settings.getSpriteName("terrain", "nebula_clouds"));
		// api.setNebulaMapTex("graphics/terrain/nebula_amber_map.png");

		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width / 2;
			float y = (float) Math.random() * height - height / 2;
			float radius = 100f + (float) Math.random() * 1000f;
			api.addNebula(x, y, radius);
		}

		float minX = -width / 2;
		float minY = -height / 2;

		api.addObjective((float) minX + width * 0.5f, minY + height * 0.5f, "nav_buoy");
		api.addObjective((float) minX + width * 0.5f, minY + height * 0.9f, "nav_buoy");
	}

	private static final Map<String, Float> QUALITY_FACTORS = new HashMap<>(13);

	static {
		QUALITY_FACTORS.put("default", 0.5f);
		QUALITY_FACTORS.put("shadow_industry", 0.65f); // Pre-collapse organization that is well equipped
		QUALITY_FACTORS.put(Factions.DERELICT, 0f); // Old and worn out von Neumann probes that are are very poorly
													// equipped
		QUALITY_FACTORS.put(Factions.DIKTAT, 0.5f); // Bog standard dictatorship with average gear
		QUALITY_FACTORS.put(Factions.HEGEMONY, 0.5f); // Comsec approved average gear
		QUALITY_FACTORS.put(Factions.INDEPENDENT, 0.5f); // Independents with average gear
		QUALITY_FACTORS.put(Factions.LIONS_GUARD, 0.75f); // Elite subdivision of the Diktat with above average gear
		QUALITY_FACTORS.put(Factions.LUDDIC_CHURCH, 0.25f); // Luddites are pacifists and poorly equipped
		QUALITY_FACTORS.put(Factions.LUDDIC_PATH, 0f); // Fanatics who are very poorly equipped
		QUALITY_FACTORS.put(Factions.PERSEAN, 0.55f); // Space NATO has slightly above average gear
		QUALITY_FACTORS.put(Factions.PIRATES, 0f); // Criminals who are very poorly equipped
		QUALITY_FACTORS.put(Factions.REMNANTS, 1f); // Are you Omega? Top of the line gear baby
		QUALITY_FACTORS.put(Factions.TRITACHYON, 0.85f); // Mega-corp with high-quality gear
		QUALITY_FACTORS.put("blackrock_driveyards", 0.75f); // Esoteric tech-lords with above average gear
		QUALITY_FACTORS.put("diableavionics", 0.75f); // Slavers with mysterious backers that posses above average gear
		QUALITY_FACTORS.put("exigency", 1f); // Stay out from under foot or be stepped on
		QUALITY_FACTORS.put("exipirated", 0.55f); // These pirates have some remarkable technology...
		QUALITY_FACTORS.put("interstellarimperium", 0.6f); // Well equipped and well disciplined
		QUALITY_FACTORS.put("junk_pirates", 0.45f); // Janky ships and weapons that are surprisingly effective
		QUALITY_FACTORS.put("pack", 0.5f); // Isolationists with effective and unique gear
		QUALITY_FACTORS.put("syndicate_asp", 0.5f); // Space FedEx is well funded and well armed
		QUALITY_FACTORS.put("templars", 1f); // What, did aliens give them this shit?
		QUALITY_FACTORS.put("ORA", 0.75f); // They found a hell of a cache of ships and weapons
		QUALITY_FACTORS.put("SCY", 0.55f); // Well equipped spies and tech-hoarders
		QUALITY_FACTORS.put("tiandong", 0.55f); // Refits tend to be made with care and have slightly above average gear
		QUALITY_FACTORS.put("Coalition", 0.65f); // Well entrenched and equipped coalition
		QUALITY_FACTORS.put("dassault_mikoyan", 0.75f); // Mega-corp with above average gear
		QUALITY_FACTORS.put("6eme_bureau", 0.85f); // Elite subdivision of DME with high-quality gear
		QUALITY_FACTORS.put("blade_breakers", 1f); // Jesus, who developed this tech?
		QUALITY_FACTORS.put("OCI", 0.75f); // Anyone who traveled as far as they have is well equipped
		QUALITY_FACTORS.put("al_ars", 0.5f); // The average of their ships tend to be middle of the road
		QUALITY_FACTORS.put("gmda", 0.5f); // Space Police with average gear
		QUALITY_FACTORS.put("draco", 0.55f); // Space Vampire pirates with slightly enhanced tech
		QUALITY_FACTORS.put("fang", 0.5f); // Psycho Werewolves with average gear
		QUALITY_FACTORS.put("HMI", 0.5f); // Miners and "legitimate" pirates with average gear
		QUALITY_FACTORS.put("mess", 0.9f); // Gray goo enhanced ships and weapons
		QUALITY_FACTORS.put("sylphon", 0.75f); // AI collaborators with advanced tech
		QUALITY_FACTORS.put("fob", 0.8f); // Aliens with... Alien tech
	}

	protected float getQuality(FactionAPI faction) {
		String id = faction.getId();

		if (QUALITY_FACTORS.containsKey(id)) {
			return QUALITY_FACTORS.get(id);
		}

		return QUALITY_FACTORS.get("default");
	}

	// Generate a fleet from the campaign fleet generator
	protected int generateFleet(FactionAPI faction, MissionDefinitionAPI api, FleetSide side, int fleetP,
			float transportPercent) {
		String factionId = faction.getId();

		float quality = getQuality(faction);
		FleetParamsV3 params = new FleetParamsV3(
				null, // LocInHyper
				factionId,
				quality,
				FleetTypes.PATROL_MEDIUM,
				fleetP, // CombatPts
				0f, // FreighterPts
				0f, // TankerPts
				transportPercent, // TransportPts
				0f, // LinerPts
				0f, // UtilityPts
				0f // QualityMod
		);
		params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;

		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

		List<FleetMemberAPI> fleetList = new ArrayList<>(fleet.getFleetData().getMembersListCopy());
		Collections.sort(fleetList, Helpers.COMPARE_PRIORITY);

		boolean flagshipChosen = false;
		for (FleetMemberAPI baseMember : fleetList) {
			String variant;
			if (baseMember.isFighterWing()) {
				variant = baseMember.getSpecId();
			} else {
				variant = baseMember.getVariant().getHullVariantId();
			}

			api.addToFleet(side, variant, baseMember.getType(), baseMember.getShipName(),
					(!baseMember.isFighterWing() && !flagshipChosen));

			if (!baseMember.isFighterWing() && !flagshipChosen) {
				flagshipChosen = true;
			}
		}

		return fleet.getFleetPoints();
	}
}
