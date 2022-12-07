package data.scripts.world;

import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;

import data.scripts.AlkemiaIds;
import data.scripts.world.systems.Relic;

public class KriegGen implements SectorGeneratorPlugin {

    protected final Logger log;

    public KriegGen() {
        log = Global.getLogger(getClass());
    }

    @Override
    public void generate(SectorAPI sector) {
        Relic relic = new Relic();
        relic.generate(sector);

        String mesg = "Failed generating Krieg!";
        StarSystemAPI relicSystem = relic.getSystem();
        if (relicSystem != null) {
            String constName = "Unknown Constellation";
            Constellation constellation = relicSystem.getConstellation();
            if (constellation != null)
                constName = constellation.getNameWithType();
            mesg = String.format("Krieg generator done (%s, %s).", relicSystem.getName(), constName);
            log.info(mesg);
        } else {
            log.warn(mesg);
        }
    }

    public static void addKriegAdmin() {
        SectorEntityToken krieg = Global.getSector().getEntityById(AlkemiaIds.KRIEG_PLANET);
        MarketAPI market = krieg.getMarket();
        if (market != null) {
            PersonAPI duke = Global.getFactory().createPerson();
            duke.setFaction(AlkemiaIds.FACTION_KRIEG);
            duke.setGender(Gender.MALE);
            duke.setPostId(Ranks.POST_FACTION_LEADER);
            duke.setRankId(Ranks.FACTION_LEADER);

            MutableCharacterStatsAPI stats = duke.getStats();
            if (stats != null) {
                stats.setSkillLevel(Skills.BEST_OF_THE_BEST, 3);
                stats.setSkillLevel(Skills.OFFICER_MANAGEMENT, 3);
                stats.setSkillLevel(Skills.TACTICAL_DRILLS, 3);
                stats.setSkillLevel(Skills.FIELD_REPAIRS, 1);
            }
            // admin.setAICoreId(Commodities.GAMMA_CORE);

            duke.getName().setFirst("Duke Harald");
            duke.getName().setLast("Dunkelheimer");
            duke.setPortraitSprite(Global.getSettings().getSpriteName("characters", "duke_harald"));
            duke.setPersonality(Personalities.AGGRESSIVE);
            duke.addTag(AlkemiaIds.TAG_LEADER);

            Global.getSector().getMemory().set(AlkemiaIds.KEY_KRIEG_LEADER, duke.getId());

            market.getCommDirectory().addPerson(duke, 0);
            market.addPerson(duke);

            PersonAPI admin = OfficerManagerEvent.createOfficer(
                    market.getFaction(), 1,
                    SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE,
                    true, null, false, false, -1,
                    MathUtils.getRandom());
            admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
            admin.getName().setLast("Dunkelheimer");
            admin.setPostId(Ranks.POST_ADMINISTRATOR);
            market.setAdmin(admin);
            market.getCommDirectory().addPerson(admin);

            Logger logger = Global.getLogger(KriegGen.class);
            logger.info("Krieg admin updated.");
        } else {
            Global.getLogger(KriegGen.class).warn("Failed to get Krieg!");
        }
    }

    public static void addBurrowAdmin() {
        SectorEntityToken burrow = Global.getSector().getEntityById(AlkemiaIds.KRIEG_BURROW);
        MarketAPI market = burrow.getMarket();
        if (market != null) {
            PersonAPI admin = OfficerManagerEvent.createOfficer(
                    market.getFaction(), 1,
                    SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE,
                    true, null, false, false, -1,
                    MathUtils.getRandom());
            admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin.setPostId(Ranks.POST_ADMINISTRATOR);
            admin.setRankId(Ranks.SPACE_LIEUTENANT);
            admin.setVoice(Voices.SPACER);
            admin.setContactWeight(1f);

            market.setAdmin(admin);
            market.getCommDirectory().addPerson(admin, 0);

            Global.getLogger(KriegGen.class).info("Burrow admin updated.");
        }
    }
}
