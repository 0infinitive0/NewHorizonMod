package newhorizon.expand.block.nuke;

import arc.Core;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import newhorizon.content.nuke.NukeUnits;

/**
 * A fast turret that only shoots at active nukes. Its shot does no damage: an active nuke it hits is switched off and changes sides. It
 * reloads quickly between shots, so a miss costs little, but a hit that works puts it out of action for a long time.
 */
public class InterceptTurret extends PowerTurret{
	/** Ticks the turret is out of action after it has taken a nuke. */
	public float successCooldown = 60f * 25f;

	public InterceptTurret(String name){
		super(name);
		targetGround = false;
		targetAir = true;
		buildType = InterceptBuild::new;
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-recharge", (InterceptBuild build) -> new Bar(
			() -> build.lockout > 0f ? Core.bundle.get("nh.nuke.bar.recharging") : Core.bundle.get("nh.nuke.bar.ready"),
			() -> Pal.techBlue,
			() -> 1f - build.lockout / successCooldown
		));
	}

	public class InterceptBuild extends PowerTurretBuild{
		/** Ticks left before it can fire again after a successful interception. */
		public float lockout;

		/** An interception worked: the long recharge starts. */
		public void recharge(){
			lockout = successCooldown;
		}

		@Override
		public void updateTile(){
			lockout = Math.max(0f, lockout - Time.delta);
			super.updateTile();
		}

		/** Only active nukes are targets. */
		@Override
		public void findTarget(){
			target = Units.bestEnemy(team, x, y, range, u -> !u.dead() && NukeUnits.isActive(u.type) && u.targetable(team), unitSort);
		}

		/** While it recharges it neither aims nor fires. */
		@Override
		public boolean hasAmmo(){
			return lockout <= 0f && super.hasAmmo();
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(lockout);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			lockout = read.f();
		}
	}
}
