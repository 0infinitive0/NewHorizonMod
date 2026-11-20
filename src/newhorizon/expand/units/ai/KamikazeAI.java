package newhorizon.expand.units.ai;

import mindustry.entities.Sized;
import mindustry.gen.Teamc;

/**
 * A drone that is a bomb: it dives straight at the mothership's target at full speed and, on touching it, blows itself up (its weapon is a death weapon,
 * which fires when the unit dies). Held back by Stand down like any drone, and replaced by the mothership after its usual time.
 */
public class KamikazeAI extends DroneAI{
	/** How close to the target's edge (px) it goes off. */
	public float fuse = 6f;

	@Override
	protected void engage(Teamc t){
		float half = t instanceof Sized s ? s.hitSize() / 2f : 0f;
		if(unit.within(t, unit.hitSize / 2f + half + fuse)){
			unit.kill();
			return;
		}
		fly(t.getX(), t.getY(), unit.speed());
	}
}
