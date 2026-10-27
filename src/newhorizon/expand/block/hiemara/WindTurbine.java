package newhorizon.expand.block.hiemara;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.world.blocks.power.PowerGenerator;

/** Makes power from the wind, which never stops but comes in gusts. */
public class WindTurbine extends PowerGenerator{
	/** Time of a gust cycle in ticks. */
	public float gustTime = 420f;
	/** How much the output drops between gusts, 0 to 1. */
	public float gustDepth = 0.35f;
	
	public WindTurbine(String name){
		super(name);
		
		placeableLiquid = true;
	}
	
	public class WindTurbineBuild extends GeneratorBuild{
		@Override
		public void updateTile(){
			float gust = 1f - gustDepth * (0.5f + 0.5f * Mathf.sin(Time.time / gustTime * Mathf.PI2 + id * 1.7f));
			productionEfficiency = enabled ? gust : 0f;
		}
	}
}
