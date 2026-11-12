package newhorizon.expand.bullets.plasma;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;

/**
 * The Plasma Mine Cannon's shot: a shell thrown, like a mine, to the place aimed at. It does nothing by itself (an enemy touching it does not set
 * it off, nothing shoots it down); where it comes to rest it <b>bursts</b> into {@link #count} plasma mines, which scatter round that place,
 * come to rest and drift as any mine does. So one shot covers a patch of ground instead of one point.
 */
public class PlasmaClusterBulletType extends PlasmaMineBulletType{
	/** The mine it splits into. */
	public final PlasmaMineBulletType mine;
	/** How many mines, and how far from the shell each rests, in px (a range). */
	public int count = 5;
	public float minScatter = 30f, maxScatter = 75f;
	/** Speed (px a tick) under which the shell has come to rest and bursts. */
	public float burstSpeed = 0.5f;

	public PlasmaClusterBulletType(PlasmaMineBulletType mine){
		super();
		this.mine = mine;
		//A thrown shell's real speed is worked out from how far it is thrown (see PlasmaMineBulletType.init), and it slows as it goes. This is
		//only the speed a turret leads its target with: about the shell's average over a throw, so the burst lands where a walker will be.
		speed = 2f;
		collides = false;
		collidesAir = false;
		collidesGround = false;
		splashDamage = 0f;
		splashDamageRadius = 0f;
		puddle = 0f;
		hitSize = 10f;
		width = height = 12f;
	}

	@Override
	protected boolean thrown(Bullet b){
		return b.vel.len() > burstSpeed;
	}

	/** It has come to rest: the mines come out of it, each thrown a little way out, and it is gone. */
	@Override
	protected void rest(Bullet b){
		if(!Vars.net.client()){
			float start = Mathf.random(360f);
			for(int i = 0; i < count; i++){
				float angle = start + i * 360f / count + Mathf.range(20f);
				float out = Mathf.random(minScatter, maxScatter);
				//the mine slows as it flies, so it is aimed a little further than where it should rest
				float aim = out + 40f;
				mine.create(b.owner, b.team, b.x, b.y, angle, -1f, 1f, 1f, null, null, b.x + Mathf.cosDeg(angle) * aim, b.y + Mathf.sinDeg(angle) * aim);
			}
			Fx.shootBig.at(b.x, b.y, 0f, pink);
		}
		b.remove();
	}

	@Override
	public void draw(Bullet b){
		super.draw(b);
		Draw.z(Layer.bullet);
		Draw.color(glow, 0.7f * arc.math.Mathf.clamp(b.fout() * 6f));
		Lines.stroke(1.4f);
		Lines.circle(b.x, b.y, 12f + 1.5f * Mathf.absin(arc.util.Time.time + b.id * 5f, 6f, 1f));
		Draw.reset();
	}
}
