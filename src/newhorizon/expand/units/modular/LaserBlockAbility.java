package newhorizon.expand.units.modular;

import arc.Core;
import arc.math.Angles;
import arc.math.geom.Intersector;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.SapBulletType;
import mindustry.entities.bullet.ShrapnelBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * Laser block: the unit takes no damage while an enemy laser (a lance's beam, a continuous beam or flame, a shrapnel slash, a sap beam)
 * passes through it. It is checked where the unit takes damage, in {@code ChimeraEntity.rawDamage}, against the enemy bullets that are
 * in the world then; this class holds the test.
 */
public class LaserBlockAbility extends Ability{
	/** How far outside the unit's own size a beam may pass and still count. */
	public float margin = 6f;

	protected float lastHealth = -1f, lastShield;

	@Override
	public Ability copy(){
		LaserBlockAbility copy = (LaserBlockAbility)super.copy();
		copy.lastHealth = -1f;
		return copy;
	}

	/**
	 * An instant laser does its damage while it is being made, before it is in the world, so the damage cannot be turned away where it
	 * lands. It is given back instead: if the unit lost health since last tick and a laser of the enemy's, a moment old, passes through it,
	 * what it lost is restored (a hit that kills outright cannot be undone). A beam that lasts is turned away where it lands, see
	 * {@code ChimeraEntity.rawDamage}.
	 */
	@Override
	public void update(Unit unit){
		if(Vars.net.client()) return;
		if(lastHealth < 0f){
			lastHealth = unit.health;
			lastShield = unit.shield;
		}
		if(!unit.dead && (unit.health < lastHealth - 0.001f || unit.shield < lastShield - 0.001f) && passes(unit, margin, 4f)){
			unit.health = Math.min(unit.maxHealth, Math.max(unit.health, lastHealth));
			unit.shield = Math.max(unit.shield, lastShield);
		}
		lastHealth = unit.health;
		lastShield = unit.shield;
	}

	/** Whether a unit with the ability is being hit by an enemy laser now. */
	public static boolean blocks(Unit unit){
		LaserBlockAbility ability = null;
		for(Ability a : unit.abilities){
			if(a instanceof LaserBlockAbility){
				ability = (LaserBlockAbility)a;
				break;
			}
		}
		return ability != null && passes(unit, ability.margin, Float.MAX_VALUE);
	}

	/** Whether an enemy laser that is no older than {@code maxAge} ticks has its line through the unit. */
	public static boolean passes(Unit unit, float margin, float maxAge){
		float reach = 900f;
		boolean[] hit = {false};
		Groups.bullet.intersect(unit.x - reach, unit.y - reach, reach * 2f, reach * 2f, b -> {
			if(hit[0] || b.team == unit.team || b.time > maxAge) return;
			float len = length(b.type);
			if(len <= 0f) return;
			float ex = b.x + Angles.trnsx(b.rotation(), len), ey = b.y + Angles.trnsy(b.rotation(), len);
			if(Intersector.distanceSegmentPoint(b.x, b.y, ex, ey, unit.x, unit.y) <= unit.hitSize / 2f + margin) hit[0] = true;
		});
		return hit[0];
	}

	/** How long a laser-like bullet's line is, or 0 for a bullet that is not one. */
	public static float length(BulletType type){
		if(type instanceof LaserBulletType) return ((LaserBulletType)type).length;
		if(type instanceof ContinuousBulletType) return ((ContinuousBulletType)type).length;
		if(type instanceof ShrapnelBulletType) return ((ShrapnelBulletType)type).length;
		if(type instanceof SapBulletType) return ((SapBulletType)type).length;
		return 0f;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-laser-block", "Laser block");
	}
}
