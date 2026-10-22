package newhorizon.expand.units.serpent;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.util.Tmp;
import mindustry.gen.Unit;

/**
 * Flying round a target at a fixed distance, which is what lets a Serpent's body fire on it: the head goes round the target at
 * {@code radius}, always the same way, and the sections follow it round the same ring, so their sides, where their cannons point, face
 * the target. The head looks at the target all the while (a flying unit does not have to face the way it moves), so its laser stays on it.
 */
public class SerpentOrbit{
	/** How far the ring is let be missed before the pull back to it is at its strongest. */
	public static float pullRange = 40f;

	/** Moves the unit one step round the target. {@code direction} is 1 or -1, the way round. */
	public static void orbit(Unit unit, Position target, float radius, float direction){
		Tmp.v1.set(unit).sub(target);
		float dst = Tmp.v1.len();
		if(dst < 1f) Tmp.v1.set(1f, 0f);
		Tmp.v1.nor();

		float speed = unit.speed();
		if(dst > radius * 1.6f){
			//Far away: straight in, at full speed (which is where a boost can come in).
			Tmp.v2.set(Tmp.v1).scl(-1f);
		}else{
			//Along the ring, with a push towards it when off it.
			Tmp.v2.set(-Tmp.v1.y * direction, Tmp.v1.x * direction);
			float pull = Mathf.clamp((radius - dst) / pullRange, -1.2f, 1.2f);
			Tmp.v2.add(Tmp.v1.x * pull, Tmp.v1.y * pull);
		}
		unit.moveAt(Tmp.v2.setLength(speed));
		unit.lookAt(target);
	}
}
