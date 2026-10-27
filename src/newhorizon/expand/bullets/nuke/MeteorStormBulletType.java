package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.Rand;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.NukeFx;

/**
 * A meteor shower: for {@link #duration} ticks a meteor starts to fall every {@link #interval} ticks on a random spot within
 * {@link #radius} of where the bullet is. A meteor is in the sky for {@link #fall} ticks, with a shadow and a warning ring growing on the
 * ground where it will land, and when it lands it does {@link #impactDamage} in a blast of {@link #impactRadius} (it hurts everything on
 * the ground and every structure, of every team) and leaves dust and sparks.
 *
 * <p>The spots and the times are worked out from the bullet's id and clock alone (meteor {@code i} starts at {@code i * interval}), so
 * nothing about the meteors is stored and every side draws the same shower; the damage is done on the server only.
 */
public class MeteorStormBulletType extends BulletType{
	public float radius = 220f;
	/** How long meteors keep starting to fall, ticks between two, and how long one takes to fall. */
	public float duration = 60f * 10f, interval = 12f, fall = 48f;
	public float impactRadius = 34f, impactDamage = 450f;
	public Color color = Color.valueOf("d9822b");

	public MeteorStormBulletType(){
		super(0f, 0f);
		collides = false;
		collidesTiles = false;
		hittable = false;
		absorbable = false;
		keepVelocity = false;
		hitEffect = despawnEffect = Fx.none;
		rangeOverride = 60f;
	}

	/** How many meteors fall in all. */
	public int count(){
		return Math.max(1, (int)(duration / interval));
	}

	@Override
	public void init(){
		super.init();
		lifetime = (count() - 1) * interval + fall + 2f;
	}

	/** Where meteor {@code i} of the shower round this bullet lands. */
	public float[] spot(Bullet b, int i){
		Rand rand = new Rand(b.id * 7919L + i * 104729L);
		float a = rand.random(360f), d = Mathf.sqrt(rand.nextFloat()) * (radius - impactRadius * 0.5f);
		return new float[]{b.x + Mathf.cosDeg(a) * d, b.y + Mathf.sinDeg(a) * d};
	}

	@Override
	public void update(Bullet b){
		super.update(b);
		//Meteor n lands at n * interval + fall; fdata counts those that have landed.
		while((int)b.fdata < count() && b.time >= (int)b.fdata * interval + fall){
			land(b, (int)b.fdata);
			b.fdata++;
		}
	}

	/** One meteor lands. Effects on every side, damage on the server. */
	protected void land(Bullet b, int i){
		float[] at = spot(b, i);
		NukeFx.meteorImpact.at(at[0], at[1], impactRadius, color);
		Effect.shake(4f, 18f, at[0], at[1]);
		Sounds.explosion.at(at[0], at[1], 0.6f, 1.2f + (i % 5) * 0.08f);
		if(Vars.net.client()) return;
		//Not through walls: a wall in the way takes it.
		Damage.damage(Team.derelict, at[0], at[1], impactRadius, impactDamage, false, false, true);
	}

	@Override
	public void draw(Bullet b){
		int n = count();
		int first = Math.max(0, (int)((b.time - fall) / interval) + (b.time - fall >= 0f ? 1 : 0));
		int last = Math.min(n - 1, (int)(b.time / interval));
		for(int i = first; i <= last; i++){
			float t = (b.time - i * interval) / fall;
			if(t < 0f || t >= 1f) continue;
			float[] at = spot(b, i);
			Rand rand = new Rand(b.id * 31L + i * 17L);
			//It comes in from the same side, a little different each time, and grows as it nears.
			float angle = 115f + rand.range(22f), height = 250f * (1f - t) * (1f - t * 0.2f);
			float mx = at[0] + Mathf.cosDeg(angle) * height, my = at[1] + Mathf.sinDeg(angle) * height;
			float size = 5f + 6f * t + rand.random(0f, 3f);

			//On the ground: the warning ring and the shadow, growing.
			Draw.z(Layer.debris + 1f);
			Draw.color(color, 0.18f + 0.3f * t);
			Lines.stroke(1.4f + t);
			Lines.circle(at[0], at[1], impactRadius * (0.55f + 0.45f * t));
			Draw.color(Color.black, 0.12f + 0.3f * t);
			Fill.circle(at[0], at[1], 3f + (impactRadius * 0.7f) * t);

			//In the sky: the tail of fire behind it, then the rock with a hot glow.
			Draw.z(Layer.flyingUnit + 2f);
			float tail = 26f + 40f * (1f - t);
			for(int k = 0; k < 6; k++){
				float f = k / 6f;
				Draw.color(Color.white, color, f);
				Draw.alpha(0.85f * (1f - f));
				Fill.circle(mx + Mathf.cosDeg(angle) * tail * f, my + Mathf.sinDeg(angle) * tail * f, size * (1f - f * 0.6f));
			}
			Drawf.light(mx, my, size * 5f, color, 0.6f);
			Draw.color(Color.valueOf("5a4636"));
			Fill.circle(mx, my, size);
			Draw.color(Color.valueOf("2d2118"));
			Fill.circle(mx - size * 0.25f, my - size * 0.2f, size * 0.55f);
			Draw.color(color, 0.9f);
			Lines.stroke(1.2f);
			Lines.circle(mx, my, size * 1.05f);
		}
		Draw.reset();
	}
}
