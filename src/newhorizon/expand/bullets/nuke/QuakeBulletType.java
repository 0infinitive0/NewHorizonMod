package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.Rand;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;

/**
 * An earthquake. For a few seconds the ground shakes in waves; every wave takes a share of the maximum health of every structure in reach
 * (more in the middle) and does very little to units, only a few points to those on the ground, which also stumble. Structures of
 * every team are hit. The shaking and the cracks are shown everywhere, the damage is done on the server only.
 */
public class QuakeBulletType extends BulletType{
	public float radius = 290f;
	/** Ticks between waves. */
	public float interval = 15f;
	/** Share of a structure's maximum health one wave takes in the middle (it falls to 40% of that at the edge), which ignores armor. */
	public float buildingPercent = 0.055f;
	/** What one wave does to a unit on the ground. */
	public float unitDamage = 4f;
	public float stumbleTime = 90f;
	public Color color = Color.valueOf("b08968");

	public QuakeBulletType(){
		super(0f, 0f);
		lifetime = 60f * 4f;
		collides = false;
		collidesTiles = false;
		collidesAir = false;
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
		if(!b.timer(1, interval)) return;

		Effect.shake(5f, 22f, b.x, b.y);
		Rand rand = new Rand(b.id * 7919L + (long)(b.time / interval));
		for(int i = 0; i < 10; i++){
			float a = rand.random(360f), d = Mathf.sqrt(rand.nextFloat()) * radius;
			Fx.pulverizeMedium.at(b.x + Mathf.cosDeg(a) * d, b.y + Mathf.sinDeg(a) * d);
		}
		if(Vars.net.client()) return;

		Units.nearbyBuildings(b.x, b.y, radius, build -> {
			float f = Mathf.clamp(build.dst(b) / radius);
			build.damagePierce(build.maxHealth * buildingPercent * Mathf.lerp(1f, 0.4f, f));
		});
		Units.nearby(b.x - radius, b.y - radius, radius * 2f, radius * 2f, u -> {
			if(u.dead() || !u.isGrounded() || !u.within(b, radius + u.hitSize / 2f)) return;
			u.damagePierce(unitDamage);
			u.apply(StatusEffects.slow, stumbleTime);
		});
	}

	@Override
	public void draw(Bullet b){
		float life = Mathf.clamp(Math.min(b.fin() / 0.05f, b.fout() / 0.1f));
		float wave = (b.time % interval) / interval;

		Draw.z(Layer.debris + 1f);
		//Cracks running out from the middle, growing for the first part of the quake.
		Draw.color(Color.valueOf("2b1d12"), 0.8f * life);
		Lines.stroke(2.2f);
		float reach = radius * Mathf.clamp(b.fin() * 4f) * 0.95f;
		for(int i = 0; i < 9; i++){
			float a = b.id * 17f + i * 40f + Mathf.randomSeedRange(b.id * 5L + i, 14f);
			float x = b.x, y = b.y, len = 0f;
			while(len < reach){
				float step = 16f + Mathf.randomSeed(b.id * 31L + i * 17L + (long)len, 12f);
				float na = a + Mathf.randomSeedRange(b.id * 53L + i * 13L + (long)len, 32f);
				float nx = x + Mathf.cosDeg(na) * step, ny = y + Mathf.sinDeg(na) * step;
				Lines.line(x, y, nx, ny);
				x = nx;
				y = ny;
				len += step;
			}
		}
		//The wave going out.
		Draw.color(color, 0.5f * life * (1f - wave));
		Lines.stroke(3f * (1f - wave));
		Lines.circle(b.x, b.y, radius * wave);
		Draw.reset();
	}
}
