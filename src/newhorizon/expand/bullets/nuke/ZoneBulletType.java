package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * A patch of ground that stays harmful for its whole lifetime, to everyone in it: units take damage and a status, structures take
 * slower damage. It does not care about teams; it is what a blast leaves behind.
 */
public class ZoneBulletType extends BulletType{
	public float radius = 150f;
	/** Damage per second to units, and to structures. */
	public float unitDps = 30f, buildingDps = 8f;
	/** The same as a share of the target's maximum health per second (0.03 is 3% a second), which ignores armor. */
	public float unitPercent = 0f, buildingPercent = 0f;
	public StatusEffect zoneStatus;
	/** How long a unit keeps the status after leaving. */
	public float statusTime = 60f * 3f;
	public Color color = Color.white;
	/** A particle that rises off the ground now and then, and how many appear per tick for each square pixel of the zone. */
	public mindustry.entities.Effect particle;
	public float particleDensity = 0.00002f;

	public ZoneBulletType(){
		super(0f, 0f);
		lifetime = 60f * 20f;
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

		float life = Mathf.clamp(Math.min(b.fin() / 0.05f, b.fout() / 0.1f));
		float r = radius * life;
		if(particle != null && !mindustry.Vars.headless){
			float n = r * r * Mathf.pi * particleDensity * arc.util.Time.delta;
			int count = (int)n;
			if(Mathf.chance(n - count)) count++;
			for(int i = 0; i < count; i++){
				float a = Mathf.random(360f), d = Mathf.sqrt(Mathf.random()) * r;
				particle.at(b.x + Mathf.cosDeg(a) * d, b.y + Mathf.sinDeg(a) * d, 0f, color);
			}
		}
		Units.nearby(b.x - r, b.y - r, r * 2f, r * 2f, unit -> {
			if(unit.dead() || !unit.within(b, r + unit.hitSize / 2f)) return;
			if(zoneStatus != null) unit.apply(zoneStatus, statusTime);
			if(unitDps > 0f) unit.damageContinuousPierce(unitDps / 60f);
			if(unitPercent > 0f) unit.damageContinuousPierce(unit.maxHealth * unitPercent / 60f);
		});

		if((buildingDps > 0f || buildingPercent > 0f) && b.timer(1, 10f)){
			float amount = buildingDps / 6f;
			Units.nearbyBuildings(b.x, b.y, r, build -> {
				if(amount > 0f) build.damage(amount);
				if(buildingPercent > 0f) build.damagePierce(build.maxHealth * buildingPercent / 6f);
			});
		}
	}

	@Override
	public void draw(Bullet b){
		float life = Mathf.clamp(Math.min(b.fin() / 0.05f, b.fout() / 0.1f));
		float r = radius * life;

		Draw.z(Layer.debris + 1f);
		Draw.color(color, 0.12f + 0.04f * Mathf.absin(Time.time, 30f, 1f));
		Fill.circle(b.x, b.y, r);
		Draw.color(color, 0.5f * life);
		Lines.stroke(2f);
		Lines.circle(b.x, b.y, r);
		//Slow drifting motes.
		Draw.color(color, 0.7f * life);
		for(int i = 0; i < 14; i++){
			float a = i * 137.5f + b.id * 11f;
			float d = (Mathf.randomSeed(b.id * 31L + i) * 0.85f) * r;
			float t = (Time.time * 0.01f + i * 0.13f) % 1f;
			Fill.circle(b.x + Mathf.cosDeg(a) * d, b.y + Mathf.sinDeg(a) * d + t * 10f, 2.4f * (1f - t));
		}
		Draw.reset();
	}
}
