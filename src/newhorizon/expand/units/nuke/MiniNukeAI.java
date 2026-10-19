package newhorizon.expand.units.nuke;

import arc.math.geom.Vec2;
import mindustry.entities.Units;
import mindustry.gen.Teamc;

/**
 * Flies a mini nuke (what a decoy breaks into) at the nearest thing of the enemy's within sight, and goes off on the first enemy it
 * meets. With nothing in sight it flies straight on until its fuel runs out.
 */
public class MiniNukeAI extends NukeAI{
	/** How far it can see. */
	public float sight = 520f;

	@Override
	protected boolean aim(Vec2 out){
		//Whatever it flies at is a guess, not an order: it still goes off at the first thing in its way.
		explicitAim = false;
		Teamc target = Units.closestTarget(unit.team, unit.x, unit.y, sight, u -> u.checkTarget(true, true), b -> true);
		if(target == null) return false;
		out.set(target);
		return true;
	}
}
