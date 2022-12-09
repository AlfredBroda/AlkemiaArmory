package data.plugins;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import data.scripts.ai.missile.AlkemiaFuelbombAI;

// Original code by Tartiflette
public class FuelBombEffectPlugin extends BaseEveryFrameCombatPlugin {
    private final IntervalUtil globalTimer = new IntervalUtil(0.05f, 0.05f);

    // FUEL BOMBS
    private static final Map<MissileAPI, Vector2f> FUELBOMBS = new WeakHashMap<>();
    private static boolean forceCheck = false;

    //////////////////////////////
    // //
    // MAIN LOOP //
    // //
    //////////////////////////////
    @Override
    public void init(CombatEngineAPI engine) {
        // reinitialize the map
        FUELBOMBS.clear();

        forceCheck = false;
    }

    public static void cleanSlate() {
        FUELBOMBS.clear();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, null);

        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine.isPaused()) {
            return;
        }

        globalTimer.advance(amount);
        if (globalTimer.intervalElapsed()) {
            if (!FUELBOMBS.isEmpty()) {
                checkFuelBombs(engine);
            }
        }
        // forced antimissile check
        if (forceCheck) {
            checkFuelBombs(engine);
        }
    }

    public static void addFuelBomb(MissileAPI antimissile, Vector2f location) {
        FUELBOMBS.put(antimissile, location);
    }

    public static void forceCheck() {
        forceCheck = true;
    }

    public static final Color FLARE_COLOR = new Color(200, 165, 55, 255);

    private void checkFuelBombs(CombatEngineAPI engine) {
        for (Iterator<MissileAPI> iter = FUELBOMBS.keySet().iterator(); iter.hasNext();) {
            MissileAPI c = iter.next();
            if (!engine.isEntityInPlay(c) || c.isFading()) {
                AlkemiaFuelbombAI.explode(c, c, engine);

                iter.remove();
            }
        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
    }
}
