package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * A ball of light that hangs where it was made and, a few times a second, strikes arcs of lightning at what is around it, like an
 * Aegis does: several targets at once, units and structures of every team alike. The arcs are drawn everywhere; the damage is done on the
 * server only.
 *
 * <p>Two options turn it into something else. With {@link #refresh} every arc that finds a target gives the orb its whole life back, so
 * it lasts as long as there is something to strike. With a {@link #pullStart} the orb, once it has been there that long (in all, not
 * counting the refreshes), starts to drag every unit in reach towards itself, harder and harder, until the orb is gone. The orb can
 * also be made {@link #hittable}, which is what lets point defence shoot it down: its {@code damage} is then how much it can take.
 */
public class ArcOrbBulletType extends BulletType{
	public float radius = 210f;
	/** Ticks between volleys (15 is four a second), targets hit by one, and the damage of one arc. */
	public float interval = 15f;
	public int maxTargets = 20;
	public float arcDamage = 50f;
	public StatusEffect arcStatus = StatusEffects.electrified;
	public float statusTime = 60f * 2f;
	public Color color = Color.valueOf("ffe066");
	/** The colour of the arcs and the sparks around the orb. */
	public Color arcColor = Color.valueOf("8fe3ff");
	/** Whether striking something gives the orb its whole life back. */
	public boolean refresh = false;
	/** Ticks of life after which the orb starts to pull (negative: never), how fast the pull grows per tick, and its limit. Same units as {@link ForceZoneBulletType}. */
	public float pullStart = -1f, pullGrowth = 0.0005f, pullMax = 1.2f;

	/** What an orb keeps track of: how long it has been there, which does not start over when its life does, and how many volleys it has made. */
	protected static class State{
		float age;
		int volleys;
	}

	public ArcOrbBulletType(){
		super(0f, 0f);
		lifetime = 60f * 10f;
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

	protected State state(Bullet b){
		if(!(b.data instanceof State)) b.data = new State();
		return (State)b.data;
	}

	public float age(Bullet b){
		return b.data instanceof State ? ((State)b.data).age : b.time;
	}

	/** How strongly the orb is there: it swells at the start of its life and fades at the end. */
	public float strength(Bullet b){
		return Mathf.clamp(Math.min(age(b) / (lifetime * 0.05f), b.fout() / 0.08f));
	}

	/** How hard the orb pulls now, per tick: nothing before it is old enough, then more and more. */
	public float pull(Bullet b){
		if(pullStart < 0f) return 0f;
		return Mathf.clamp((age(b) - pullStart) * pullGrowth, 0f, pullMax);
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		State st = state(b);
		st.age += Time.delta;
		if(b.timer(1, interval) && strength(b) > 0.5f) volley(b);
		float force = pull(b);
		if(force > 0f) drag(b, force);
	}

	/** Drags every unit in reach towards the orb. Heavy units move less. */
	protected void drag(Bullet b, float force){
		Units.nearby(b.x - radius, b.y - radius, radius * 2f, radius * 2f, unit -> {
			if(unit.dead() || !unit.within(b, radius + unit.hitSize / 2f)) return;
			float mass = Mathf.clamp(300f / unit.mass(), 0.25f, 1.5f);
			Tmp.v1.set(b).sub(unit);
			if(Tmp.v1.isZero(0.5f)) return;
			unit.vel.add(Tmp.v1.nor().scl(force * mass * Time.delta));
		});
	}

	@Override
	public void removed(Bullet b){
		super.removed(b);
		//Gone, whether it ran out or was shot down.
		Fx.sparkExplosion.at(b.x, b.y, 0f, arcColor);
	}

	/** One volley: arcs to a few things in reach, chosen at random but the same way on every side. */
	public void volley(Bullet b){
		Seq<Position> found = new Seq<>();
		Units.nearby(b.x - radius, b.y - radius, radius * 2f, radius * 2f, u -> {
			if(!u.dead() && u.within(b, radius + u.hitSize / 2f)) found.add(u);
		});
		Units.nearbyBuildings(b.x, b.y, radius, build -> {
			if(build.isValid()) found.add(build);
		});
		found.sort(p -> b.dst(p));

		State st = state(b);
		Rand rand = new Rand(b.id * 9973L + st.volleys++);
		int hits = Math.min(maxTargets, found.size);
		for(int i = 0; i < hits; i++){
			Position target = found.remove(rand.nextInt(found.size));
			Fx.chainLightning.at(b.x, b.y, 0f, arcColor, target);
			if(Vars.net.client()) continue;
			if(target instanceof mindustry.gen.Unit){
				mindustry.gen.Unit u = (mindustry.gen.Unit)target;
				u.damage(arcDamage);
				if(arcStatus != null) u.apply(arcStatus, statusTime);
			}else if(target instanceof mindustry.gen.Building){
				((mindustry.gen.Building)target).damage(arcDamage);
			}
		}
		//Striking something gives the orb its life back.
		if(hits > 0 && refresh) b.time = 0f;
		//With nothing in reach it still crackles.
		if(hits == 0){
			Vec2 spot = new Vec2().trns(rand.random(360f), rand.random(radius * 0.4f, radius * 0.9f)).add(b.x, b.y);
			Fx.chainLightning.at(b.x, b.y, 0f, arcColor, spot);
		}
		Sounds.acceleratorLightning1.at(b.x, b.y, 0.6f, 1.2f);
	}

	@Override
	public void draw(Bullet b){
		float power = strength(b);
		float pulse = 1f + 0.12f * Mathf.absin(Time.time, 6f, 1f);

		Draw.z(Layer.effect + 1f);
		Drawf.light(b.x, b.y, radius * 0.8f * power, color, 0.55f * power);
		Draw.color(color, 0.3f * power);
		Fill.circle(b.x, b.y, 26f * power * pulse);
		Draw.color(color, 0.8f * power);
		Fill.circle(b.x, b.y, 14f * power * pulse);
		Draw.color(Color.white, power);
		Fill.circle(b.x, b.y, 7f * power * pulse);

		//Little flickering sparks around it.
		Draw.color(arcColor, power);
		Lines.stroke(1.6f);
		Angles.randLenVectors(b.id * 13L + (long)(Time.time / 3f), 6, 22f * power, 34f * power, (x, y) -> Lines.lineAngle(b.x + x, b.y + y, Mathf.angle(x, y), 6f));

		float force = pull(b);
		if(force > 0f){
			//The pull: the edge of its reach, and streaks falling in towards the orb, more of them and brighter the harder it pulls.
			float f = force / pullMax;
			Draw.z(Layer.effect + 0.5f);
			Draw.color(arcColor, 0.25f + 0.35f * f);
			Lines.stroke(1.5f + 1.5f * f);
			Lines.circle(b.x, b.y, radius * power);
			Draw.color(color, 0.05f + 0.07f * f);
			Fill.circle(b.x, b.y, radius * power);
			int streaks = 10 + (int)(18 * f);
			Draw.color(arcColor, (0.35f + 0.5f * f) * power);
			for(int i = 0; i < streaks; i++){
				float phase = ((Time.time * (0.012f + 0.02f * f) + i / (float)streaks) % 1f);
				float angle = i * 137.5f + b.id * 11f;
				float dst = radius * power * (1f - phase * phase);
				float len = 6f + 14f * f;
				Lines.stroke(1f + 1.4f * phase);
				Lines.lineAngle(b.x + Angles.trnsx(angle, dst), b.y + Angles.trnsy(angle, dst), angle + 180f, len);
			}
		}
		Draw.reset();
	}
}
