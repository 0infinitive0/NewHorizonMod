package newhorizon.expand.bullets.nuke;

import arc.util.Time;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.gen.Bullet;

/** A meltdown-style beam that stays where it was fired from and turns while it burns: {@link #turns} full circles over its lifetime. */
public class SpinLaserBulletType extends ContinuousLaserBulletType{
	public float turns = 1f;

	public SpinLaserBulletType(float damage){
		super(damage);
	}

	/** Degrees the beam turns each tick. */
	public float spinSpeed(){
		return 360f * turns / lifetime;
	}

	@Override
	public void update(Bullet b){
		b.rotation(b.rotation() + spinSpeed() * Time.delta);
		super.update(b);
	}
}
