package newhorizon.expand.bullets.chronos;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/**
 * A bolt that, on hitting a unit, arcs on to the nearest enemy units around it, one after the other, damaging and freezing every one
 * of them. Each jump deals a share of the last one's damage.
 */
public class ChainFreezeBulletType extends BasicBulletType{
	public int jumps = 4;
	public float jumpRange = 80f;
	/** Damage kept by each further jump. */
	public float falloff = 0.82f;
	public Color chainColor = Color.white;
	public StatusEffect freeze;
	public float freezeTime = 60f * 4f;

	public ChainFreezeBulletType(float speed, float damage){
		super(speed, damage);
	}

	@Override
	public void hitEntity(Bullet b, Hitboxc entity, float health){
		super.hitEntity(b, entity, health);
		if(entity instanceof Unit) chain(b, (Unit)entity);
	}

	/** The units a chain started at {@code first} reaches, in order. */
	public Seq<Unit> chainTargets(mindustry.game.Team team, Unit first){
		Seq<Unit> visited = new Seq<>();
		visited.add(first);
		Unit cur = first;
		for(int i = 0; i < jumps; i++){
			Unit next = Units.closestEnemy(team, cur.x, cur.y, jumpRange, u -> !visited.contains(u) && u.checkTarget(collidesAir, collidesGround));
			if(next == null) break;
			visited.add(next);
			cur = next;
		}
		visited.remove(0);
		return visited;
	}

	protected void chain(Bullet b, Unit first){
		float dmg = damage;
		Unit cur = first;
		for(Unit next : chainTargets(b.team, first)){
			dmg *= falloff;
			Fx.chainLightning.at(cur.x, cur.y, 0f, chainColor, next);
			Sounds.shootArc.at(next.x, next.y, 1.2f, 0.5f);
			next.damage(dmg);
			if(freeze != null) next.apply(freeze, freezeTime);
			cur = next;
		}
	}
}
