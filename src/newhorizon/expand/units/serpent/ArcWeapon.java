package newhorizon.expand.units.serpent;

import arc.math.Angles;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.type.Weapon;
import newhorizon.expand.bullets.nuke.ArcStrikeBulletType;

/**
 * The head's lightning cannon. It is a usual weapon except that it does not start a shot with nothing to hit: when it is ready (no charge
 * going, no cooldown left) and nothing the bolt could strike is at the point aimed at, firing is switched off for that tick. So a pilot
 * who holds the button over empty ground neither charges the weapon nor spends its long cooldown. A charge that has begun is never
 * cancelled. AI and commanded attacks always aim at a target, so this changes nothing for them.
 */
public class ArcWeapon extends Weapon{
	public ArcWeapon(String name){
		super(name);
	}

	@Override
	public void update(Unit unit, WeaponMount mount){
		if(mount.shoot && !mount.charging && mount.reload <= 0.0001f && bullet instanceof ArcStrikeBulletType){
			float mx = unit.x + Angles.trnsx(unit.rotation - 90f, x, y + shootY), my = unit.y + Angles.trnsy(unit.rotation - 90f, x, y + shootY);
			if(((ArcStrikeBulletType)bullet).pick(unit.team, mx, my, mount.aimX, mount.aimY) == null) mount.shoot = false;
		}
		super.update(unit, mount);
	}
}
