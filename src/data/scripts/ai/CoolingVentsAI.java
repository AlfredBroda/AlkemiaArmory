package data.scripts.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class CoolingVentsAI implements ShipSystemAIScript {

    private final IntervalUtil TICKER = new IntervalUtil(1.5f, 2.5f);

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        TICKER.advance(amount);
        if (TICKER.intervalElapsed() && !system.isActive()) {
            if (flags.hasFlag(AIFlags.SAFE_VENT)) {
                return;
            }

            if (ship.getFluxTracker().getFluxLevel() > 0.6f && AIUtils.canUseSystemThisFrame(ship)) {
                ship.useSystem();
            }
        }
    }
}
