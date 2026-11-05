package newhorizon.expand.units.gravity;

import mindustry.entities.Units;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * The Gravity Base's pull: a tractor beam that finds its own target, the nearest enemy unit in reach that is <b>outside</b> the unit's
 * field (one that is already inside is not a target, so it picks another), and holds the beam on it, dragging it toward the base. Nobody
 * aims it.
 */
public class PullWeapon extends Weapon{
	/** A unit closer than this to the base is not a target. */
	public float fieldRadius = 300f;

	public PullWeapon(String name){
		super(name);
		controllable = false;
		autoTarget = true;
		continuous = true;
	}

	@Override
	protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
		return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground) && !u.within(unit, fieldRadius), t -> false);
	}
}
