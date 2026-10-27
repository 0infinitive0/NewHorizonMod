package newhorizon.expand.units.serpent;

import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * The head's flame thrower. While the head is locked on to a target (see {@link SerpentLock}: its AI has one, or it was commanded to
 * attack one) it breathes fire all the time: firing is forced on and the firing cone is opened right out for that tick, so it does not
 * matter whether the target is in reach or the head is turned towards it. With no target it does nothing by itself; a piloted head fires
 * when the button is held, as any weapon does.
 */
public class FlameWeapon extends Weapon{
	/** The cone the weapon fires inside while it is not locked on. */
	public float cone = 25f;

	public FlameWeapon(){
		super();
	}

	@Override
	public void update(Unit unit, WeaponMount mount){
		boolean locked = unit.controller() instanceof SerpentLock && ((SerpentLock)unit.controller()).lockedTarget() != null;
		if(locked){
			mount.shoot = true;
			shootCone = 360f;
		}else{
			shootCone = cone;
		}
		super.update(unit, mount);
	}
}
