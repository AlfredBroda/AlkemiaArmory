package data.missions.rescue;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {
		
		api.initFleet(FleetSide.PLAYER, "ACO", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Alkemia Customs salvage fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Tri-Tachyon Corporate Acquisitions Division fleet");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Your flagship's Burn Drive system enables you to move faster");
		api.addBriefingItem("Use Repair Drones to fix your armor");
		api.addBriefingItem("Use your Deluxe wings to quickly eliminate isolated ships");
		api.addBriefingItem("Use the station Point Defenses to your advantage");

		api.addToFleet(FleetSide.PLAYER, "alkemia_perch_standard", FleetMemberType.SHIP, "ACO Quicksilver", true);
		api.addToFleet(FleetSide.PLAYER, "alkemia_falcon_cargo_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "alkemia_brawler_ranger", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "alkemia_kestrel_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "alkemia_shepherd_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shepherd_Frontier", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "alkemia_kite_support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "alkemia_kite_standard", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "alkemia_deluxe_lrstrike_wing", FleetMemberType.FIGHTER_WING, false);
		// api.addToFleet(FleetSide.PLAYER, "alkemia_deluxe_lrstrike_wing", FleetMemberType.FIGHTER_WING, false);

		String stationName = "ACS Salvager's Rest";
		api.addToFleet(FleetSide.PLAYER, "alkemia_station_mining_small_Standard", FleetMemberType.SHIP, stationName, false);
		api.defeatOnShipLoss(stationName);

		// Set up the enemy fleet
		// api.addToFleet(FleetSide.ENEMY, "paragon_Raider", FleetMemberType.SHIP, true);
		api.addToFleet(FleetSide.ENEMY, "astral_Strike", FleetMemberType.SHIP, "TTS Black Ghost", true);
		api.addToFleet(FleetSide.ENEMY, "aurora_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "medusa_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "medusa_PD", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brawler_tritachyon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brawler_tritachyon_Standard", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "dagger_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "dagger_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "longbow_wing", FleetMemberType.FIGHTER_WING, false);
		api.addToFleet(FleetSide.ENEMY, "longbow_wing", FleetMemberType.FIGHTER_WING, false);

		float width = 16000f;
		float height = 16000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		for (int i = 0; i < 5; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 1000f; 
			api.addNebula(x, y, radius);
		}
		
		for (float x = -10000f; x < 10000f; x += 500f) {
			float y = x;
			float radius = 400f + (float) Math.random() * 200f; 
			api.addNebula(x, y, radius);
		}
		
		float minX = -width/2;
		float minY = -height/2;
//		api.addObjective((float)minX + width * 0.05f, minY + height * 0.95f, "nav_buoy");
//		api.addObjective((float)minX + width * 0.95f, minY + height * 0.95f, "nav_buoy");
		api.addObjective((float)minX + width * 0.25f, minY + height * 0.5f, "sensor_array");
// 		api.addObjective((float)minX + width * 0.25f, minY + height * 0.7f, "comm_relay");
		api.addObjective((float)minX + width * 0.75f, minY + height * 0.5f, "nav_buoy");
//		api.addObjective((float)minX + width * 0.75f, minY + height * 0.5f, "comm_relay");
		
		api.addAsteroidField(0, 0, 0, 2000f,
								20f, 70f, 100);
		
//		api.addPlanet((float)(minX + width * 0.1f), (float)(height * 0.05f),  (float)320f, "star_yellow", (float) 300f);
//		api.addPlanet((float)(minX + width * 0.75f), (float)(height * 0.07f),  (float)256f, "desert", (float) 250f);
//		api.addPlanet((float)(minX + width * 0.85f), (float)(-height * 0.07f), (float) 96f, "barren", (float) 100f);
	}

}
