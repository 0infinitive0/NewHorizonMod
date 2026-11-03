package newhorizon.expand.units.crab;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * Turns back every reflectable bullet of another team that gets within {@link #radius} of the unit: the bullet changes to the unit's
 * team and goes the way it came, so it is the shooter that gets hit. Bullets that cannot be reflected (lasers, bolts and the like) are
 * left for the shield. It is meant to be first in the unit's list of abilities, ahead of the shield that would swallow the bullet.
 */
public class ReflectAbility extends Ability{
	public float radius = 200f;
	public Color color = Color.valueOf("ff8c5a");
	/** The most bullets turned back in a tick, so that a huge volley cannot cost the frame. */
	public int perTick = 60;

	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		int[] left = {perTick};
		Groups.bullet.intersect(unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, b -> {
			if(left[0] <= 0 || b.team == unit.team || !b.type.reflectable || !b.within(unit, radius) || b.vel.len() < 0.01f) return;
			//Only ones that are going in.
			if(b.vel.dot(unit.x - b.x, unit.y - b.y) <= 0f) return;
			left[0]--;
			reflect(b, unit);
		});
	}

	protected void reflect(Bullet b, Unit unit){
		b.team = unit.team;
		b.owner = unit;
		b.vel.scl(-1f);
		b.rotation(b.vel.angle());
		if(b.collided != null) b.collided.clear();
		//It has to be able to hit the old owner again.
		b.time = Math.min(b.time, b.lifetime * 0.5f);
		Fx.hitLaserBlast.at(b.x, b.y, b.rotation(), color);
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-reflect");
	}
}
