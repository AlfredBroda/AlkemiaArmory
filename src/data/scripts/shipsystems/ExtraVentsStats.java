package data.scripts.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class ExtraVentsStats extends BaseShipSystemScript {

	private static final float GLOW_THRESHOLD = 0.2f;
	private static final float HARDFLUX_FRACTION = 1f;
	private static final float DISSIPATION_MULT = 2f;
	private static final CharSequence VENT_PATTERN = "_vent";

	private Map<String, WeaponAPI> vents = new HashMap<>();

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT * effectLevel);
		stats.getHardFluxDissipationFraction().modifyFlat(id, HARDFLUX_FRACTION * effectLevel);

		if ((state == State.IN || state == State.OUT) && stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			String key = ship.getId() + "_" + id;
			Object test = Global.getCombatEngine().getCustomData().get(key);
			if (test == null) {
				Global.getCombatEngine().getCustomData().put(key, new Object());

				if (vents.isEmpty()) {
					List<WeaponAPI> weapons = ship.getAllWeapons();
					for (WeaponAPI w : weapons) {
						if (w.getId().contains(VENT_PATTERN)) {
							w.getAnimation().setFrame(1);
							vents.put(w.getSlot().getId(), w);
						}
					}
				}
				setGlowForAll(effectLevel);
			} else {
				Global.getCombatEngine().getCustomData().remove(key);
			}
		}
	}

	private void setGlowForAll(float level) {
		Boolean isActive = level > GLOW_THRESHOLD;
		for (Entry<String, WeaponAPI> vent : vents.entrySet()) {
			WeaponAPI w = vent.getValue();

			AnimationAPI anim = w.getAnimation();
			int frame = 0;
			if (isActive) {
				frame = Math.round((anim.getNumFrames() - 1) * level);
			}
			if (frame > (anim.getNumFrames() - 1)) {
				frame = anim.getNumFrames() - 1;
			} else if (frame < 0) {
				frame = 0;
			}
			anim.setFrame(frame);

			SpriteAPI glowSprite = w.getGlowSpriteAPI();
			if (isActive) {
				glowSprite.setAdditiveBlend();
				glowSprite.renderAtCenter(w.getLocation().getX(), w.getLocation().getY());
			} else {
				glowSprite.setNormalBlend();
			}
			Color c = glowSprite.getColor();
			Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * level));
			glowSprite.setColor(newColor);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getFluxDissipation().unmodify(id);
		stats.getHardFluxDissipationFraction().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased flux dissipation", false);
		}
		return null;
	}
}
