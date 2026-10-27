package newhorizon.expand.block.hiemara;

import mindustry.world.blocks.power.PowerGenerator;

/** Produces power all the time, without any fuel. */
public class FloatingGenerator extends PowerGenerator{
	public FloatingGenerator(String name){
		super(name);
		
		placeableLiquid = true;
	}
	
	public class FloatingGeneratorBuild extends GeneratorBuild{
		@Override
		public void updateTile(){
			productionEfficiency = enabled ? 1f : 0f;
		}
	}
}
