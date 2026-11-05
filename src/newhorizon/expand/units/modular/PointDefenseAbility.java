package newhorizon.expand.units.modular;

import arc.Core;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * Point defense: every {@link #reload} ticks it takes {@link #bulletDamage} off the nearest enemy bullet within {@link #range} that can be
 * shot down (a bullet that is {@code hittable}), removing the bullet if that is all its damage; the way the base game's point defense
 * weapon works, but as an ability, with the same beam and hit effects.
 */
public class PointDefenseAbility extends Ability{
	public float range = 190f, reload = 8f, bulletDamage = 50f;
	public Color color = Color.valueOf("ffcc66");

	protected float timer;

	@Override
	public Ability copy(){
		PointDefenseAbility copy = (PointDefenseAbility)super.copy();
		copy.timer = 0f;
		return copy;
	}

	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		timer += Time.delta;
		if(timer < reload) return;

		Bullet[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Groups.bullet.intersect(unit.x - range, unit.y - range, range * 2f, range * 2f, b -> {
			if(b.team == unit.team || !b.type.hittable || b.isAdded() == false) return;
			float d = b.dst2(unit);
			if(d <= range * range && d < bestDst[0]){
				bestDst[0] = d;
				best[0] = b;
			}
		});
		Bullet target = best[0];
		if(target == null) return;

		timer = 0f;
		Fx.pointBeam.at(unit.x, unit.y, unit.angleTo(target), color, new Vec2(target.x, target.y));
		Fx.pointHit.at(target.x, target.y, color);
		if(Vars.net.client()) return;
		if(target.damage() > bulletDamage) target.damage(target.damage() - bulletDamage);
		else target.remove();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-point-defense", "Point defense");
	}
}
