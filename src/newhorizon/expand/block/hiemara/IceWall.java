package newhorizon.expand.block.hiemara;

import arc.Core;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.weather.ColdWave;

/** A wall of ice. It does not mend while it is warm, but in a cold wave it freezes whole again, the faster the harder the cold. */
public class IceWall extends Wall{
	/** Share of the max health regained per second in a full cold wave. */
	public float coldHealPercent = 0.06f;
	
	public IceWall(String name){
		super(name);
		update = true;
		placeableLiquid = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.repairSpeed, coldHealPercent * 100f, StatUnit.percent);
		stats.add(Stat.abilities, Core.bundle.get("stat.cold-heal"));
	}
	
	public class IceWallBuild extends WallBuild{
		@Override
		public void updateTile(){
			if(health >= maxHealth)return;
			float cold = ColdWave.strength();
			if(cold > 0.01f){
				heal(maxHealth * coldHealPercent * cold * delta() / 60f);
			}
		}
	}
}
