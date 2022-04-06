package data.scripts.campaign.rulecmd;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
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
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import org.apache.log4j.Logger;

import data.scripts.AlkemiaIds;

/**
 * Author: Frederoo
 */
public class Alkemia_Opportunity extends HubMissionWithBarEvent {
    public static final String GIVER_KEY = "$alkemia_mechanicsData";
    public static final String SP_REQUIRED = "$alkemia_opp_requiresSP";
    public static final String SP_SPENT = "$alkemia_opp_spentSP";
    public static final String CREDITS_SPENT = "$alkemia_opp_paid";
    public static final String MOD_REF = "$alkemia_opportunity_mod_ref";
    public static final String MOD_NAME = "$alkemia_opportunity_mod_name";
    public static final String DID_INSTALL = "$alkemia_oppPicked";
    public static final String TAG_INTEL = "alkemia_opportunity";
    public static final float BASE_COMPLICATIONS = 0.3f;
    public static final float EVENT_DAYS = 5.0f;

    public static enum Stage {
        ACTIVE,
        PAID,
        COMPLETED,
        ENDED
    }

    public static List<String> POSSIBLE_MODS = new ArrayList<String>();
    static {
        POSSIBLE_MODS.add(AlkemiaIds.ALKEMIA_HULLMOD_DRONEBAYS);
        POSSIBLE_MODS.add(AlkemiaIds.ALKEMIA_HULLMOD_NANOFORGE);
    }

    protected static final int COLUMNS = 7;

    protected float baseCostMultiplier = 0.5f;
    protected float baseCost = 0.0f;
    protected float complicationChance = BASE_COMPLICATIONS;

    protected MarketAPI eventMarket;
    protected LocationAPI eventSystem;

    private static Logger log = Global.getLogger(Alkemia_Opportunity.class);
    private static MemoryAPI globalMemory = Global.getSector().getMemoryWithoutUpdate();

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (!barEvent)
            return false;

        // Must be at a Starship Workshop
        boolean hasWorkshop = createdAt.hasIndustry(AlkemiaIds.ALKEMIA_INDUSTRY_WORKSHOP);
        if (!hasWorkshop)
            return false;

        setPostingLocation(createdAt.getStarSystem().getCenter());

        // setGiverFaction(Factions.ALKEMIA);
        setGiverPost(Ranks.POST_CITIZEN);
        setGiverImportance(PersonImportance.LOW);
        setGiverVoice(Voices.SPACER);
        setGiverFaction(Factions.INDEPENDENT);
        setGiverIsPotentialContactOnSuccess();
        findOrCreateGiver(createdAt, false, true);

        PersonAPI person = getPersonOverride();
        if (person.getGender() == Gender.MALE) {
            person.setPortraitSprite(Global.getSettings().getSpriteName("characters",
                    String.format("male_mechanic%d", getRandom(1, 2))));
        } else {
            person.setPortraitSprite(Global.getSettings().getSpriteName("characters",
                    String.format("female_mechanic%d", getRandom(1, 3))));
        }

        Global.getSector().getMemoryWithoutUpdate().set(GIVER_KEY, person, EVENT_DAYS);

        eventMarket = createdAt;
        eventSystem = createdAt.getContainingLocation();

        setRepFactionChangesTiny();

        setStartingStage(Stage.ACTIVE);
        setSuccessStage(Stage.COMPLETED);
        setFailureStage(Stage.ENDED);

        PersonAPI captain = Global.getSector().getPlayerPerson();
        setStageOnMemoryFlag(Stage.COMPLETED, captain, DID_INSTALL);
        // setTimeLimit(Stage.ENDED, EVENT_DAYS, createdAt.getStarSystem()); // Won't
        // expire while player is in-system
        setTimeLimit(Stage.ENDED, EVENT_DAYS, null);

        boolean refSet = setPersonMissionRef(person, "$alkemia_opportunity_ref");
        globalMemory.set("$alkemia_oppGiverName", person.getName().getFirst(), 0);

        String selectedMod = null;
        if (globalMemory.contains(MOD_REF) && globalMemory.get(MOD_REF) != null) {
            selectedMod = globalMemory.getString(MOD_REF);
        } else {
            int modIndex = Math.round((float) Math.random() * (POSSIBLE_MODS.size() - 1));
            selectedMod = POSSIBLE_MODS.get(modIndex);
            globalMemory.set(MOD_REF, selectedMod, EVENT_DAYS);
        }

        HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(selectedMod);
        if (modSpec != null) {
            globalMemory.set(MOD_NAME, modSpec.getDisplayName(), EVENT_DAYS);
            baseCost = modSpec.getBaseValue();

            setName(String.format("Installation - %s", modSpec.getDisplayName()));
        }

        MemoryAPI playerMemory = captain.getMemory();
        if (playerMemory.contains(SP_SPENT)
                || hasRepAll(person, RepLevel.INHOSPITABLE)) {
            setSPRequired(false);
        } else {
            setSPRequired(true);
        }

        if (!playerMemory.contains(CREDITS_SPENT)) {
            playerMemory.set(CREDITS_SPENT, false);
        }

        return refSet;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map, Object currentStage) {
        MarketAPI entity = getPersonOverride().getMarket();

        if (entity != null && entity.getStarSystem() != null) {
            return entity.getStarSystem().getCenter();
        }

        return null;
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "repairs_finished");
    }

    public static boolean getSPRequired() {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(SP_REQUIRED);
    }

    public static void setSPRequired(boolean required) {
        Global.getSector().getMemoryWithoutUpdate().set(SP_REQUIRED, required, EVENT_DAYS);
    }

    // protected String getMissionTypeNoun() {
    // return "contract";
    // }

    @Override
    protected void updateInteractionDataImpl() {
        set(SP_REQUIRED, getSPRequired());
        set("$alkemia_opp_isInhosp", !hasRepAll(getPersonOverride(), RepLevel.INHOSPITABLE));
        set("$alkemia_opp_liked", hasRepAny(getPersonOverride(), RepLevel.WELCOMING));
        set("$alkemia_opp_cost", Misc.getWithDGS(baseCostMultiplier * baseCost));
    }

    public float getTimeRemainingFraction() {
        if (!isAccepted())
            return super.getTimeRemainingFraction();

        float f = 1f - elapsed / EVENT_DAYS;
        return f;
    }

    public float getDaysRemainig() {
        return EVENT_DAYS - elapsed;
    }

    public boolean isCompleted() {
        return currentStage == Stage.COMPLETED;
    }

    public boolean isAccepted() {
        return currentStage == Stage.ACTIVE;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_LOCAL);

        tags.add(getPersonOverride().getFaction().getId());

        return tags;
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap) {

        if (action.equals("showPicker")) {
            showPicker(dialog, memoryMap);
            return true;
        }

        if (action.equals("spentSP")) {
            float playerRel = getPersonOverride().getFaction().getRelToPlayer().getRel();
            baseCostMultiplier -= playerRel; // If SP was required than this value is negative hence increase cost
            complicationChance = Math.abs(playerRel); // the less the reputation, the bigger the chance for a nasty
                                                      // surprise
            log.info(String.format("complication chance: %g", complicationChance));
            log.info(String.format("new price: %s", Misc.getWithDGS(baseCostMultiplier * baseCost)));
            set("$alkemia_opp_cost", Misc.getWithDGS(baseCostMultiplier * baseCost));

            return true;
        }

        if (action.equals("giveIntel")) {
            addTag(TAG_INTEL);
            IntelManagerAPI intelManager = Global.getSector().getIntelManager();
            if (!intelManager.hasIntelOfClass(Alkemia_Opportunity.class)) {
                intelManager.addIntel(this, false);
            }
            // globalMemory.set(INTEL_REF, this);
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

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color tc = Misc.getTextColor();
        Color ac = Global.getSettings().getColor("colorAlkemia");
        float opad = 10f;

        String modName = globalMemory.getString(MOD_NAME);
        FactionAPI faction = getPerson().getFaction();
        LocationAPI system = eventMarket.getContainingLocation();
        if (system == null) {
            log.warn("System is null!");
        }

        LabelAPI label = info.addPara(
                String.format(
                        "You met a group of Alkemia Customs mechanics %s %s in the %s that can integrate %s into one of your carriers.",
                        eventMarket.getOnOrAt(), eventMarket.getName(), system.getNameWithLowercaseTypeShort(),
                        modName),
                opad, tc,
                faction.getBaseUIColor(), modName);

        label.setHighlight("Alkemia Customs", eventMarket.getName(), modName);
        label.setHighlightColors(ac, eventMarket.getFaction().getBaseUIColor(), h);
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        float opad = 10f;

        FactionAPI faction = getPerson().getFaction();

        if (currentStage == Stage.ACTIVE && timeLimit != null) {
            String days = getDaysString(timeLimit.days);
            String marketName = eventMarket.getName();
            LocationAPI system = eventMarket.getContainingLocation();
            if (system == null) {
                log.warn("System is null!");
            }
            String systemName = system.getNameWithLowercaseTypeShort();
            Color factionColors = eventMarket.getFaction().getBaseUIColor();

            LabelAPI label = info.addPara(
                    String.format("You need to return to %s in the %s in %s time to complete the installation.",
                            marketName, systemName, days),
                    opad, tc, faction.getBaseUIColor(), days);
            label.setHighlight(marketName, systemName);
            label.setHighlightColors(factionColors, factionColors);

            return true;
        }
        return false;
    }

    public String getStageDescriptionText() {
        return null;
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

                        boolean didComplication = false;
                        if (rollProbability(complicationChance)) {
                            variant.addPermaMod(HullMods.ILL_ADVISED);
                            DModManager.setDHull(variant);

                            memoryMap.get(MemKeys.LOCAL).set("$alkemia_oppIllAdvised", true, 0);
                            setDoNotAutoAddPotentialContactsOnSuccess();
                            didComplication = true;
                        }

                        FireBest.fire(null, dialog, memoryMap, "alkemia_oppPostText");
                        memoryMap.get(MemKeys.PLAYER).set(DID_INSTALL, true, 0);
                        FireBest.fire(null, dialog, memoryMap, "alkemia_oppPicked");

                        if (!didComplication) {
                            float probability = 1.0f - complicationChance;
                            log.info(String.format("new contact chance: %g", probability));
                            setPersonIsPotentialContactOnSuccess(getPersonOverride(), probability);
                            // addPotentialContacts(dialog);
                        }
                        removeIntel();
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        memoryMap.get(MemKeys.PLAYER).set(DID_INSTALL, false, 0);
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
        currentStage = Stage.COMPLETED;

        log.info("done accept");

        abort();
    }

    @Override
    public void abort() {
        globalMemory.expire(MOD_REF, 0);
        MemoryAPI playerMemory = Global.getSector().getPlayerPerson().getMemory();
        // This would make subsequent encounters get a "fresh start"
        // playerMemory.expire("$alkemia_metMechanics", 0);
        playerMemory.expire(SP_SPENT, 0);
        playerMemory.expire(CREDITS_SPENT, 0);

        super.abort();
    }

    private void removeIntel() {
        log.info("removing intel");
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();
        while (intelManager.hasIntelOfClass(Alkemia_Opportunity.class)) {
            Alkemia_Opportunity toRemove = (Alkemia_Opportunity) intelManager.getFirstIntel(Alkemia_Opportunity.class);
            log.info(toRemove.getSmallDescriptionTitle());

            toRemove.currentStage = Stage.COMPLETED;
            toRemove.abort();
            // intelManager.removeIntel(toRemove);
        }
        log.info("remove intel done");
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
