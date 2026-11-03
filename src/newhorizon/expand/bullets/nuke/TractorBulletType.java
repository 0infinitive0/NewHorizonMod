package newhorizon.expand.bullets.nuke;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
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
 * damage, as with a continuous laser. The unit it holds is kept in the bullet's data, and the beam is drawn the way the Parallax's is: the base
 * game's beam textures from the weapon to that unit, only while there is one.
 */
public class TractorBulletType extends ContinuousBulletType{
	/** The beam's half width for hitting, the pull at the weapon and the extra pull at the far end (both per tick, before mass). */
	public float width = 14f, force = 40f, scaledForce = 20f;
	/** A pulled unit is not dragged closer than this. */
	public float holdDistance = 26f;
	public Color color = Color.valueOf("6cb6ff");
	/** The beam's width, as the Parallax's laserWidth. */
	public float beamWidth = 0.6f;

	private static TextureRegion laser, laserStart, laserEnd;

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
		//Every peer keeps it, for drawing; only the server pulls.
		b.data = target;
		if(Vars.net.client()) return;
		if(target == null || bestDst[0] <= holdDistance || target.type.knockbackMultiplier <= 0f) return;
		float pull = (force + (1f - Mathf.clamp(bestDst[0] / len)) * scaledForce) * Time.delta * target.type.knockbackMultiplier;
		target.impulse(Tmp.v1.set(b).sub(target).limit(pull));
	}

	@Override
	public void draw(Bullet b){
		if(!(b.data instanceof Unit)) return;
		Unit target = (Unit)b.data;
		if(target.dead() || !target.isAdded()) return;

		if(laser == null || !laser.found()){
			//The Parallax's own beam textures; the base game's plain laser if this build of the game names them differently.
			laser = Core.atlas.find("parallax-laser", Core.atlas.find("laser"));
			laserStart = Core.atlas.find("parallax-laser-start", Core.atlas.find("laser-end"));
			laserEnd = Core.atlas.find("parallax-laser-end", Core.atlas.find("laser-end"));
		}
		float fade = Mathf.clamp(b.time / 10f) * Mathf.clamp((b.lifetime - b.time) / 10f);

		Draw.z(Layer.bullet);
		Draw.mixcol(color, Mathf.absin(4f, 0.6f));
		Drawf.laser(laser, laserStart, laserEnd, b.x, b.y, target.x, target.y, beamWidth * fade);
		Draw.mixcol();
		Drawf.light(b.x, b.y, target.x, target.y, 26f, color, 0.5f * fade);
		Draw.reset();
	}

	@Override
	public void drawLight(Bullet b){
	}
}
