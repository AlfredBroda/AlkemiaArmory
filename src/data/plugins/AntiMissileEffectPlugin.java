package data.plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

// Original code by Tartiflette
public class AntiMissileEffectPlugin extends BaseEveryFrameCombatPlugin {

    private final IntervalUtil globalTimer = new IntervalUtil(0.05f, 0.05f);

    // ANTIMISSILE VARIABLES
    private static final Map<MissileAPI, MissileAPI> ANTIMISSILES = new WeakHashMap<>();
    private static boolean forceCheck = false;

    //////////////////////////////
    //                          //
    //        MAIN LOOP         //
    //                          //
    //////////////////////////////
    @Override
    public void init(CombatEngineAPI engine) {
        // reinitialize the map
        ANTIMISSILES.clear();
    }

    public static void cleanSlate() {
        ANTIMISSILES.clear();
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
            // check antimissiles timed
            if (!ANTIMISSILES.isEmpty()) {
                checkAntimissiles(engine);
            }
        }
        // forced antimissile check
        if (forceCheck) {
            checkAntimissiles(engine);
        }
    }

    public static List<MissileAPI> getAntimissiles() {
        List<MissileAPI> missiles = new ArrayList<>();

        for (MissileAPI m : ANTIMISSILES.keySet()) {
            if (ANTIMISSILES.get(m) != null) {
                missiles.add(ANTIMISSILES.get(m));
            }
        }
        return missiles;
    }

    public static void addAntimissiles(MissileAPI antimissile, MissileAPI target) {
        ANTIMISSILES.put(antimissile, target);
    }

    public static void forceCheck() {
        forceCheck = true;
    }

    private void checkAntimissiles(CombatEngineAPI engine) {
        for (Iterator<MissileAPI> iter = ANTIMISSILES.keySet().iterator(); iter.hasNext();) {
            MissileAPI c = iter.next();
            if (!engine.isEntityInPlay(c) || !engine.isEntityInPlay(ANTIMISSILES.get(c))) {
                iter.remove();
            }
        }
        forceCheck = false;
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
    }
}
