package newhorizon.expand.units.ai;

import arc.math.Angles;
import arc.math.Mathf;
import mindustry.gen.Teamc;

/**
 * Flies a shooting drone (the Gunner, the Laser and the Tesla): it closes to {@link #standOff} of its weapon range from the target and then
 * circles it there, one way or the other (its own, by its id), so that it keeps firing while it moves. Its weapons aim at what the mothership
 * attacks, see {@link DroneAI}.
 */
public class ShooterDroneAI extends DroneAI{
	/** The distance it keeps from the target, as a share of its weapon range. */
	public float standOff = 0.7f;
	/** How far round the target (degrees) the point it flies to is ahead of it, while it circles. */
	public float lead = 28f;

	@Override
	protected void engage(Teamc t){
		float range = Math.max(40f, unit.type.maxRange);
		float want = range * standOff;
		float dist = unit.dst(t);
		if(dist > want * 1.35f){
			fly(t.getX(), t.getY(), unit.speed());
			return;
		}
		//on the circle round the target, a little ahead of where it is
		float side = (unit.id % 2 == 0) ? 1f : -1f;
		float a = Angles.angle(t.getX(), t.getY(), unit.x, unit.y) + side * lead;
		fly(t.getX() + Angles.trnsx(a, want), t.getY() + Angles.trnsy(a, want), unit.speed());
	}
}
