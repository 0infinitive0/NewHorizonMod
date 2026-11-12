package newhorizon.expand.bullets.plasma;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/**
 * A plasma mine. It is thrown to the place aimed at, stops there and then <b>drifts slowly</b>, with a gentle pull toward the nearest enemy
 * unit in reach. It cannot be shot down or reflected and walls do not stop it on its way; it goes off when an enemy unit comes within a few tiles of it or when it
 * is over an enemy building, and if nothing does in its lifetime it fizzles out without a blast.
 */
public class PlasmaMineBulletType extends PlasmaBoltBulletType{
	/** The slowest it drifts and the fastest it drifts (px a tick), and how far it notices an enemy. */
	public float driftSpeed = 0.3f, maxDrift = 0.5f, seekRange = 110f;
	/** How hard it leans toward a target a tick, from 0 to 1. */
	public float seekLean = 0.03f;
	/** How near (px, plus half the unit's size) an enemy unit has to come to a mine at rest to set it off, without touching it. */
	public float fuseRadius = 30f;

	public PlasmaMineBulletType(){
		super(5f, 0f);
		lifetime = 60f * 25f;
		drag = 0.02f;
		collides = true;
		collidesAir = true;
		collidesGround = true;
		collidesTiles = false;
		collideTerrain = false;
		hittable = false;
		absorbable = false;
		reflectable = false;
		despawnHit = false;
		hitSize = 18f;
		width = height = 10f;
		splashDamage = 220f;
		splashDamageRadius = 56f;
		statusDuration = 240f;
		puddle = 8f;
		despawnEffect = Fx.none;
		hitEffect = Fx.massiveExplosion;
		trailLength = 0;
		shrinkX = shrinkY = 0f;
	}

	/** Thrown so that it comes to rest about where it was aimed (a mine's total travel is its speed over its drag). */
	@Override
	public void init(Bullet b){
		super.init(b);
		if(b.aimX != 0f || b.aimY != 0f){
			float dist = Mathf.dst(b.x, b.y, b.aimX, b.aimY);
			float v = Mathf.clamp(dist * drag, driftSpeed, 9f);
			if(b.vel.len() > 0.0001f) b.vel.setLength(v);
		}
		//A fraction of a degree of scatter: no two mines fly the very same line (and a mine never sits exactly in line with its target).
		b.vel.rotate(Mathf.range(0.5f));
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		if(thrown(b)) return;
		rest(b);
	}

	/** Whether it is still being thrown (too fast to have come to rest). */
	protected boolean thrown(Bullet b){
		return b.vel.len() > maxDrift * 1.6f;
	}

	/** One tick of a mine that has come to rest: it goes off over an enemy building, and otherwise drifts and leans toward the nearest enemy. */
	protected void rest(Bullet b){
		//over a building of another team: the game never collides a bullet that ignores tiles with one, so it is checked here
		if(collidesGround && !Vars.net.client()){
			Building under = Vars.world.buildWorld(b.x, b.y);
			if(under != null && under.isValid() && under.team != b.team && under.team != Team.derelict){
				detonate(b);
				return;
			}
		}
		//an enemy unit close enough: a mine is a proximity mine, so that a walking unit cannot slip between two of them
		if(!Vars.net.client()){
			boolean[] close = {false};
			float scan = fuseRadius + 40f;
			Units.nearbyEnemies(b.team, b.x - scan, b.y - scan, scan * 2f, scan * 2f, u -> {
				if(!close[0] && !u.dead() && u.checkTarget(collidesAir, collidesGround) && b.within(u, fuseRadius + u.hitSize / 2f)) close[0] = true;
			});
			if(close[0]){
				detonate(b);
				return;
			}
		}
		float len = b.vel.len();
		if(len < 0.001f) b.vel.set(driftSpeed, 0f).setAngle(Mathf.randomSeed(b.id, 360f));
		else if(len < driftSpeed) b.vel.setLength(driftSpeed);
		Teamc target = Units.closestTarget(b.team, b.x, b.y, seekRange, u -> u.checkTarget(collidesAir, collidesGround), t -> false);
		if(target != null){
			Vec2 want = Tmp.v1.set(target.getX() - b.x, target.getY() - b.y).setLength(maxDrift);
			b.vel.lerp(want, seekLean * arc.util.Time.delta);
		}
		if(b.vel.len() > maxDrift) b.vel.setLength(maxDrift);
	}

	/** Sets the mine off where it is. */
	protected void detonate(Bullet b){
		if(!b.isAdded()) return;
		hit(b, b.x, b.y);
		b.remove();
	}

	@Override
	public void draw(Bullet b){
		float pulse = 1f + 0.12f * Mathf.absin(arc.util.Time.time + b.id * 7f, 8f, 1f);
		float fade = Mathf.clamp(b.fout() * 6f);
		Draw.z(Layer.bullet);
		Draw.color(pink, 0.28f * fade);
		Fill.circle(b.x, b.y, 15f * pulse);
		Draw.color(pink, fade);
		Fill.circle(b.x, b.y, 8f * pulse);
		Draw.color(glow, fade);
		Fill.circle(b.x, b.y, 4.5f * pulse);
		Draw.color(arc.graphics.Color.white, fade);
		Fill.circle(b.x, b.y, 2f);
		Lines.stroke(1.2f);
		Draw.color(pink, 0.8f * fade);
		for(int i = 0; i < 6; i++){
			float a = i * 60f + arc.util.Time.time * 2f;
			Lines.line(b.x + Mathf.cosDeg(a) * 9f * pulse, b.y + Mathf.sinDeg(a) * 9f * pulse, b.x + Mathf.cosDeg(a) * 13f * pulse, b.y + Mathf.sinDeg(a) * 13f * pulse);
		}
		Draw.reset();
	}

	@Override
	public void drawLight(Bullet b){
		Drawf.light(b.x, b.y, 50f, pink, 0.6f);
	}
}
