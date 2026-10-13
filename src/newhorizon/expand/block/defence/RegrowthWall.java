package newhorizon.expand.block.defence;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/** A wall that slowly regrows its health once it has gone a while without taking damage. */
public class RegrowthWall extends Wall{
	/** Ticks without taking damage before regrowth starts. */
	public float regrowDelay = 180f;
	/** Health regained per second, as a fraction of max health. */
	public float regrowPercent = 0.02f;
	public Effect regrowEffect = Fx.none;

	public RegrowthWall(String name){
		super(name);
		update = true;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.repairSpeed, regrowPercent * 100f, StatUnit.percent);
	}

	public class RegrowthWallBuild extends WallBuild{
		public float lastDamaged = -99999f;

		@Override
		public void damage(float amount){
			lastDamaged = Time.time;
			super.damage(amount);
		}

		@Override
		public void updateTile(){
			if(health < maxHealth && Time.time - lastDamaged >= regrowDelay){
				heal(maxHealth * regrowPercent * Time.delta / 60f);
				if(Mathf.chanceDelta(0.03f)){
					regrowEffect.at(x + Mathf.range(size * 3f), y + Mathf.range(size * 3f));
				}
			}
		}
	}
}
