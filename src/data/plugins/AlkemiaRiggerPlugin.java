package data.plugins;

// import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
// import com.fs.starfarer.api.util.Misc;

public class AlkemiaRiggerPlugin extends BaseEveryFrameCombatPlugin {
    private static Logger log = Global.getLogger(AlkemiaRiggerPlugin.class);

    CombatEngineAPI engine;

    float skippedFrames = 0;
    Vector2f mode4Direction;

    private static final Map<ShipAPI, ShipAPI> TO_MANAGE = new WeakHashMap<>();
    // private static final Color COLOR_RED = new Color(255, 0, 0, 255);

    private List<ShipAPI> managedShips;

    private boolean forceCheck = true;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;

        checkManaged(engine);

        log.info(String.format("Have %d ships to manage", managedShips.size()));
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (skippedFrames < 0.5f) {
            skippedFrames += amount;
            return;
        }

        if (engine == null)
            return;

        if (forceCheck) {
            checkManaged(engine);
        }

        Iterator<ShipAPI> ships = managedShips.iterator();
        while (ships.hasNext()) {
            ShipAPI ship = ships.next();
            if (!engine.isEntityInPlay(ship)) {
                forceCheck = true;
                continue;
            }
            // float facing = ship.getFacing();
            // engine.addFloatingText(ship.getLocation(), String.format("%.2f", facing), 30.0F,
            //         COLOR_RED, ship, 0.1f, 0.5f);
        }
    }

    private void checkManaged(CombatEngineAPI engine) {
        for (Iterator<ShipAPI> iter = TO_MANAGE.keySet().iterator(); iter.hasNext();) {
            ShipAPI c = iter.next();
            if (!engine.isEntityInPlay(c) || !engine.isEntityInPlay(TO_MANAGE.get(c))) {
                iter.remove();
            }
        }
        managedShips = new ArrayList<>(TO_MANAGE.values());
    }

    public List<ShipAPI> getManaged() {
        return managedShips;
    }

    public static void manage(ShipAPI ship) {
        TO_MANAGE.put(ship, ship);
    }

    public static void clear() {
        TO_MANAGE.clear();
    }
}
