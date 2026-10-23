package newhorizon.expand.block.chronos;

import arc.util.Time;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.chronos.ChronosFx;

/** A wall that rewinds: after {@link #restoreDelay} ticks without taking damage it is back at full health. */
public class ChronoWall extends Wall{
	public float restoreDelay = 60f * 6f;

	public ChronoWall(String name){
		super(name);
		update = true;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.repairTime, restoreDelay / 60f, StatUnit.seconds);
	}

	public class ChronoWallBuild extends WallBuild{
		/** Ticks without damage. */
		public float calm;
		protected float lastHealth = -1f;

		@Override
		public void updateTile(){
			if(lastHealth < 0f) lastHealth = health;

			//Healing from elsewhere does not count as damage; only losing health does.
			if(health < lastHealth - 0.001f) calm = 0f;
			else calm += Time.delta;

			if(health < maxHealth - 0.001f && calm >= restoreDelay){
				health = maxHealth;
				calm = 0f;
				ChronosFx.rewind.at(x, y, size * 6f);
			}
			lastHealth = health;
		}
	}
}
