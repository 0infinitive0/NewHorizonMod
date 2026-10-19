package newhorizon.expand.bullets.chronos;

import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import newhorizon.expand.block.chronos.TemporalDisable;

/** Knocks the enemy turret (or generator, or shield) it hits offline for {@link #disableTime} ticks. */
public class DisruptorBulletType extends BasicBulletType{
	public float disableTime = 60f * 5f;

	public DisruptorBulletType(float speed, float damage){
		super(speed, damage);
	}

	@Override
	public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
		super.hitTile(b, build, x, y, initialHealth, direct);
		if(build.team != b.team) TemporalDisable.disable(build, disableTime);
	}
}
