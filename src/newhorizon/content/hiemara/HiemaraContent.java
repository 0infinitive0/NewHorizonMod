package newhorizon.content.hiemara;

import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.UnlockableContent;
import newhorizon.content.NHPlanets;
import newhorizon.expand.hiemara.CargoCap;

/**
 * Everything that belongs to the planet Hiemara and to nothing else.
 *
 * <p>Load order: floors -> units -> blocks. It has to run before the ice floors are made, because every rich water
 * tile gets an ice form as well. The tech tree is attached from {@code NHTechTree}.
 */
public class HiemaraContent{
	public static void load(){
		HiemaraFloors.load();
		HiemaraUnits.load();
		HiemaraBlocks.load();
		CargoCap.load();
	}
	
	/** @return every block that can be built on Hiemara: its own blocks and the cores. */
	public static arc.struct.Seq<mindustry.world.Block> buildable(){
		var list = HiemaraBlocks.all.copy();
		list.addAll(mindustry.Vars.content.blocks().select(b -> b instanceof mindustry.world.blocks.storage.CoreBlock));
		return list;
	}
	
	/** Shows the content, and the base game items it needs, on Hiemara. */
	public static void showOnPlanet(){
		var planet = NHPlanets.hiemara;
		for(var block : HiemaraBlocks.all)block.shownPlanets.add(planet);
		for(var unit : HiemaraUnits.airLine)unit.shownPlanets.add(planet);
		for(var unit : HiemaraUnits.navalLine)unit.shownPlanets.add(planet);
		for(var unit : HiemaraUnits.tankerLine)unit.shownPlanets.add(planet);
		for(var item : HiemaraFloors.ores)item.shownPlanets.add(planet);
		Items.copper.shownPlanets.add(planet);
		Items.lead.shownPlanets.add(planet);
		Items.silicon.shownPlanets.add(planet);
		Items.sporePod.shownPlanets.add(planet);
		Items.pyratite.shownPlanets.add(planet);
		Liquids.oil.shownPlanets.add(planet);
		Liquids.water.shownPlanets.add(planet);
	}
}
