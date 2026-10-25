package newhorizon.expand.bullets.nuke;

import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;

/**
 * Invisible and motionless: it fires {@link #waves} waves of {@link #count} missiles each from where it stands, {@link #interval} ticks
 * apart, the first one at once. Each wave is a ring, turned a little from the last, so the missiles do not all follow the same lines.
 */
public class SwarmSpawnerBulletType extends BulletType{
	public BulletType missile;
	public int waves = 3, count = 120;
	public float interval = 60f;

	public SwarmSpawnerBulletType(){
		super(0f, 0f);
		collides = false;
		collidesTiles = false;
		hittable = false;
		absorbable = false;
		keepVelocity = false;
		hitEffect = despawnEffect = Fx.none;
		rangeOverride = 60f;
	}

	@Override
	public void init(){
		super.init();
		lifetime = (waves - 1) * interval + 2f;
	}

	@Override
	public void init(Bullet b){
		super.init(b);
		//The first wave leaves the moment the nuke goes off.
		launch(b, 0);
		b.fdata = 1f;
	}

	@Override
	public void update(Bullet b){
		super.update(b);

		//Wave n is due at n * interval; fdata counts those already fired.
		int due = Math.min(waves, 1 + (int)(b.time / interval));
		while(b.fdata < due){
			launch(b, (int)b.fdata);
			b.fdata++;
		}
	}

	/** Fires one wave. */
	protected void launch(Bullet b, int wave){
		float turn = wave * 360f / count / waves + Mathf.random(1.5f);
		for(int i = 0; i < count; i++){
			float angle = turn + i * 360f / count;
			missile.create(b.owner, b.team, b.x, b.y, angle, -1f, 1f + Mathf.range(0.08f));
		}
	}
}
