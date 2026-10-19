package newhorizon.expand.bullets.chronos;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import newhorizon.content.chronos.ChronosColor;
import newhorizon.content.chronos.ChronosFx;

/**
 * A black hole that stays where it was dropped for its whole lifetime: it drags every enemy unit in its reach towards its centre,
 * swallows enemy bullets, and crushes whatever gets close to the middle. It does nothing to its owner's side.
 */
public class BlackHoleBulletType extends BulletType{
	/** How far it reaches. */
	public float radius = 110f;
	/** Pull on a unit at the very edge and at the centre, per tick. */
	public float pullEdge = 0.02f, pullCenter = 0.45f;
	/** Share of the radius that is the crushing core. */
	public float coreFraction = 0.28f;
	/** Damage per second to anything in the core. */
	public float coreDps = 260f;
	/** Damage per second further out. */
	public float outerDps = 28f;
	/** Bullets this close to the centre are swallowed, as a share of the radius. */
	public float absorbFraction = 0.9f;

	public BlackHoleBulletType(){
		super(0f, 0f);
		lifetime = 60f * 5f;
		collides = false;
		collidesTiles = false;
		collidesGround = true;
		collidesAir = true;
		hittable = false;
		absorbable = false;
		keepVelocity = false;
		despawnHit = false;
		hitEffect = despawnEffect = Fx.none;
		lightColor = ChronosColor.violet;
		lightRadius = 90f;
		lightOpacity = 0.5f;
		rangeOverride = 60f;
	}

	@Override
	public void update(Bullet b){
		super.update(b);

		float life = Mathf.clamp(b.fin() < 0.1f ? b.fin() / 0.1f : b.fout() < 0.12f ? b.fout() / 0.12f : 1f);
		float r = radius * life;

		Units.nearbyEnemies(b.team, b.x - r, b.y - r, r * 2f, r * 2f, unit -> {
			float dst = unit.dst(b);
			if(dst > r + unit.hitSize / 2f) return;

			float t = 1f - Mathf.clamp(dst / r);
			float force = Mathf.lerp(pullEdge, pullCenter, t * t);
			//Heavier things are harder to drag in.
			unit.vel.add(Tmp.v1.set(b).sub(unit).nor().scl(force * Mathf.clamp(300f / unit.mass(), 0.25f, 1.5f)));
			if(dst < r * coreFraction){
				unit.damageContinuousPierce(coreDps / 60f);
			}else{
				unit.damageContinuous(outerDps / 60f);
			}
		});

		//Buildings are not pulled in, but the core crushes them.
		if(b.timer(1, 10f)){
			Damage.damage(b.team, b.x, b.y, r * coreFraction, coreDps / 6f * 0.5f, false, collidesAir, collidesGround);
		}

		Groups.bullet.intersect(b.x - r, b.y - r, r * 2f, r * 2f, other -> {
			if(other.team == b.team || other == b || !other.type.absorbable) return;
			if(other.within(b, r * absorbFraction)) other.absorb();
		});
	}

	@Override
	public void despawned(Bullet b){
		super.despawned(b);
		ChronosFx.holeCollapse.at(b.x, b.y, radius);
	}

	@Override
	public void draw(Bullet b){
		float life = Mathf.clamp(b.fin() < 0.1f ? b.fin() / 0.1f : b.fout() < 0.12f ? b.fout() / 0.12f : 1f);
		float r = radius * life;

		Draw.z(Layer.effect + 0.5f);
		//Faint accretion rings, spinning faster the closer to the middle.
		for(int i = 0; i < 4; i++){
			float f = (i + 1) / 4f;
			Draw.color(i % 2 == 0 ? ChronosColor.violet : ChronosColor.chrono, 0.18f + 0.1f * (1f - f));
			Lines.stroke(1.6f);
			float rr = r * f;
			float spin = spin(b, 70f / f);
			Lines.arc(b.x, b.y, rr, 0.62f, spin);
			Lines.arc(b.x, b.y, rr, 0.35f, spin + 180f);
		}

		Draw.color(ChronosColor.violet, 0.25f);
		Fill.circle(b.x, b.y, r * coreFraction * 1.3f);
		Draw.color(ChronosColor.hole);
		Fill.circle(b.x, b.y, r * coreFraction);
		Draw.color(ChronosColor.chronoLight, 0.9f);
		Lines.stroke(2f);
		Lines.circle(b.x, b.y, r * coreFraction);
		Draw.reset();
	}

	private static float spin(Bullet b, float period){
		return (arc.util.Time.time * 360f / period) % 360f + b.id * 13f;
	}
}
