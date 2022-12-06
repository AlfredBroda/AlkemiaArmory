package data.scripts.campaign.econ;

import com.fs.starfarer.api.impl.campaign.econ.impl.Spaceport;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Pair;

public class KriegAirbase extends Spaceport {
    public static final float BASE_ACCESSIBILITY = 0.3f;

    public static float OFFICER_PROB_MOD = 0.1f;
	
	public static float UPKEEP_MULT_PER_DEFICIT = 0.05f;
		
	public static final float ALPHA_CORE_ACCESSIBILITY = 0.2f;
	public static final float IMPROVE_ACCESSIBILITY = 0.2f;	
	
	public void apply() {
		super.apply(true);
		
		int size = market.getSize();
		
		demand(Commodities.FUEL, size - 2);
		demand(Commodities.SUPPLIES, size - 2);
		demand(Commodities.SHIPS, size - 3);
		
		supply(Commodities.CREW, size - 1 );
		
		
		String desc = getNameForModifier();
		
		Pair<String, Integer> deficit = getUpkeepAffectingDeficit();
		
		if (deficit.two > 0) {
			float loss = getUpkeepPenalty(deficit);
			getUpkeep().modifyMult("deficit", 1f + loss, getDeficitText(deficit.one));
		} else {
			getUpkeep().unmodifyMult("deficit");
		}
		
		market.setHasSpaceport(true);
		
		float a = BASE_ACCESSIBILITY;
		if (a > 0) {
			market.getAccessibilityMod().modifyFlat(getModId(0), a, desc);
		}
		
		float officerProb = OFFICER_PROB_MOD;
		market.getStats().getDynamic().getMod(Stats.OFFICER_PROB_MOD).modifyFlat(getModId(0), officerProb);
		//market.getStats().getDynamic().getMod(Stats.OFFICER_IS_MERC_PROB_MOD).modifyFlat(getModId(0), officerProb);
		
		if (!isFunctional()) {
				supply.clear();
				unapply();
				market.setHasSpaceport(false);
		}
	}

    protected float getUpkeepPenalty(Pair<String, Integer> deficit) {
		float loss = deficit.two * UPKEEP_MULT_PER_DEFICIT;
		if (loss < 0) loss = 0;
		return loss;
	}
	
	protected Pair<String, Integer> getUpkeepAffectingDeficit() {
		return getMaxDeficit(Commodities.FUEL, Commodities.SUPPLIES, Commodities.SHIPS);
	}

    public float getPopulationGrowthBonus() {
		return 1;
	}

    public boolean canDowngrade() {
		return false;
	}

    public boolean canInstallAICores() {
		return false;
	}
}
