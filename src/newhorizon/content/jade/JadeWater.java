package newhorizon.content.jade;

import mindustry.content.Liquids;
import mindustry.world.blocks.environment.Floor;

/** What the Jade Accord counts as water: spores love it. */
public class JadeWater{
	/**
	 * A liquid floor whose liquid is water: shallow and deep water, tainted water, sand and darksand water. Tar, slag and
	 * cryofluid pools are liquid floors too, but not water.
	 */
	public static boolean isWater(Floor floor){
		return floor != null && floor.isLiquid && floor.liquidDrop == Liquids.water;
	}
}
