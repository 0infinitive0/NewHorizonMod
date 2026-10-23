package newhorizon.expand.units.serpent;

import arc.math.Angles;
import mindustry.entities.Units;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * A cannon that turns, but only within a limited arc about the side it is mounted on, and that only looks for targets in that arc: the
 * closest target overall may be straight ahead where it cannot fire, which would keep it from ever shooting at one on its side.
 */
public class SideWeapon extends Weapon{
	public SideWeapon(String name){
		super(name);
	}

	/** Whether the point is in the cannon's arc, as seen from the unit. */
	protected boolean inArc(Unit unit, float x, float y){
		return Angles.within(unit.angleTo(x, y), unit.rotation + baseRotation, rotationLimit / 2f + 8f);
	}

	@Override
	protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
		return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground) && inArc(unit, u.x, u.y), t -> ground && inArc(unit, t.x, t.y));
	}
}
