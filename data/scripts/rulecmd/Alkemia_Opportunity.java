package scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import data.scripts.AlkemiaIds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Frederoo
 */
public class Alkemia_Opportunity extends HubMissionWithBarEvent {
    public static final String GIVER_KEY = "$alkemia_opportunityData";
    public static final String SP_REQUIRED = "$alkemia_opportunitySPReq";
    public static final float EVENT_DAYS = 60;
    protected static final int COLUMNS = 7;

    public static List<String> POSSIBLE_MODS = new ArrayList<String>();
    static {
        POSSIBLE_MODS.add(AlkemiaIds.ALKEMIA_HULLMOD_DRONEBAYS);
        POSSIBLE_MODS.add(AlkemiaIds.ALKEMIA_HULLMOD_NANOFORGE);
    }

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (!barEvent)
            return false;

        // Must be at a Starship Workshop
        boolean hasWorkshop = createdAt.hasIndustry(AlkemiaIds.ALKEMIA_INDUSTRY_WORKSHOP);
        if (!hasWorkshop)
            return false;

        // setGiverFaction(Factions.ALKEMIA);
        setGiverPost(Ranks.POST_CITIZEN);
        setGiverVoice(Voices.BUSINESS);
        findOrCreateGiver(createdAt, false, true);

        PersonAPI person = getPerson();

        setRepFactionChangesTiny();

        boolean refSet = setPersonMissionRef(person, "$alkemia_opportunity_ref");
        int modIndex = Math.round((float) Math.random());
        Global.getSector().getMemoryWithoutUpdate().set("$alkemia_opportunity_mod_ref", POSSIBLE_MODS.get(modIndex));

        if (!Global.getSector().getMemoryWithoutUpdate().contains(SP_REQUIRED)
                && genRandom.nextFloat() < 0.1f)
            setSPRequired(true);

        return refSet;
    }

    @Override
    public PersonAPI getPerson() {
        PersonAPI person = (PersonAPI) Global.getSector().getMemoryWithoutUpdate().get(GIVER_KEY);
        if (person == null) {
            person = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson();

            // person.setPortraitSprite(Global.getSettings().getSpriteName("intel",
            // "repairs_finished"));

            Global.getSector().getMemoryWithoutUpdate().set(GIVER_KEY, person);
        }

        return person;
    }

    public static boolean getSPRequired() {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(SP_REQUIRED);
    }

    public static void setSPRequired(boolean required) {
        Global.getSector().getMemoryWithoutUpdate().set(SP_REQUIRED, required, EVENT_DAYS);
    }

    @Override
    protected void updateInteractionDataImpl() {
        set("$alkemia_opp_requiresSP", getSPRequired());
        set("$alkemia_opp_isInhosp", Global.getSector().getPlayerFaction()
                .getRelationshipLevel(Factions.INDEPENDENT)
                .isAtBest(RepLevel.INHOSPITABLE));
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap) {
        if (action.equals("showPicker")) {
            showPicker(dialog, memoryMap);
            return true;
        }
        if (action.equals("spentSP")) {
            set("$alkemia_opp_requiresSP", false);
            setSPRequired(false);
            return true;
        }

        if (action.equals("canPick")) {
            if (getAvailableShips().isEmpty()) {
                dialog.getOptionPanel().setEnabled("alkemia_oppShowPicker", false);
                dialog.getOptionPanel().setTooltip("alkemia_oppShowPicker", "No ships that can install this hullmod");
            }

            return true;
        }

        return true;
    }

    protected void showPicker(final InteractionDialogAPI dialog, final Map<String, MemoryAPI> memoryMap) {
        List<FleetMemberAPI> avail = getAvailableShips();

        int rows = avail.size() / 7 + 1;

        dialog.showFleetMemberPickerDialog("Pick ship for installation", "Ok", "Cancel", rows, COLUMNS, 58f,
                true, false, avail, new FleetMemberPickerListener() {

                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members.isEmpty())
                            return;

                        String availableMod = Global.getSector().getMemoryWithoutUpdate().getString("$alkemia_opportunity_mod_ref");

                        ShipVariantAPI variant = members.get(0).getVariant();
                        if (variant.getSMods().contains(availableMod)) {
                            variant.removePermaMod(availableMod);
                        }
                        variant.addPermaMod(availableMod);
                        if (genRandom.nextBoolean()) {
                            variant.addMod("ill_advised");
                            DModManager.setDHull(variant);
        
                            memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppIllAdvised", true, 0);
                        }
        

                        FireBest.fire(null, dialog, memoryMap, "Alkemia_oppPostText");
                        memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppPicked", true, 0);
                        FireBest.fire(null, dialog, memoryMap, "Alkemia_oppPicked");
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppPicked", false, 0);
                        FireBest.fire(null, dialog, memoryMap, "Alkemia_oppPicked");
                    }
                });
    }

    protected List<FleetMemberAPI> getAvailableShips() {
        CampaignFleetAPI pool = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null);
        String availableMod = Global.getSector().getMemoryWithoutUpdate().getString("$alkemia_opportunity_mod_ref");
        for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (hasBuiltInMod(m, availableMod) || !isCarrier(m))
                continue;

            pool.getFleetData().addFleetMember(m);
        }

        return pool.getFleetData().getMembersListCopy();
    }

    @Override
    public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        currentStage = new Object(); // so that the abort() assumes the mission was successful
        abort();
    }

    /**
     * Does not count S-mods as "built-in"
     * 
     * @param member the ship to check
     * @param mod ID of the hullmod
     * @return whether the ship has a mod built-in
     */
    private boolean hasBuiltInMod(FleetMemberAPI member, String mod) {
        ShipVariantAPI variant = member.getVariant();

        if (variant.hasHullMod(mod)) return true;

        return variant.hasHullMod(mod)
                && !(variant.getSMods().contains(mod)
                        || variant.getNonBuiltInHullmods().contains(mod));
    }

    private boolean isCarrier(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();

        if (variant.isCarrier() || member.getHullSpec().getFighterBays() > 0) return true;

        return false;
    }

}
