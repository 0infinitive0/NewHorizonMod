package newhorizon.expand.bullets.nuke;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.NukeFx;

/**
 * A patch of space that drags every unit in it towards its centre (a positive force) or throws them away from it (a negative one),
 * whichever team they are on. Heavy units move less. It does no damage of its own; {@link #onEnd} runs when it runs out, which is how a
 * gravity nuke makes its blast after the pull.
 */
public class ForceZoneBulletType extends BulletType{
	public float radius = 240f;
	/** Pull per tick at the edge and at the centre. A negative sign pushes instead. */
	public float edge = 0.1f, center = 0.5f;
	public Color color = Color.white;
	/**
	 * With a number of pulses above zero the force comes in that many short bursts instead of all the time: every {@code pulseInterval}
	 * ticks it pushes (or pulls) for {@code pulseOn} ticks, with a ring of force going out and a thump.
	 */
	public int pulses = 0;
	public float pulseInterval = 24f, pulseOn = 6f;
	/** Runs once when the zone ends. */
	public Cons<Bullet> onEnd;

	public ForceZoneBulletType(){
		super(0f, 0f);
		lifetime = 60f * 3f;
		collides = false;
		collidesTiles = false;
		collidesAir = true;
		collidesGround = true;
		hittable = false;
		absorbable = false;
		keepVelocity = false;
		hitEffect = despawnEffect = Fx.none;
		rangeOverride = 60f;
	}

	@Override
	public void update(Bullet b){
		super.update(b);

		float r = radius;
		if(pulses > 0){
			//The start of a pulse: a ring going out and a thump.
			if(b.timer(2, pulseInterval)){
				NukeFx.shockRing.at(b.x, b.y, radius, color);
				mindustry.entities.Effect.shake(6f, 24f, b.x, b.y);
				mindustry.gen.Sounds.explosion.at(b.x, b.y, 0.6f, 1.5f);
			}
			if(b.time % pulseInterval >= pulseOn) return;
		}
		Units.nearby(b.x - r, b.y - r, r * 2f, r * 2f, unit -> {
			if(unit.dead()) return;
			float dst = unit.dst(b);
			if(dst > r + unit.hitSize / 2f) return;

			float t = 1f - Mathf.clamp(dst / r);
			float sign = Math.signum(center);
			float force = Mathf.lerp(Math.abs(edge), Math.abs(center), t * t) * sign;
			//Heavier things are harder to move.
			float mass = Mathf.clamp(300f / unit.mass(), 0.25f, 1.5f);
			Tmp.v1.set(b).sub(unit);
			if(Tmp.v1.isZero(0.5f)) return;
			unit.vel.add(Tmp.v1.nor().scl(force * mass));
		});
	}

	@Override
	public void despawned(Bullet b){
		super.despawned(b);
		if(onEnd != null) onEnd.get(b);
	}

	@Override
	public void draw(Bullet b){
		float r = radius * Mathf.clamp(b.fout() * 1.2f);
		if(pulses > 0){
			//Only the field itself: the rings are effects.
			Draw.z(Layer.effect + 0.5f);
			Draw.color(color, 0.07f);
			arc.graphics.g2d.Fill.circle(b.x, b.y, radius * Mathf.clamp(b.fout() * 4f));
			Draw.reset();
			return;
		}
		Draw.z(Layer.effect + 0.5f);
		Draw.color(color, 0.1f);
		arc.graphics.g2d.Fill.circle(b.x, b.y, r);
		Draw.color(color, 0.6f);
		Lines.stroke(2f);
		Lines.circle(b.x, b.y, r);
		//Arcs sweeping in (a pull) or out (a push).
		float spin = (b.time * (center >= 0f ? -4f : 4f)) % 360f;
		for(int i = 0; i < 4; i++){
			float f = ((b.time * 0.03f * (center >= 0f ? -1f : 1f) + i * 0.25f) % 1f + 1f) % 1f;
			Lines.stroke(1.5f);
			Lines.arc(b.x, b.y, r * f, 0.2f, spin + i * 90f);
		}
		Draw.reset();
	}
}
