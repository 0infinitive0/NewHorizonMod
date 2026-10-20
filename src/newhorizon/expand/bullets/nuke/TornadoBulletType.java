package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.IntFloatMap;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.NukeStatus;

/**
 * A tornado. It drifts slowly along a bending path and, wherever it goes, drags in every unit in reach (of any team), swings it around
 * itself for a few seconds, which carries it a short way along with the tornado, and then flings it outward. Everything in reach takes
 * damage over time, and now and then it throws a bolt of lightning, at a unit it is carrying or at nothing in particular.
 *
 * <p>The units themselves are handled on the server only; what they look like and the lightning are made everywhere. Units too big to
 * be carried (above {@link #maxCarrySize}) are only pulled a little and burned.
 */
public class TornadoBulletType extends BulletType{
	public float radius = 170f;
	/** Damage per second to every unit in reach, and to every structure (more towards the middle, where it passes over them). */
	public float dps = 36f, buildingDps = 48f;
	/** Distance from the centre at which a carried unit swings, and how fast it goes round (degrees per tick). */
	public float orbit = 56f, spin = 7f;
	/** Ticks a unit is swung for, and ticks it is thrown for. */
	public float swingTime = 170f, flingTime = 12f;
	/** Speed units are pulled in at, and thrown at, in pixels per tick. */
	public float pullSpeed = 4.2f, flingSpeed = 13f;
	/** How long a thrown unit cannot be caught again. */
	public float immunity = 60f * 5f;
	/** Units bigger than this are not carried. */
	public float maxCarrySize = 48f;
	public float lightningDamage = 32f;
	public int lightningLength = 22;
	public Color color = Color.valueOf("9bb7d4"), lightningColor = Color.valueOf("d9e8ff");

	/** The three stages a carried unit goes through. */
	public static final int PULL = 0, SWING = 1, FLING = 2;

	/** What the tornado remembers: who it has, and who has just been thrown. */
	public static class State{
		public final IntMap<Caught> caught = new IntMap<>();
		public final IntFloatMap thrown = new IntFloatMap();
		public float lightning = 30f;
	}

	public static class Caught{
		public int phase;
		public float timer, angle, radius;
		public final arc.math.geom.Vec2 dir = new arc.math.geom.Vec2();
	}

	public TornadoBulletType(){
		super(0.8f, 0f);
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

	@Override
	public void init(Bullet b){
		super.init(b);
		b.data = new State();
	}

	public static State state(Bullet b){
		return b.data instanceof State ? (State)b.data : null;
	}

	/** How strongly the tornado is there: it builds up and dies down at the ends of its life. */
	public float strength(Bullet b){
		return Mathf.clamp(Math.min(b.fin() / 0.08f, b.fout() / 0.1f));
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		State s = state(b);
		if(s == null) return;

		//A slow, bending path.
		b.vel.rotate(Mathf.sin(b.time / 50f + b.id * 1.7f) * 0.55f * Time.delta);

		float power = strength(b);
		float r = radius * power;

		if(!Vars.net.client()){
			//Everything in reach burns, and anything free that is light enough is caught.
			Units.nearby(b.x - r, b.y - r, r * 2f, r * 2f, u -> {
				if(u.dead() || !u.within(b, r + u.hitSize / 2f)) return;
				if(dps > 0f) u.damageContinuousPierce(dps / 60f);
				if(!s.caught.containsKey(u.id)) tryCatch(b, s, u);
			});

			//Structures in reach are torn at too, the ones under the funnel most.
			if(buildingDps > 0f && b.timer(1, 10f)){
				float rr = r;
				Units.nearbyBuildings(b.x, b.y, rr, build -> {
					float f = Mathf.clamp(build.dst(b) / Math.max(rr, 1f));
					build.damage(buildingDps / 6f * Mathf.lerp(1.5f, 0.5f, f));
				});
			}

			//Everything caught is driven from here, wherever it has got to: a unit that has been thrown out of reach is still being thrown.
			IntSeq gone = new IntSeq();
			for(IntMap.Entry<Caught> e : s.caught){
				Unit u = Groups.unit.getByID(e.key);
				if(u == null || u.dead() || !u.isAdded()){
					gone.add(e.key);
					continue;
				}
				if(!drive(b, s, u, e.value)) gone.add(e.key);
			}
			for(int i = 0; i < gone.size; i++) s.caught.remove(gone.get(i));
		}

		s.lightning -= Time.delta;
		if(s.lightning <= 0f && power > 0.5f){
			s.lightning = Mathf.random(24f, 70f);
			strike(b, s);
		}
	}

	/** Catches a free unit, unless it was only just thrown or is too heavy: those are only pulled a little. */
	protected void tryCatch(Bullet b, State s, Unit u){
		if(s.thrown.get(u.id, 0f) > Time.time) return;

		if(u.hitSize > maxCarrySize){
			float mass = Mathf.clamp(300f / u.mass(), 0.25f, 1.5f);
			Tmp.v1.set(b).sub(u);
			if(!Tmp.v1.isZero(1f)) u.vel.add(Tmp.v1.nor().scl(0.35f * mass));
			return;
		}

		Caught c = new Caught();
		c.phase = PULL;
		c.angle = b.angleTo(u);
		c.radius = u.dst(b);
		s.caught.put(u.id, c);
	}

	/** One tick of pulling, swinging or throwing a caught unit. Returns false once the unit is free again. */
	protected boolean drive(Bullet b, State s, Unit u, Caught c){
		//Whatever it is doing, a carried unit cannot walk or fly away on its own.
		u.apply(NukeStatus.tornadoCaught, 10f);
		float delta = Time.delta;
		c.timer += delta;

		if(c.phase == PULL){
			float dst = u.dst(b);
			if(dst <= orbit + 8f || c.timer > 100f){
				c.phase = SWING;
				c.timer = 0f;
				c.angle = b.angleTo(u);
				c.radius = Math.max(dst, orbit);
			}else{
				Tmp.v1.set(b).sub(u).limit(pullSpeed * Mathf.clamp(300f / u.mass(), 0.5f, 1.5f));
				u.vel.set(Tmp.v1);
			}
		}

		if(c.phase == SWING){
			c.angle += spin * delta;
			c.radius = Mathf.approach(c.radius, orbit + Mathf.sin(c.timer / 14f) * 10f, 1.4f * delta);
			Tmp.v2.trns(c.angle, c.radius).add(b.x, b.y).sub(u);
			u.vel.set(Tmp.v2.limit(20f));

			if(c.timer >= swingTime) startFling(b, u, c);
		}else if(c.phase == FLING){
			u.vel.set(c.dir).scl(flingSpeed);
			if(c.timer >= flingTime){
				//Let go: it keeps some of the speed and cannot be caught again for a while.
				u.vel.scl(0.35f);
				s.thrown.put(u.id, Time.time + immunity);
				return false;
			}
		}
		return true;
	}

	/** Chooses the way the unit is thrown: out from the centre and along the way it was going round. */
	protected void startFling(Bullet b, Unit u, Caught c){
		c.phase = FLING;
		c.timer = 0f;
		Tmp.v3.set(u).sub(b);
		if(Tmp.v3.isZero(0.5f)) Tmp.v3.trns(c.angle, 1f);
		Tmp.v3.nor();
		//The tangent of a swing that turns counter-clockwise.
		c.dir.set(Tmp.v3).scl(0.8f).add(-Tmp.v3.y * 0.6f, Tmp.v3.x * 0.6f).nor();
	}

	/** A bolt, at a unit the tornado is carrying or in a random direction. */
	protected void strike(Bullet b, State s){
		float angle = Mathf.random(360f);
		if(!s.caught.isEmpty() && Mathf.chance(0.6f)){
			int pick = Mathf.random(s.caught.size - 1), i = 0;
			for(IntMap.Entry<Caught> e : s.caught){
				if(i++ == pick){
					Unit u = Groups.unit.getByID(e.key);
					if(u != null) angle = b.angleTo(u);
					break;
				}
			}
		}
		Lightning.create(Team.derelict, lightningColor, lightningDamage, b.x, b.y, angle, lightningLength);
	}

	@Override
	public void despawned(Bullet b){
		super.despawned(b);
		State s = state(b);
		if(s == null || Vars.net.client()) return;

		//When the storm dies down, whoever is still in it is let go the same way: thrown out.
		for(IntMap.Entry<Caught> e : s.caught){
			Unit u = Groups.unit.getByID(e.key);
			if(u == null || u.dead()) continue;
			startFling(b, u, e.value);
			u.vel.set(e.value.dir).scl(flingSpeed);
		}
		s.caught.clear();
	}

	@Override
	public void draw(Bullet b){
		float power = strength(b);
		float r = radius * power;
		Draw.z(Layer.effect + 0.5f);

		//The funnel seen from above: arms of cloud spiralling round the centre, faster the closer in, with dust flung about.
		Draw.color(color, 0.1f * power);
		Fill.circle(b.x, b.y, r);
		for(int i = 0; i < 6; i++){
			float f = (i + 1) / 6f;
			float rr = r * f;
			float spinAngle = (b.time * (9f - i * 1.1f) + b.id * 20f + i * 50f) % 360f;
			Draw.color(i % 2 == 0 ? color : Color.white, (0.16f + 0.2f * (1f - f)) * power);
			Lines.stroke(2.2f + 1.6f * (1f - f));
			Lines.arc(b.x, b.y, rr, 0.32f, spinAngle);
			Lines.arc(b.x, b.y, rr, 0.22f, spinAngle + 180f);
		}
		Draw.color(Color.valueOf("3a4658"), 0.55f * power);
		Fill.circle(b.x, b.y, r * 0.12f);
		Draw.color(color, 0.8f * power);
		for(int i = 0; i < 22; i++){
			float f = (Mathf.randomSeed(b.id * 17L + i) * 0.9f + 0.1f);
			float a = (b.time * (3f + 5f * (1f - f)) + i * 47f) % 360f;
			Fill.circle(b.x + Mathf.cosDeg(a) * r * f, b.y + Mathf.sinDeg(a) * r * f, 1.6f);
		}
		Draw.reset();
	}
}
