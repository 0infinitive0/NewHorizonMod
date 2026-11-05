package newhorizon.expand.units.spore;

import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.entities.units.UnitController;
import mindustry.gen.Unit;
import newhorizon.content.spore.SporeContent;

/**
 * What Blight does to a host's mind. It wraps the host's own controller: every {@link #interval} ticks it looks for the closest unit of
 * the host's <b>own team</b> that is alive and does not carry Blight, within {@link #seekRange}: any kind of unit, air, ground or naval. With one, the host moves toward it and stops a few pixels from touching it, while its own controller still does its targeting, so
 * its weapons go on firing as usual. With none, the wrapper just runs the host's own controller: its normal behaviour resumes at once.
 * Whoever installs the wrapper takes it off again with {@link #release} when the infection ends.
 */
public class BlightSeekAI extends AIController{
	/** How far it looks, and how often (ticks). */
	public static float seekRange = 240f, interval = 20f;

	/** The controller the host had before. */
	public final UnitController previous;
	private Unit target;
	private float timer;

	public BlightSeekAI(UnitController previous){
		this.previous = previous;
	}

	/** Puts the wrapper on a host that is not under a player's hands (and is not wrapped already). */
	public static void wrap(Unit unit){
		if(unit == null || unit.dead || unit.isPlayer() || unit.controller() instanceof BlightSeekAI) return;
		unit.controller(new BlightSeekAI(unit.controller()));
	}

	/** Gives the host its own controller back. */
	public static void release(Unit unit){
		if(unit != null && unit.controller() instanceof BlightSeekAI){
			unit.controller(((BlightSeekAI)unit.controller()).previous);
		}
	}

	@Override
	public void updateUnit(){
		timer -= Time.delta;
		if(timer <= 0f || !valid(target)){
			timer = interval;
			target = find();
		}
		if(target == null){
			previous.updateUnit();
			return;
		}
		if(previous instanceof AIController){
			AIController ai = (AIController)previous;
			ai.updateVisuals();
			ai.updateTargeting();
		}
		unit.lookAt(target);
		moveTo(target, unit.hitSize / 2f + target.hitSize / 2f + 4f);
	}

	private boolean valid(Unit u){
		return u != null && !u.dead && u.isAdded() && u.team == unit.team && !SporeContent.carriesBlight(u) && unit.within(u, seekRange + u.hitSize / 2f);
	}

	private Unit find(){
		Unit[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearby(unit.team, unit.x - seekRange, unit.y - seekRange, seekRange * 2f, seekRange * 2f, u -> {
			if(u == unit || !valid(u)) return;
			float d = u.dst2(unit);
			if(d < bestDst[0]){
				bestDst[0] = d;
				best[0] = u;
			}
		});
		return best[0];
	}
}
