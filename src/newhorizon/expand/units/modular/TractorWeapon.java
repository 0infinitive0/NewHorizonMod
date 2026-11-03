package newhorizon.expand.units.modular;

import mindustry.entities.Units;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * The weapon of the Tractor Beam arm, which works the way the base game's Parallax does: it finds its own target, the nearest enemy
 * unit in reach (never a building, since a beam cannot drag one), turns to it, and holds the beam on it for as long as it is in reach.
 * Nobody aims it.
 */
public class TractorWeapon extends Weapon{
	public TractorWeapon(String name){
		super(name);
		controllable = false;
		autoTarget = true;
		continuous = true;
	}

	@Override
	protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
		return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> false);
	}
}
