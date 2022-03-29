package data.scripts.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.AlkemiaIds;

/**
 * Author: Frederoo
 */
public class Alkemia_Opportunity extends HubMissionWithBarEvent {
    public static final String GIVER_KEY = "$alkemia_opportunityData";
    public static final String SP_REQUIRED = "$alkemia_opp_requiresSP";
    public static final String SP_SPENT = "$alkemia_opp_spentSP";
    public static final String CREDITS_SPENT = "$alkemia_opp_paid";
    public static final String MOD_REF = "$alkemia_opportunity_mod_ref";

    public static final float BASE_COMPLICATIONS = 0.3f;
    public static final float EVENT_DAYS = 60;
    protected static final int COLUMNS = 7;

    protected PersonAPI person = null;
    protected float baseCostMultiplier = 0.5f;
    protected float baseCost = 0.0f;
    protected float complicationChance = BASE_COMPLICATIONS;

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
        setGiverImportance(PersonImportance.LOW);
        setGiverVoice(Voices.SPACER);
        findOrCreateGiver(createdAt, false, true);

        person = getPerson();

        setGiverFaction(Factions.INDEPENDENT);

        setRepFactionChangesTiny();

        MemoryAPI globalMemory = Global.getSector().getMemoryWithoutUpdate();

        boolean refSet = setPersonMissionRef(person, "$alkemia_opportunity_ref");
        globalMemory.set("$alkemia_oppGiverName", person.getName().getFirst(), 0);

        String selectedMod = globalMemory.getString(MOD_REF);
        if (selectedMod == null) {
            int modIndex = Math.round((float) Math.random() * (POSSIBLE_MODS.size() - 1));
            selectedMod = POSSIBLE_MODS.get(modIndex);
            globalMemory.set(MOD_REF, selectedMod, EVENT_DAYS);
        }

        HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(selectedMod);
        globalMemory.set("$alkemia_opportunity_mod_name", modSpec.getDisplayName(), 0);
        baseCost = modSpec.getBaseValue();

        if (Global.getSector().getMemoryWithoutUpdate().contains(SP_SPENT)
                || hasRepAll(person, RepLevel.INHOSPITABLE)) {
            setSPRequired(false);
        } else {
            setSPRequired(true);
        }

        globalMemory.set(CREDITS_SPENT, false);

        return refSet;
    }

    @Override
    public PersonAPI getPerson() {
        if (person == null) {
            person = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson();

            if (person.getGender() == Gender.MALE) {
                person.setPortraitSprite(Global.getSettings().getSpriteName("characters",
                        String.format("male_mechanic%d", getRandom(1, 2))));
            } else {
                person.setPortraitSprite(Global.getSettings().getSpriteName("characters",
                        String.format("female_mechanic%d", getRandom(1, 3))));
            }

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
        set(SP_REQUIRED, getSPRequired());
        set("$alkemia_opp_isInhosp", !hasRepAll(person, RepLevel.INHOSPITABLE));
        set("$alkemia_opp_liked", hasRepAny(person, RepLevel.WELCOMING));
        set("$alkemia_opp_cost", Misc.getWithDGS(baseCostMultiplier * baseCost));
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap) {

        if (action.equals("doPay")) {
            set(CREDITS_SPENT, true);
            return true;
        }

        if (action.equals("showPicker")) {
            showPicker(dialog, memoryMap);
            return true;
        }

        if (action.equals("spentSP")) {
            Global.getSector().getMemoryWithoutUpdate().set(SP_SPENT, true);
            baseCostMultiplier = 1.0f;
            complicationChance = 0.5f;
            set("$alkemia_opp_cost", Misc.getWithDGS(baseCostMultiplier * baseCost));

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

        int rows = avail.size() / COLUMNS + 1;

        dialog.showFleetMemberPickerDialog(
                "Pick a ship for mod integration", "Ok", "Cancel", rows, COLUMNS, 58f,
                true, false, avail, new FleetMemberPickerListener() {

                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members.isEmpty()) {
                            FireBest.fire(null, dialog, memoryMap, "alkemia_oppBackout");
                            return;
                        }

                        String availableMod = Global.getSector().getMemoryWithoutUpdate()
                                .getString(MOD_REF);

                        ShipVariantAPI variant = members.get(0).getVariant();
                        if (variant.getSMods().contains(availableMod)) {
                            variant.removePermaMod(availableMod);
                        }
                        variant.addPermaMod(availableMod);

                        if (rollProbability(complicationChance)) {
                            variant.addPermaMod(HullMods.ILL_ADVISED);
                            DModManager.setDHull(variant);

                            memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppIllAdvised", true, 0);
                            setDoNotAutoAddPotentialContactsOnSuccess();
                        }

                        FireBest.fire(null, dialog, memoryMap, "alkemia_oppPostText");
                        memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppPicked", true, 0);
                        FireBest.fire(null, dialog, memoryMap, "alkemia_oppPicked");
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppPicked", false, 0);
                        FireBest.fire(null, dialog, memoryMap, "alkemia_oppBackout");
                    }
                });
    }

    protected List<FleetMemberAPI> getAvailableShips() {
        CampaignFleetAPI pool = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null);
        String availableMod = Global.getSector().getMemoryWithoutUpdate().getString(MOD_REF);
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
     * @param mod    ID of the hullmod
     * @return whether the ship has a mod built-in
     */
    private boolean hasBuiltInMod(FleetMemberAPI member, String mod) {
        ShipVariantAPI variant = member.getVariant();

        if (variant.hasHullMod(mod))
            return true;

        return variant.hasHullMod(mod)
                && !(variant.getSMods().contains(mod)
                        || variant.getNonBuiltInHullmods().contains(mod));
    }

    private boolean isCarrier(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();

        if (variant.getNonBuiltInWings().size() > 0)
            return true;

        return false;
    }

    private int getRandom(int min, int max) {
        double seed = Math.random() * (max - min);
        long crop = Math.round(seed);
        return min + (int) crop;
    }

    private boolean hasRepAll(PersonAPI person, RepLevel rep) {
        return person.getRelToPlayer().isAtWorst(rep) &&
                Global.getSector().getPlayerFaction()
                        .getRelationshipLevel(person.getFaction())
                        .isAtWorst(rep);
    }

    private boolean hasRepAny(PersonAPI person, RepLevel rep) {
        return person.getRelToPlayer().isAtWorst(rep) ||
                Global.getSector().getPlayerFaction()
                        .getRelationshipLevel(person.getFaction())
                        .isAtWorst(rep);
    }
}
