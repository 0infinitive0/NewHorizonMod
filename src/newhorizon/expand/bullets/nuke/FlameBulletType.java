package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.expand.status.NoHeal;

import static arc.graphics.g2d.Draw.color;

/**
 * A flame like the Scorch's, made bigger: a short-lived bullet that goes through everything in its way (units and buildings alike, until its life ends), sets what it touches
 * alight (a building it hits catches fire), and stops it healing for {@link #healBlock} ticks, whether it is a unit (the unhealable status) or a building (see {@link NoHeal}).
 * Its look is all in effects: a burst at the nozzle, and puffs that are left along its way and swell as they fade, which is what makes a
 * thrown flame spread out.
 */
public class FlameBulletType extends BulletType{
	/** How long a hit stops healing, in ticks. */
	public float healBlock = 90f;

	/** The burst at the nozzle, the puff along the way and the sparks of a hit. */
	public static final Effect shoot = new Effect(30f, 200f, e -> {
		color(Pal.lightFlame, Pal.darkFlame, Color.gray, e.fin());
		arc.math.Angles.randLenVectors(e.id, 12, e.finpow() * 70f, e.rotation, 16f, (x, y) -> Fill.circle(e.x + x, e.y + y, 1.4f + e.fout() * 3.4f));
		Drawf.light(e.x, e.y, 46f * e.fout(), Pal.lightFlame, 0.5f);
	}), puff = new Effect(28f, 100f, e -> {
		color(Pal.lightFlame, Pal.darkFlame, Color.gray, e.fin());
		Draw.alpha(Mathf.clamp(e.fout() * 1.5f) * 0.8f);
		Fill.circle(e.x, e.y, 2f + e.finpow() * 7f);
		Draw.reset();
	}), hit = new Effect(26f, 80f, e -> {
		color(Pal.lightFlame, Pal.darkFlame, e.fin());
		arc.math.Angles.randLenVectors(e.id, 8, 3f + e.fin() * 18f, (x, y) -> Fill.circle(e.x + x, e.y + y, 0.9f + e.fout() * 3.2f));
	});

	public FlameBulletType(float speed, float damage){
		super(speed, damage);
		pierce = true;
		pierceBuilding = true;
		pierceCap = -1;
		//A flame that hits an enemy building sets its tile alight (never the shooter's own).
		makeFire = true;
		collidesAir = true;
		collidesGround = true;
		keepVelocity = false;
		hittable = false;
		absorbable = false;
		reflectable = false;
		hitSize = 14f;
		status = StatusEffects.burning;
		statusDuration = 60f * 3f;
		shootEffect = shoot;
		hitEffect = hit;
		despawnEffect = mindustry.content.Fx.none;
		smokeEffect = mindustry.content.Fx.none;
		trailEffect = puff;
		trailChance = 0.4f;
		lightColor = Pal.lightFlame;
	}

	@Override
	public void hitEntity(Bullet b, Hitboxc entity, float health){
		super.hitEntity(b, entity, health);
		if(!Vars.net.client() && entity instanceof Unit) ((Unit)entity).apply(NukeStatus.unhealable, healBlock);
	}

	@Override
	public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
		super.hitTile(b, build, x, y, initialHealth, direct);
		NoHeal.mark(build, healBlock);
	}
}
