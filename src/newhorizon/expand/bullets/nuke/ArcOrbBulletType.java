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

	/** How strongly the orb is there: it swells and fades at the ends of its life. */
	public float strength(Bullet b){
		return Mathf.clamp(Math.min(b.fin() / 0.05f, b.fout() / 0.08f));
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		if(b.timer(1, interval) && strength(b) > 0.5f) volley(b);
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

		Rand rand = new Rand(b.id * 9973L + (long)(b.time / interval));
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
		Draw.reset();
	}
}
