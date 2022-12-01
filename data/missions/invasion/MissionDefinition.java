package data.missions.invasion;

import java.util.List;
import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		api.initFleet(FleetSide.PLAYER, "HSS", FleetGoal.ESCAPE, false);
		api.initFleet(FleetSide.ENEMY, "KAF", FleetGoal.ATTACK, true);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Hegemony Invasion Fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Krieg Air Force");

		// These show up as items in the bulleted list under
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Use Nav Buoys to your advantage");

		// api.addToFleet(FleetSide.PLAYER, "dominator_XIV_Elite", FleetMemberType.SHIP, "HSS Broadsword", true);
		api.addToFleet(FleetSide.PLAYER, "eradicator_Support", FleetMemberType.SHIP, true);
		List<String> variants = new ArrayList();
		variants.add("mora_Assault");
		variants.add("enforcer_Assault");
		variants.add("vanguard_Attack");
		variants.add("vanguard_Attack");
		variants.add("kite_Starting");
		variants.add("kite_hegemony_Interceptor");
		variants.add("hound_Standard");

		for (String ship : variants) {
			FleetMemberAPI fleetMemeber = api.addToFleet(FleetSide.PLAYER, ship, FleetMemberType.SHIP, false);
		}

		// api.addToFleet(FleetSide.PLAYER, "broadsword_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.PLAYER, "talon_wing", FleetMemberType.FIGHTER_WING, false);

		// Objectives
		variants.clear();
		variants.add("colossus_Standard");
		// variants.add("phaeton_Standard");
		variants.add("nebula_Standard");
		variants.add("colossus_Standard");

		for (String ship : variants) {
			FleetMemberAPI fleetMemeber = api.addToFleet(FleetSide.PLAYER, ship, FleetMemberType.SHIP, false);
			api.defeatOnShipLoss(fleetMemeber.getShipName());
			api.addBriefingItem(String.format("Protect %s as it makes landfall", fleetMemeber.getShipName()));
		}

		// Set up the enemy fleet
		variants.clear();
		variants.add("krieg_fortress_Standard");
		variants.add("krieg_kr47_Standard");
		variants.add("krieg_kadze_Assault");
		variants.add("krieg_kadze_Assault");
		variants.add("krieg_kr35_Standard");
		variants.add("krieg_kr35_Standard");
		variants.add("krieg_bakemono_Standard");
		variants.add("krieg_bakemono_Bomber");
		variants.add("krieg_bakemono_Bomber");

		for (String ship : variants) {
			FleetMemberAPI fleetMemeber = api.addToFleet(FleetSide.ENEMY, ship, FleetMemberType.SHIP, false);
		}

		api.addToFleet(FleetSide.ENEMY, "krieg_vampire_Fighter_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "krieg_vampire_Fighter_wing", FleetMemberType.FIGHTER_WING, false);

		float width = 10000f;
		float height = 16000f;
		api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

		// This does not work. Needs to be defined in descriptor.json
		api.setBackgroundSpriteName("/graphics/alkemia/backgrounds/tahlan_lethia.jpg");
		/*
		"fields1":"graphics/alkemia/backgrounds/fields1.png",
		"desert1":"graphics/alkemia/backgrounds/desert1.png",
		"desert2":"graphics/alkemia/backgrounds/desert2.png",
		"ocean1":"graphics/alkemia/backgrounds/ocean1.png",
		"ocean2":"graphics/alkemia/backgrounds/ocean2.png",
		"basin1":"graphics/alkemia/backgrounds/basin.png"
		*/

		api.setNebulaTex(Global.getSettings().getSpriteName("terrain","nebula_clouds"));
        // api.setNebulaMapTex("graphics/terrain/nebula_amber_map.png");

		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width / 2;
			float y = (float) Math.random() * height - height / 2;
			float radius = 100f + (float) Math.random() * 1000f;
			api.addNebula(x, y, radius);
		}

		float minX = -width / 2;
		float minY = -height / 2;

		// api.addObjective((float)minX + width * 0.5f, minY + height * 0.3f,
		// "nav_buoy");
		api.addObjective((float) minX + width * 0.5f, minY + height * 0.5f, "nav_buoy");
		// api.addObjective((float)minX + width * 0.5f, minY + height * 0.7f,
		// "nav_buoy");
		api.addObjective((float) minX + width * 0.5f, minY + height * 0.9f, "nav_buoy");

		// api.addAsteroidField(0, 0, 0, 2000f, 20f, 70f, 100);

		// api.addPlanet((float)(minX + width * 0.1f), (float)(height * 0.05f),
		// (float)320f, "star_yellow", (float) 300f);
		// api.addPlanet((float)(minX + width * 0.75f), (float)(height * 0.07f),
		// (float)256f, "desert", (float) 250f);
		// api.addPlanet((float)(minX + width * 0.85f), (float)(-height * 0.07f),
		// (float) 96f, "barren", (float) 100f);

	}
}
