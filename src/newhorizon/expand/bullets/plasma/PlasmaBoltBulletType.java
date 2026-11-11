package newhorizon.expand.bullets.plasma;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Puddles;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.world.Tile;
import newhorizon.content.industry.IndustryLiquids;

/**
 * A plasma bullet: it burns what it hits (melting), and wherever it bursts it leaves a little plasma on the ground (a puddle that, like every
 * plasma puddle, sets fires and melts what stands in it).
 */
public class PlasmaBoltBulletType extends BasicBulletType{
	public static final Color pink = Color.valueOf("ff7bd0"), glow = Color.valueOf("ffd3f2");
	/** How much plasma a burst leaves on the ground. */
	public float puddle = 4f;

	public PlasmaBoltBulletType(float speed, float damage){
		super(speed, damage);
		backColor = trailColor = lightColor = pink;
		frontColor = glow;
		status = StatusEffects.melting;
		statusDuration = 180f;
	}

	@Override
	public void hit(Bullet b, float x, float y){
		super.hit(b, x, y);
		leave(x, y);
	}

	/** Leaves the plasma puddle at a place in the world. */
	protected void leave(float x, float y){
		if(puddle <= 0f || Vars.world == null) return;
		Tile tile = Vars.world.tileWorld(x, y);
		if(tile != null) Puddles.deposit(tile, IndustryLiquids.plasma, puddle * (0.8f + Mathf.random(0.4f)));
	}
}
