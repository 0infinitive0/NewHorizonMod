package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.bullet.ContinuousBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/**
 * A tractor beam, held by the weapon like a continuous laser: while it fires, the one enemy unit nearest to the weapon that the beam
 * touches is dragged toward the weapon (harder the nearer it is to the end, easier for light units: the force is an impulse, so it is
 * divided by the unit's mass; a unit that cannot be pushed cannot be pulled either). Everything in the beam also takes a trickle of
 * damage, as with a continuous laser. The beam is drawn with chevrons that run toward the weapon.
 */
public class TractorBulletType extends ContinuousBulletType{
	/** The beam's half width for hitting, the pull at the weapon and the extra pull at the far end (both per tick, before mass). */
	public float width = 14f, force = 40f, scaledForce = 20f;
	/** A pulled unit is not dragged closer than this. */
	public float holdDistance = 26f;
	public Color color = Color.valueOf("6cb6ff");

	public TractorBulletType(float damage){
		super();
		this.damage = damage;
		lifetime = 60f;
		length = 240f;
		damageInterval = 8f;
		largeHit = false;
		hitColor = color;
		collidesAir = true;
		collidesGround = true;
		hitEffect = Fx.hitBeam;
		despawnEffect = Fx.none;
		shootEffect = Fx.none;
		smokeEffect = Fx.none;
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		if(Vars.net.client()) return;

		float rot = b.rotation(), len = currentLength(b);
		float ex = b.x + Angles.trnsx(rot, len), ey = b.y + Angles.trnsy(rot, len);
		Unit[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearbyEnemies(b.team, b.x - len, b.y - len, len * 2f, len * 2f, unit -> {
			if(unit.dead() || !unit.isAdded() || !unit.targetable(b.team) || !unit.checkTarget(collidesAir, collidesGround)) return;
			if(Intersector.distanceSegmentPoint(b.x, b.y, ex, ey, unit.x, unit.y) > width + unit.hitSize / 2f) return;
			float d = unit.dst(b);
			if(d < bestDst[0]){
				bestDst[0] = d;
				best[0] = unit;
			}
		});

		Unit target = best[0];
		if(target == null || bestDst[0] <= holdDistance || target.type.knockbackMultiplier <= 0f) return;
		float pull = (force + (1f - Mathf.clamp(bestDst[0] / len)) * scaledForce) * Time.delta * target.type.knockbackMultiplier;
		target.impulse(Tmp.v1.set(b).sub(target).limit(pull));
	}

	@Override
	public void draw(Bullet b){
		float len = currentLength(b);
		float fade = Mathf.clamp(b.time / 10f) * Mathf.clamp((b.lifetime - b.time) / 10f);
		float rot = b.rotation();
		float ex = b.x + Angles.trnsx(rot, len), ey = b.y + Angles.trnsy(rot, len);

		Draw.z(Layer.bullet + 1f);
		Draw.color(color, 0.35f * fade);
		Lines.stroke(width * 0.9f);
		Lines.line(b.x, b.y, ex, ey);
		Draw.color(color, 0.8f * fade);
		Lines.stroke(2.2f + Mathf.absin(Time.time, 4f, 1.2f));
		Lines.line(b.x, b.y, ex, ey);
		Draw.color(Color.white, fade);
		Lines.stroke(1f);
		Lines.line(b.x, b.y, ex, ey);

		//Chevrons that run toward the weapon.
		int n = Math.max(2, (int)(len / 36f));
		for(int i = 0; i < n; i++){
			float t = 1f - ((Time.time * 0.03f + i / (float)n) % 1f);
			float px = b.x + Angles.trnsx(rot, len * t), py = b.y + Angles.trnsy(rot, len * t);
			Draw.color(Color.white, 0.8f * fade);
			Fill.poly(px, py, 3, 4.5f, rot + 180f);
		}
		Drawf.light(b.x, b.y, ex, ey, 30f, color, 0.6f * fade);
		Draw.reset();
	}

	@Override
	public void drawLight(Bullet b){
	}
}
