package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.block.nuke.InterceptTurret;

/**
 * A very fast, homing shot that does no damage. An active nuke it hits is switched off and goes over to the shooter's team; the turret
 * that fired it is told it worked, which is what starts that turret's recharge.
 */
public class InterceptBulletType extends BasicBulletType{
	public InterceptBulletType(float speed){
		super(speed, 0f);
		width = 5f;
		height = 20f;
		lifetime = 30f;
		collidesTiles = false;
		collidesGround = false;
		collidesAir = true;
		homingPower = 0.25f;
		homingRange = 200f;
		trailLength = 12;
		trailWidth = 2f;
		backColor = trailColor = lightColor = Color.valueOf("7fd8ff");
		frontColor = Color.white;
		hitEffect = despawnEffect = Fx.hitLaserBlast;
		shootEffect = smokeEffect = Fx.none;
	}

	@Override
	public void hitEntity(Bullet b, Hitboxc entity, float health){
		super.hitEntity(b, entity, health);
		if(Vars.net.client() || !(entity instanceof Unit)) return;
		Unit unit = (Unit)entity;
		if(unit.team == b.team || !NukeUnits.isActive(unit.type)) return;
		if(NukeUnits.capture(unit, b.team) && b.owner instanceof InterceptTurret.InterceptBuild){
			((InterceptTurret.InterceptBuild)b.owner).recharge();
		}
	}
}
